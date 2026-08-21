package com.cbec.ai.pipeline.util;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class OfficialSourceFetcher {

    private final HttpClient httpClient;

    public OfficialSourceFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Downloads live official dataset payload directly from a government portal URL.
     */
    public InputStream fetchFromUrl(String url) {
        log.info("OfficialSourceFetcher - Fetching live data from official URL: {}", url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("User-Agent", "CBEC-AI Trade Data Pipeline/1.0 (Official Trade Data Extractor)")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("OfficialSourceFetcher - Successfully connected to official URL. HTTP Status: {}", response.statusCode());
                return response.body();
            } else {
                throw new HsPipelineException("Failed to fetch data from official portal. HTTP Status Code: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error fetching live official dataset from URL: {}", url, e);
            throw new HsPipelineException("Unable to fetch data from official source URL: " + e.getMessage(), e);
        }
    }
}
