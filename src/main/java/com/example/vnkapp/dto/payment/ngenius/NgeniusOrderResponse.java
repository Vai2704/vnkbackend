package com.example.vnkapp.dto.payment.ngenius;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body from POST /transactions/outlets/{outletRef}/orders.
 * Only the fields we actually use are mapped; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NgeniusOrderResponse(
        @JsonProperty("_id") String id,
        String reference,
        @JsonProperty("_links") Links links,
        @JsonProperty("_embedded") Embedded embedded
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Links(Link payment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(String href) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(List<Payment> payment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            @JsonProperty("_id") String id,
            String reference,
            String state
    ) {}

    @JsonIgnore
    public String paymentUrl() {
        return links != null && links.payment() != null ? links.payment().href() : null;
    }

    @JsonIgnore
    public String paymentId() {
        if (embedded == null || embedded.payment() == null || embedded.payment().isEmpty()) {
            return null;
        }
        Payment payment = embedded.payment().get(0);
        return payment.reference() != null ? payment.reference() : payment.id();
    }
}
