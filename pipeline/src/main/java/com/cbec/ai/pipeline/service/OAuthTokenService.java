package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.config.TradeTariffConfiguration;
import com.cbec.ai.pipeline.exception.OAuthAuthenticationException;
import com.cbec.ai.pipeline.model.dto.OAuthTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class OAuthTokenService {

    private final TradeTariffConfiguration config;
    private final RestTemplate restTemplate;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiryTime;

    public OAuthTokenService(TradeTariffConfiguration config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Retrieves a valid cached access token, automatically refreshing before expiry.
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            return cachedAccessToken;
        }

        lock.lock();
        try {
            if (isTokenValid()) {
                return cachedAccessToken;
            }
            return fetchNewAccessToken();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Forces an immediate token refresh (e.g. after receiving HTTP 401 Unauthorized).
     */
    public String forceRefreshToken() {
        lock.lock();
        try {
            log.info("Force refreshing UK Trade Tariff OAuth2 access token...");
            return fetchNewAccessToken();
        } finally {
            lock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedAccessToken != null && tokenExpiryTime != null && Instant.now().plusSeconds(60).isBefore(tokenExpiryTime);
    }

    private String fetchNewAccessToken() {
        log.info("Authentication started: Requesting OAuth2 client_credentials token from {}", config.getTokenUrl());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", config.getClientId());
            body.add("client_secret", config.getClientSecret());

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(
                    config.getTokenUrl(), requestEntity, OAuthTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OAuthTokenResponse tokenResponse = response.getBody();
                this.cachedAccessToken = tokenResponse.getAccessToken();

                long expiresInSeconds = tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600L;
                this.tokenExpiryTime = Instant.now().plusSeconds(expiresInSeconds);

                log.info("Token acquired successfully. Access token type: {}, expires in: {}s",
                        tokenResponse.getTokenType(), expiresInSeconds);

                return this.cachedAccessToken;
            } else {
                throw new OAuthAuthenticationException("Failed acquiring OAuth token. HTTP Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("OAuth authentication failed for UK Trade Tariff API", e);
            throw new OAuthAuthenticationException("OAuth authentication failed: " + e.getMessage(), e);
        }
    }

    public void clearCacheForTesting() {
        lock.lock();
        try {
            this.cachedAccessToken = null;
            this.tokenExpiryTime = null;
        } finally {
            lock.unlock();
        }
    }
}
