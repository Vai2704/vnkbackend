package com.example.vnkapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs N-Genius configuration status at startup so missing credentials are obvious
 * before the first checkout attempt fails.
 */
@Component
public class NgeniusStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NgeniusStartupLogger.class);

    private final NgeniusProperties properties;

    public NgeniusStartupLogger(NgeniusProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isConfigured()) {
            log.info("N-Genius payment gateway configured: outletRef={}, currency={}, apiBaseUrl={}",
                    properties.getOutletRef(), properties.getCurrency(), properties.getApiBaseUrl());
        } else {
            log.warn("N-Genius payment gateway is NOT fully configured. Set NGENIUS_API_KEY "
                    + "(and verify NGENIUS_OUTLET_REF). Checkout payment initiation will fail until configured.");
        }
        if (properties.getWebhookHeaderValue() == null || properties.getWebhookHeaderValue().isBlank()) {
            log.warn("ngenius.webhook-header-value is not set — webhook endpoint will accept "
                    + "requests without secret validation.");
        }
    }
}
