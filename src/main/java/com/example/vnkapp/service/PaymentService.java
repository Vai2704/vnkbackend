package com.example.vnkapp.service;

import com.example.vnkapp.config.NgeniusProperties;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderRequest;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderResponse;
import com.example.vnkapp.dto.payment.ngenius.NgeniusWebhookPayload;
import com.example.vnkapp.entity.Order;
import com.example.vnkapp.entity.Payment;
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

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NgeniusPaymentService ngeniusPaymentService;
    private final NgeniusProperties properties;
    private final ObjectMapper gatewayObjectMapper = new ObjectMapper();

    public PaymentService(PaymentRepository paymentRepository,
                           OrderRepository orderRepository,
                           NgeniusPaymentService ngeniusPaymentService,
                           NgeniusProperties properties) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.ngeniusPaymentService = ngeniusPaymentService;
        this.properties = properties;
    }

    /**
     * Creates a payment session with the gateway for a freshly placed order and persists a
     * PENDING Payment record with the hosted-page redirect URL. Throws on any gateway failure
     * so the caller's transaction (order creation) rolls back rather than leaving an order
     * with no way to pay for it.
     */
    @Transactional
    public Payment initiateNgeniusPayment(Order order) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Payment gateway is not configured. Set NGENIUS_API_KEY and NGENIUS_OUTLET_REF.");
        }

        NgeniusOrderRequest request = buildOrderRequest(order);

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

    /**
     * Replaces a stale/failed payment session with a new N-Genius hosted-page link for a
     * pending order that has not been paid yet.
     */
    @Transactional
    public Payment retryNgeniusPayment(Order order) {
        paymentRepository.findByOrderIdActive(order.getId()).ifPresent(existing -> {
            if (existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new IllegalArgumentException("Order is already paid");
            }
            existing.setStatus(com.example.vnkapp.entity.BaseEntity.STATUS_INACTIVE);
            paymentRepository.save(existing);
        });
        return initiateNgeniusPayment(order);
    }

    public boolean isValidWebhookRequest(HttpServletRequest request) {
        String expected = properties.getWebhookHeaderValue();
        if (expected == null || expected.isBlank()) {
            log.warn("ngenius.webhook-header-value is not configured - accepting webhook without secret validation");
            return true;
        }
        return expected.equals(request.getHeader(properties.getWebhookHeaderName()));
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

        payment.setPaymentStatus(newStatus);
        if (payload.order().id() != null) {
            payment.setGatewayOrderId(payload.order().id());
        }
        applyGatewayPaymentDetails(payment, payload);
        payment.setGatewayResponse(toJson(payload));

        switch (newStatus) {
            case COMPLETED -> {
                payment.setPaidAt(Instant.now());
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            case FAILED -> payment.setFailureReason(payload.eventName());
            case REFUNDED -> {
                payment.setRefundedAt(Instant.now());
                order.setOrderStatus(OrderStatus.REFUNDED);
            }
            case PARTIALLY_REFUNDED -> payment.setRefundedAt(Instant.now());
            default -> { /* PROCESSING - no side effects beyond the status update above */ }
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
        log.info("Processed N-Genius webhook eventId={} '{}' for order {}: paymentStatus={}",
                payload.eventId(), payload.eventName(), order.getOrderNumber(), newStatus);
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

    private NgeniusOrderRequest buildOrderRequest(Order order) {
        NgeniusOrderRequest.Amount amount = new NgeniusOrderRequest.Amount(
                properties.getCurrency(), toMinorUnits(order.getTotalAmount()));

        NgeniusOrderRequest.MerchantAttributes merchantAttributes = new NgeniusOrderRequest.MerchantAttributes(
                String.valueOf(properties.getPaymentAttempts()),
                properties.getCancelUrl(),
                properties.getRedirectUrl(),
                true);

        NgeniusOrderRequest.BillingAddress billingAddress = new NgeniusOrderRequest.BillingAddress(
                firstNameOf(order.getShippingFullName()),
                lastNameOf(order.getShippingFullName()),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingCountry(),
                countryCodeOf(order.getShippingCountry()),
                order.getShippingPostalCode());

        return new NgeniusOrderRequest(
                properties.getAction(),
                amount,
                order.getOrderNumber(),
                null,
                merchantAttributes,
                billingAddress);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String firstNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        return fullName.trim().split("\\s+", 2)[0];
    }

    private String lastNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : null;
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
