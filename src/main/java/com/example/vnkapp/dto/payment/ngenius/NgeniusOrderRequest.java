package com.example.vnkapp.dto.payment.ngenius;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for POST /transactions/outlets/{outletRef}/orders.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NgeniusOrderRequest(
        String action,
        Amount amount,
        String reference,
        String emailAddress,
        MerchantAttributes merchantAttributes,
        BillingAddress billingAddress
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Amount(String currencyCode, long value) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MerchantAttributes(
            String paymentAttempts,
            String cancelUrl,
            String redirectUrl,
            Boolean showPayerName
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BillingAddress(
            String firstName,
            String lastName,
            String city,
            String state,
            String country,
            String countryCode,
            String postalCode
    ) {}
}
