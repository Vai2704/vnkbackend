package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.payment.PaymentCallbackResponseDto;
import com.example.vnkapp.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

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
     * Body is read as raw bytes so vendor Content-Types (not only application/json) still work.
     */
    @PostMapping(value = "/webhooks/ngenius", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> handleNgeniusWebhook(HttpServletRequest request) throws IOException {
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("N-Genius webhook POST received: contentType={}, bodyLength={}, headerNames={}, secretHeaderPresent={}",
                request.getContentType(),
                rawBody.length(),
                Collections.list(request.getHeaderNames()),
                paymentService.isValidWebhookRequest(request));

        if (!paymentService.isValidWebhookRequest(request)) {
            log.warn("Rejected N-Genius webhook - missing/invalid {} header. Body preview: {}",
                    "X-Webhook-Secret",
                    rawBody.length() > 500 ? rawBody.substring(0, 500) : rawBody);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            paymentService.handleNgeniusWebhookRaw(rawBody);
        } catch (Exception ex) {
            log.error("Error processing N-Genius webhook body={}", rawBody, ex);
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
