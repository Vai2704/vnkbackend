package com.example.vnkapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credentials/config are provided per-environment via NGENIUS_* env vars (see .env.example).
 */
@Component
@ConfigurationProperties(prefix = "ngenius")
@Getter
@Setter
public class NgeniusProperties {

    private String authUrl = "https://api-gateway.sandbox.ngenius-payments.com/identity/auth/access-token";

    private String apiBaseUrl = "https://api-gateway.sandbox.ngenius-payments.com";

    private String outletRef;

    /** Value sent as-is in "Authorization: Basic <apiKey>" when requesting an access token. */
    private String apiKey;

    private String action = "SALE";

    private String currency = "AED";

    private String redirectUrl;

    private String cancelUrl;

    private int paymentAttempts = 3;

    /** Custom header name configured on the N-Genius dashboard for webhook authenticity checks. */
    private String webhookHeaderName = "X-Webhook-Secret";

    /** Expected value of the webhook header above. */
    private String webhookHeaderValue;

    public boolean isConfigured() {
        return outletRef != null && !outletRef.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }
}
