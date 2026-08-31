package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.payment.PaymentCallbackResponseDto;
import com.example.vnkapp.dto.payment.ngenius.NgeniusWebhookPayload;
import com.example.vnkapp.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * N-Genius payment webhook endpoint.
 * See https://docs.ngenius-payments.com/reference/consuming-web-hooks
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Browser callback after N-Genius hosted pay page redirect.
     * Verifies the payment with N-Genius and confirms the order when captured.
     */
    @GetMapping("/ngenius/callback")
    public ResponseEntity<?> confirmFromRedirect(
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String orderRef,
            @RequestParam(required = false) String reference) {
        String resolved = firstNonBlank(ref, orderRef, reference);
        log.info("N-Genius redirect callback, ref={}", resolved);
        try {
            PaymentCallbackResponseDto result = paymentService.confirmFromRedirect(resolved);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, result));
        } catch (IllegalArgumentException ex) {
            log.warn("N-Genius redirect callback rejected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDto<>("Error", ex.getMessage(), null));
        } catch (Exception ex) {
            log.error("N-Genius redirect callback failed, ref={}", resolved, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ApiResponseDto<>("Error",
                            "Unable to confirm payment with the payment gateway. Please try again.", null));
        }
    }

    /**
     * N-Genius sends one POST per event with no retries. Respond 200/201 within 15 seconds.
     */
    @PostMapping("/webhooks/ngenius")
    public ResponseEntity<Void> handleNgeniusWebhook(
            HttpServletRequest request,
            @RequestBody NgeniusWebhookPayload payload) {
        log.info("Received N-Genius webhook: eventId={}, eventName={}, outletId={}, orderRef={}, paymentState={}",
                payload.eventId(),
                payload.eventName(),
                payload.outletId(),
                payload.order() != null ? payload.order().reference() : null,
                payload.firstPaymentState());

        if (!paymentService.isValidWebhookRequest(request)) {
            log.warn("Rejected N-Genius webhook eventId={} - invalid/missing secret header", payload.eventId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            paymentService.handleNgeniusWebhook(payload);
        } catch (Exception ex) {
            // Acknowledge anyway — N-Genius does not retry lost events.
            log.error("Error processing N-Genius webhook eventId={}", payload.eventId(), ex);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
