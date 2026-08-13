package com.example.vnkapp.service;

import com.example.vnkapp.config.NgeniusProperties;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderRequest;
import com.example.vnkapp.dto.payment.ngenius.NgeniusOrderResponse;
import com.example.vnkapp.dto.payment.ngenius.NgeniusTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Thin HTTP client for the N-Genius (Network International) payment gateway sandbox API.
 * Handles access-token acquisition/caching (tokens expire after 300s per the gateway) and
 * order creation. See https://docs.ngenius-payments.com/reference/consuming-web-hooks and
 * the "request an access token" / "creating orders" reference pages.
 */
@Service
public class NgeniusPaymentService {

    private static final Logger log = LoggerFactory.getLogger(NgeniusPaymentService.class);

    private static final MediaType IDENTITY_MEDIA_TYPE = MediaType.valueOf("application/vnd.ni-identity.v1+json");
    private static final MediaType PAYMENT_MEDIA_TYPE = MediaType.valueOf("application/vnd.ni-payment.v2+json");

    // Refresh a little before the gateway-reported expiry to avoid using a token that
    // expires mid-flight on a slow request.
    private static final Duration TOKEN_SAFETY_MARGIN = Duration.ofSeconds(20);
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 300;

    private static final String TOKEN_REQUEST_BODY = "{}";

    private final RestClient restClient;
    private final NgeniusProperties properties;
    private final Object tokenLock = new Object();

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public NgeniusPaymentService(NgeniusProperties properties) {
        this.properties = properties;
        ObjectMapper objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(List.of(
                MediaType.TEXT_PLAIN,
                MediaType.APPLICATION_JSON,
                IDENTITY_MEDIA_TYPE,
                PAYMENT_MEDIA_TYPE,
                MediaType.ALL));

        this.restClient = RestClient.builder()
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(stringConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }

    public NgeniusOrderResponse createOrder(NgeniusOrderRequest orderRequest) {
        try {
            return callCreateOrder(orderRequest, getAccessToken(false));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                log.warn("N-Genius rejected access token as unauthorized, refreshing and retrying once");
                return callCreateOrder(orderRequest, getAccessToken(true));
            }
            log.error("N-Genius create-order failed: status={}, body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw ex;
        }
    }

    private NgeniusOrderResponse callCreateOrder(NgeniusOrderRequest orderRequest, String accessToken) {
        String uri = properties.getApiBaseUrl() + "/transactions/outlets/" + properties.getOutletRef() + "/orders";
        return restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, PAYMENT_MEDIA_TYPE.toString())
                .header(HttpHeaders.ACCEPT, PAYMENT_MEDIA_TYPE.toString())
                .body(orderRequest)
                .retrieve()
                .body(NgeniusOrderResponse.class);
    }

    private String getAccessToken(boolean forceRefresh) {
        Instant now = Instant.now();
        if (!forceRefresh && cachedAccessToken != null && now.isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }
        synchronized (tokenLock) {
            now = Instant.now();
            if (!forceRefresh && cachedAccessToken != null && now.isBefore(tokenExpiresAt)) {
                return cachedAccessToken;
            }
            return fetchAndCacheAccessToken(now);
        }
    }

    private String fetchAndCacheAccessToken(Instant now) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "N-Genius API key is not configured. Set ngenius.api-key or NGENIUS_API_KEY.");
        }

        NgeniusTokenResponse response;
        String authorization = buildBasicAuthorization(apiKey);
        try {
            // N-Genius/CDN requires Content-Length on POST. Empty string body causes 411;
            // send "{}" like curl -d '{}' (matches identity API expectations).
            log.debug("Requesting N-Genius access token with Authorization: Basic <configured>");
            response = restClient.post()
                    .uri(properties.getAuthUrl())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(IDENTITY_MEDIA_TYPE)
                    .accept(IDENTITY_MEDIA_TYPE)
                    .body(TOKEN_REQUEST_BODY)
                    .retrieve()
                    .body(NgeniusTokenResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("N-Genius access-token request failed: status={}, body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Failed to obtain N-Genius access token", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to obtain N-Genius access token", ex);
        }

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("N-Genius access-token response did not contain an access_token");
        }

        long ttlSeconds = response.expiresIn() != null && response.expiresIn() > 0
                ? response.expiresIn()
                : DEFAULT_TOKEN_TTL_SECONDS;

        cachedAccessToken = response.accessToken();
        tokenExpiresAt = now.plusSeconds(ttlSeconds).minus(TOKEN_SAFETY_MARGIN);
        log.info("Fetched new N-Genius access token, expires in {}s", ttlSeconds);
        return cachedAccessToken;
    }

    /**
     * Builds {@code Authorization: Basic &lt;api-key&gt;}. The configured api-key is the
     * Base64(clientId:clientSecret) value from the N-Genius portal — we always prepend
     * {@code Basic } unless the config already includes it.
     */
    private String buildBasicAuthorization(String apiKey) {
        String trimmed = apiKey.trim();
        if (trimmed.regionMatches(true, 0, "Basic ", 0, 6)) {
            return trimmed;
        }
        return "Basic " + trimmed;
    }
}
