package com.example.vnkapp.controller;

import com.example.vnkapp.dto.payment.ngenius.NgeniusWebhookPayload;
import com.example.vnkapp.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
