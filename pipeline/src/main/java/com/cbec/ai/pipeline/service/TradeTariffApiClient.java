package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.config.TradeTariffConfiguration;
import com.cbec.ai.pipeline.exception.OAuthAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class TradeTariffApiClient {

    private final OAuthTokenService tokenService;
    private final TradeTariffConfiguration config;
    private final RestTemplate restTemplate;

    public TradeTariffApiClient(OAuthTokenService tokenService, TradeTariffConfiguration config) {
        this.tokenService = tokenService;
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Executes GET request against UK Trade Tariff API with automatic OAuth token injection and 401 retry.
     */
    public String fetchTariffData(String endpointPath) {
        String fullUrl = endpointPath.startsWith("http") ? endpointPath : config.getApiBaseUrl() + endpointPath;
        log.info("API request: GET {}", fullUrl);

        int maxRetries = 3;
        int attempt = 0;
        boolean retried401 = false;

        while (attempt < maxRetries) {
            attempt++;
            try {
                String token = tokenService.getAccessToken();

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);

                log.info("Response status: {}", response.getStatusCode());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }

            } catch (HttpClientErrorException.Unauthorized e) {
                log.warn("Received HTTP 401 Unauthorized from UK Trade Tariff API. Attempting token refresh (Retry 401)...");
                if (!retried401) {
                    retried401 = true;
                    tokenService.forceRefreshToken();
                    continue;
                } else {
                    throw new OAuthAuthenticationException("HTTP 401 Unauthorized persisted after token refresh", e);
                }

            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Received HTTP 429 Rate Limit from UK Trade Tariff API. Exponential backoff...");
                long backoffMs = getRetryAfterSeconds(e.getResponseHeaders()) * 1000L;
                if (backoffMs <= 0) backoffMs = (long) Math.pow(2, attempt) * 1000L;
                sleep(backoffMs);

            } catch (HttpServerErrorException e) {
                log.warn("Received HTTP Server Error {} from UK Trade Tariff API. Retrying attempt {}/{}", e.getStatusCode(), attempt, maxRetries);
                sleep((long) Math.pow(2, attempt) * 1000L);

            } catch (Exception e) {
                log.error("Unexpected error fetching UK Trade Tariff API endpoint {}", fullUrl, e);
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed fetching UK Trade Tariff API: " + e.getMessage(), e);
                }
                sleep(1000L * attempt);
            }
        }

        throw new RuntimeException("Exhausted retries calling UK Trade Tariff API: " + fullUrl);
    }

    private long getRetryAfterSeconds(HttpHeaders headers) {
        if (headers != null && headers.containsKey("Retry-After")) {
            try {
                String val = headers.getFirst("Retry-After");
                return Long.parseLong(val);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
