package com.example.vnkapp.dto.payment.ngenius;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * N-Genius webhook body. Extra fields are ignored so payload evolution does not break parsing.
 * See https://docs.ngenius-payments.com/reference/consuming-web-hooks
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NgeniusWebhookPayload(
        String outletId,
        String eventId,
        String eventName,
        Order order
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(
            @JsonProperty("_id") String id,
            String reference,
            Amount amount,
            String action,
            @JsonProperty("_embedded") Embedded embedded
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(List<Payment> payment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            @JsonProperty("_id") String id,
            String reference,
            String state,
            Amount amount,
            String orderReference,
            AuthResponse authResponse
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthResponse(
            String authorizationCode,
            String rrn,
            String mid,
            Boolean success,
            String resultCode,
            String resultMessage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(String currencyCode, Long value) {}

    public String firstPaymentState() {
        if (order == null || order.embedded() == null || order.embedded().payment() == null
                || order.embedded().payment().isEmpty()) {
            return null;
        }
        return order.embedded().payment().get(0).state();
    }

    public Payment firstPayment() {
        if (order == null || order.embedded() == null || order.embedded().payment() == null
                || order.embedded().payment().isEmpty()) {
            return null;
        }
        return order.embedded().payment().get(0);
    }
}
