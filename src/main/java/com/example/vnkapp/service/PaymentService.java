package com.example.vnkapp.service;

import com.example.vnkapp.config.NgeniusProperties;
import com.example.vnkapp.dto.payment.PaymentCallbackResponseDto;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderRequest;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderResponse;
import com.example.vnkapp.dto.payment.ngenius.NgeniusWebhookPayload;
import com.example.vnkapp.entity.Order;
import com.example.vnkapp.entity.Payment;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.enums.order.OrderStatus;
import com.example.vnkapp.enums.payment.PaymentMethod;
import com.example.vnkapp.enums.payment.PaymentStatus;
import com.example.vnkapp.repository.OrderRepository;
import com.example.vnkapp.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates payment-gateway order creation for our orders and reconciles payment state
 * from N-Genius webhooks. See https://docs.ngenius-payments.com/reference/consuming-web-hooks
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // N-Genius event -> our PaymentStatus. See "Supported Event Types" in the webhook docs.
    private static final Map<String, PaymentStatus> EVENT_STATUS_MAP = Map.ofEntries(
            Map.entry("AUTHORISED", PaymentStatus.PROCESSING),
            Map.entry("PARTIALLY_CAPTURED", PaymentStatus.PROCESSING),
            Map.entry("PURCHASED", PaymentStatus.COMPLETED),
            Map.entry("CAPTURED", PaymentStatus.COMPLETED),
            Map.entry("APM_PAYMENT_ACCEPTED", PaymentStatus.COMPLETED),
            Map.entry("DECLINED", PaymentStatus.FAILED),
            Map.entry("AUTHORISATION_FAILED", PaymentStatus.FAILED),
            Map.entry("PURCHASE_DECLINED", PaymentStatus.FAILED),
            Map.entry("PURCHASE_FAILED", PaymentStatus.FAILED),
            Map.entry("CAPTURE_FAILED", PaymentStatus.FAILED),
            Map.entry("FULL_AUTH_REVERSED", PaymentStatus.FAILED),
            Map.entry("PURCHASE_REVERSED", PaymentStatus.FAILED),
            Map.entry("CAPTURE_VOIDED", PaymentStatus.FAILED),
            Map.entry("CANCELLED", PaymentStatus.FAILED),
            Map.entry("CANCELLATION_REQUESTED", PaymentStatus.FAILED),
            Map.entry("GATEWAY_RISK_PRE_AUTH_REJECTED", PaymentStatus.FAILED),
            Map.entry("PRE_AUTH_FRAUD_CHECK_REJECTED", PaymentStatus.FAILED),
            Map.entry("POST_AUTH_FRAUD_CHECK_REJECTED", PaymentStatus.FAILED),
            Map.entry("REFUNDED", PaymentStatus.REFUNDED),
            Map.entry("PARTIALLY_REFUNDED", PaymentStatus.PARTIALLY_REFUNDED)
    );

    private static final Map<String, PaymentStatus> PAYMENT_STATE_MAP = Map.ofEntries(
            Map.entry("PURCHASED", PaymentStatus.COMPLETED),
            Map.entry("CAPTURED", PaymentStatus.COMPLETED),
            Map.entry("APM_PAYMENT_ACCEPTED", PaymentStatus.COMPLETED),
            Map.entry("AUTHORISED", PaymentStatus.PROCESSING),
            Map.entry("PARTIALLY_CAPTURED", PaymentStatus.PROCESSING),
            Map.entry("STARTED", PaymentStatus.PENDING),
            Map.entry("AWAIT_3DS", PaymentStatus.PENDING),
            Map.entry("FAILED", PaymentStatus.FAILED),
            Map.entry("DECLINED", PaymentStatus.FAILED),
            Map.entry("AUTHORISATION_FAILED", PaymentStatus.FAILED),
            Map.entry("PURCHASE_DECLINED", PaymentStatus.FAILED),
            Map.entry("PURCHASE_FAILED", PaymentStatus.FAILED),
            Map.entry("CAPTURE_FAILED", PaymentStatus.FAILED),
            Map.entry("CANCELLED", PaymentStatus.FAILED),
            Map.entry("REFUNDED", PaymentStatus.REFUNDED),
            Map.entry("PARTIALLY_REFUNDED", PaymentStatus.PARTIALLY_REFUNDED)
    );

    private static final Set<String> SUCCESS_PAYMENT_STATES = Set.of(
            "PURCHASED", "CAPTURED", "APM_PAYMENT_ACCEPTED");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NgeniusPaymentService ngeniusPaymentService;
    private final NgeniusProperties properties;
    private final ReferralService referralService;
    private final ObjectMapper gatewayObjectMapper = new ObjectMapper();

    public PaymentService(PaymentRepository paymentRepository,
                           OrderRepository orderRepository,
                           NgeniusPaymentService ngeniusPaymentService,
                           NgeniusProperties properties,
                           ReferralService referralService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.ngeniusPaymentService = ngeniusPaymentService;
        this.properties = properties;
        this.referralService = referralService;
    }

    /**
     * Creates a payment session with the gateway for a freshly placed order and persists a
     * PENDING Payment record with the hosted-page redirect URL. Throws on any gateway failure
     * so the caller's transaction (order creation) rolls back rather than leaving an order
     * with no way to pay for it.
     */
    @Transactional
    public Payment initiateNgeniusPayment(Order order) {
        return initiateNgeniusPayment(order, null);
    }

    /**
     * Replaces a stale/failed payment session with a new N-Genius hosted-page link for a
     * pending order that has not been paid yet.
     */
    @Transactional
    public Payment retryNgeniusPayment(Order order) {
        return retryNgeniusPayment(order, null);
    }

    @Transactional
    public Payment retryNgeniusPayment(Order order, User user) {
        paymentRepository.findByOrderIdActive(order.getId()).ifPresent(existing -> {
            if (existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new IllegalArgumentException("Order is already paid");
            }
            existing.setStatus(com.example.vnkapp.entity.BaseEntity.STATUS_INACTIVE);
            paymentRepository.save(existing);
        });
        return initiateNgeniusPayment(order, user);
    }

    @Transactional
    public Payment initiateNgeniusPayment(Order order, User user) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Payment gateway is not configured. Set NGENIUS_API_KEY and NGENIUS_OUTLET_REF.");
        }

        NgeniusOrderRequest request = buildOrderRequest(order, user);

        NgeniusOrderResponse response;
        try {
            response = ngeniusPaymentService.createOrder(request);
        } catch (Exception ex) {
            log.error("N-Genius order creation failed for order {}", order.getOrderNumber(), ex);
            throw new IllegalStateException(
                    "Unable to initiate payment with the payment gateway. Please try again.", ex);
        }

        String paymentUrl = response != null ? response.paymentUrl() : null;
        if (response == null || response.id() == null || paymentUrl == null) {
            log.error("N-Genius did not return a payment link for order {}: {}", order.getOrderNumber(), response);
            throw new IllegalStateException("Payment gateway did not return a payment link.");
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .currency(properties.getCurrency())
                .paymentMethod(PaymentMethod.NGENIUS)
                .paymentStatus(PaymentStatus.PENDING)
                .gatewayOrderId(response.id())
                .gatewayPaymentId(response.paymentId())
                .gatewayPaymentUrl(paymentUrl)
                .gatewayResponse(toJson(response))
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("N-Genius payment session created for order {}: gatewayOrderId={}, paymentUrl={}",
                order.getOrderNumber(), response.id(), paymentUrl);
        return saved;
    }

    public boolean isValidWebhookRequest(HttpServletRequest request) {
        String expected = properties.getWebhookHeaderValue();
        if (expected == null || expected.isBlank()) {
            log.warn("ngenius.webhook-header-value is not configured - rejecting webhook");
            return false;
        }
        String headerName = properties.getWebhookHeaderName();
        if (headerName == null || headerName.isBlank()) {
            headerName = "X-Webhook-Secret";
        }
        String actual = request.getHeader(headerName);
        return expected.equals(actual);
    }

    /**
     * Called from the hosted-page success URL. Looks up the order, asks N-Genius for the
     * current payment state, and marks the order confirmed only when the gateway says paid.
     */
    @Transactional
    public PaymentCallbackResponseDto confirmFromRedirect(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("Payment reference is missing from the redirect.");
        }

        Order order = resolveOrderFromRedirectRef(ref.trim());
        if (order == null) {
            throw new IllegalArgumentException("No order found for payment reference: " + ref);
        }

        Payment payment = paymentRepository.findByOrderIdActive(order.getId()).orElse(null);
        if (payment == null) {
            throw new IllegalArgumentException("No payment record found for order: " + order.getOrderNumber());
        }

        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED
                && order.getOrderStatus() == OrderStatus.CONFIRMED) {
            return new PaymentCallbackResponseDto(
                    order.getOrderNumber(),
                    order.getOrderStatus(),
                    payment.getPaymentStatus(),
                    null,
                    "Payment already confirmed.");
        }

        if (payment.getGatewayOrderId() == null || payment.getGatewayOrderId().isBlank()) {
            throw new IllegalStateException("Payment is missing the N-Genius order id.");
        }

        NgeniusOrderResponse gatewayOrder;
        try {
            gatewayOrder = ngeniusPaymentService.getOrder(payment.getGatewayOrderId());
        } catch (Exception ex) {
            log.error("Failed to retrieve N-Genius order {} for {}", payment.getGatewayOrderId(),
                    order.getOrderNumber(), ex);
            throw new IllegalStateException("Unable to verify payment with the payment gateway. Please try again.");
        }

        String gatewayState = gatewayOrder != null ? gatewayOrder.paymentState() : null;
        PaymentStatus mappedStatus = gatewayState != null
                ? PAYMENT_STATE_MAP.getOrDefault(gatewayState.toUpperCase(), PaymentStatus.PENDING)
                : PaymentStatus.PENDING;

        if (gatewayState != null && SUCCESS_PAYMENT_STATES.contains(gatewayState.toUpperCase())) {
            mappedStatus = PaymentStatus.COMPLETED;
        }

        applyPaymentStatus(order, payment, mappedStatus, toJson(gatewayOrder), gatewayState);
        if (gatewayOrder != null && gatewayOrder.paymentId() != null) {
            payment.setGatewayPaymentId(gatewayOrder.paymentId());
            paymentRepository.save(payment);
        }

        return new PaymentCallbackResponseDto(
                order.getOrderNumber(),
                order.getOrderStatus(),
                payment.getPaymentStatus(),
                gatewayState,
                callbackMessage(order, payment, gatewayState));
    }

    @Transactional
    public void handleNgeniusWebhookRaw(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("N-Genius webhook body was empty");
            return;
        }

        NgeniusWebhookPayload payload;
        try {
            payload = gatewayObjectMapper.readValue(rawBody, NgeniusWebhookPayload.class);
        } catch (Exception ex) {
            log.error("Failed to parse N-Genius webhook JSON: {}", rawBody, ex);
            return;
        }

        log.info("Parsed N-Genius webhook: eventId={}, eventName={}, outletId={}, orderRef={}, paymentState={}",
                payload.eventId(),
                payload.eventName(),
                payload.outletId(),
                payload.order() != null ? payload.order().reference() : null,
                payload.firstPaymentState());
        handleNgeniusWebhook(payload);
    }

    @Transactional
    public void handleNgeniusWebhook(NgeniusWebhookPayload payload) {
        if (payload.order() == null) {
            log.warn("N-Genius webhook missing order, eventId={}", payload.eventId());
            return;
        }

        Order order = resolveOrderFromWebhook(payload.order());
        if (order == null) {
            log.warn("N-Genius webhook references unknown order: reference={}, gatewayId={}",
                    payload.order().reference(), payload.order().id());
            return;
        }

        Payment payment = paymentRepository.findByOrderIdActive(order.getId()).orElse(null);
        if (payment == null) {
            log.warn("No payment record found for order: {}", order.getOrderNumber());
            return;
        }

        PaymentStatus newStatus = EVENT_STATUS_MAP.get(payload.eventName());
        if (newStatus == null) {
            log.info("Unhandled/informational N-Genius webhook event '{}' for order {}",
                    payload.eventName(), order.getOrderNumber());
            return;
        }

        if (payload.order().id() != null) {
            payment.setGatewayOrderId(payload.order().id());
        }
        applyGatewayPaymentDetails(payment, payload);
        applyPaymentStatus(order, payment, newStatus, toJson(payload), payload.eventName());
        log.info("Processed N-Genius webhook eventId={} '{}' for order {}: paymentStatus={}",
                payload.eventId(), payload.eventName(), order.getOrderNumber(), newStatus);
    }

    private void applyPaymentStatus(Order order, Payment payment, PaymentStatus newStatus,
                                    String gatewayResponse, String reason) {
        if (!shouldApplyPaymentStatus(payment.getPaymentStatus(), newStatus)) {
            log.info("Ignoring N-Genius status {} for order {} because payment is already {}",
                    newStatus, order.getOrderNumber(), payment.getPaymentStatus());
            return;
        }

        payment.setPaymentStatus(newStatus);
        if (gatewayResponse != null) {
            payment.setGatewayResponse(gatewayResponse);
        }

        switch (newStatus) {
            case COMPLETED -> {
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(Instant.now());
                }
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            case FAILED -> payment.setFailureReason(reason);
            case REFUNDED -> {
                payment.setRefundedAt(Instant.now());
                order.setOrderStatus(OrderStatus.REFUNDED);
            }
            case PARTIALLY_REFUNDED -> payment.setRefundedAt(Instant.now());
            default -> { /* PENDING / PROCESSING */ }
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        if (newStatus == PaymentStatus.COMPLETED) {
            referralService.completeReferralOnFirstPaidOrder(order.getUserId(), order.getId());
        }
    }

    /**
     * SALE mode sends AUTHORISED then CAPTURED. Never let AUTHORISED overwrite a completed capture.
     */
    private boolean shouldApplyPaymentStatus(PaymentStatus current, PaymentStatus incoming) {
        if (current == null || incoming == null || current == incoming) {
            return incoming != null;
        }
        if (current == PaymentStatus.COMPLETED) {
            return incoming == PaymentStatus.REFUNDED || incoming == PaymentStatus.PARTIALLY_REFUNDED;
        }
        return current != PaymentStatus.REFUNDED;
    }

    private Order resolveOrderFromRedirectRef(String ref) {
        Optional<Order> byOrderNumber = orderRepository.findByOrderNumberActive(ref);
        if (byOrderNumber.isPresent()) {
            return byOrderNumber.get();
        }

        Optional<Payment> byGatewayOrderId = paymentRepository.findByGatewayOrderId(ref);
        if (byGatewayOrderId.isEmpty() && !ref.startsWith("urn:order:")) {
            byGatewayOrderId = paymentRepository.findByGatewayOrderId("urn:order:" + ref);
        }
        return byGatewayOrderId
                .flatMap(payment -> orderRepository.findById(payment.getOrderId()))
                .orElse(null);
    }

    private String callbackMessage(Order order, Payment payment, String gatewayState) {
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            return "Payment confirmed. Your order is now confirmed.";
        }
        if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
            return "Payment was not successful. You can retry payment from your orders.";
        }
        return "Payment is still " + (gatewayState != null ? gatewayState.toLowerCase() : "pending")
                + ". Order remains " + order.getOrderStatus() + " until the gateway confirms capture.";
    }

    private Order resolveOrderFromWebhook(NgeniusWebhookPayload.Order webhookOrder) {
        String reference = webhookOrder.reference();
        if (reference != null && !reference.isBlank()) {
            Optional<Order> byOrderNumber = orderRepository.findByOrderNumberActive(reference);
            if (byOrderNumber.isPresent()) {
                return byOrderNumber.get();
            }
        }

        String gatewayOrderId = webhookOrder.id();
        if (gatewayOrderId != null && !gatewayOrderId.isBlank()) {
            return paymentRepository.findByGatewayOrderId(gatewayOrderId)
                    .flatMap(payment -> orderRepository.findById(payment.getOrderId()))
                    .orElse(null);
        }

        return null;
    }

    private void applyGatewayPaymentDetails(Payment payment, NgeniusWebhookPayload payload) {
        NgeniusWebhookPayload.Payment gatewayPayment = payload.firstPayment();
        if (gatewayPayment == null) {
            return;
        }

        String paymentId = gatewayPayment.reference() != null
                ? gatewayPayment.reference()
                : gatewayPayment.id();
        if (paymentId != null && !paymentId.isBlank()) {
            payment.setGatewayPaymentId(paymentId);
        }

        if (gatewayPayment.authResponse() != null) {
            String transactionId = firstNonBlank(
                    gatewayPayment.authResponse().rrn(),
                    gatewayPayment.authResponse().authorizationCode());
            if (transactionId != null) {
                payment.setTransactionId(transactionId);
            }
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private NgeniusOrderRequest buildOrderRequest(Order order, User user) {
        NgeniusOrderRequest.Amount amount = new NgeniusOrderRequest.Amount(
                properties.getCurrency(), toMinorUnits(order.getTotalAmount()));

        // showPayerName=true requires non-blank billing firstName AND lastName.
        NgeniusOrderRequest.MerchantAttributes merchantAttributes = new NgeniusOrderRequest.MerchantAttributes(
                String.valueOf(properties.getPaymentAttempts()),
                properties.getCancelUrl(),
                properties.getRedirectUrl(),
                true);

        String sourceName = firstNonBlank(
                order.getShippingFullName(),
                user != null ? user.getUsername() : null,
                "Customer");
        String[] nameParts = splitPayerName(sourceName);

        NgeniusOrderRequest.BillingAddress billingAddress = new NgeniusOrderRequest.BillingAddress(
                nameParts[0],
                nameParts[1],
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingCountry(),
                countryCodeOf(order.getShippingCountry()),
                order.getShippingPostalCode());

        String email = user != null ? firstNonBlank(user.getEmail()) : null;

        return new NgeniusOrderRequest(
                properties.getAction(),
                amount,
                order.getOrderNumber(),
                email,
                merchantAttributes,
                billingAddress);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * N-Genius rejects blank lastName when showPayerName is requested. A single-token
     * shipping name used to omit lastName entirely because of JsonInclude.NON_NULL.
     */
    private String[] splitPayerName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 && !parts[1].isBlank() ? parts[1] : firstName;
        return new String[] { firstName, lastName };
    }

    private String countryCodeOf(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        String normalized = country.trim();
        if (normalized.length() == 2) {
            return normalized.toUpperCase();
        }
        return switch (normalized.toLowerCase()) {
            case "india" -> "IN";
            case "united arab emirates", "uae" -> "AE";
            case "saudi arabia", "ksa" -> "SA";
            default -> null;
        };
    }

    private String toJson(Object value) {
        try {
            return gatewayObjectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Failed to serialize gateway payload for logging/storage", ex);
            return null;
        }
    }
}
