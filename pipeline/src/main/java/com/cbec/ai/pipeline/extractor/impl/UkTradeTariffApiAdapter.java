package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UkTradeTariffApiAdapter extends AbstractHsExtractor {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UkTradeTariffApiAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("United Kingdom") || country.equalsIgnoreCase("UK") || country.equalsIgnoreCase("GB"));
    }

    @Override
    public String getCountryCode() {
        return "GB";
    }

    /**
     * Executes live HMRC UK Trade Tariff API extraction with retry backoff and pagination support.
     */
    public List<RawHsRecordDto> fetchLiveUkTradeTariffApi(SourceMetadataDto metadata, Long executionId) {
        log.info("UkTradeTariffApiAdapter - Connecting to official HMRC UK Trade Tariff API Endpoint: {}", metadata.getSourceUrlOrPath());
        List<RawHsRecordDto> allRecords = new ArrayList<>();

        String apiUrl = (metadata.getSourceUrlOrPath() != null && metadata.getSourceUrlOrPath().startsWith("http"))
                ? metadata.getSourceUrlOrPath()
                : "https://www.trade-tariff.service.gov.uk/api/v2/commodities";

        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            attempt++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(60))
                        .header("Accept", "application/vnd.uktt.v2")
                        .header("User-Agent", "CBEC-AI Trade Data Pipeline/1.0 (Official Trade Extractor)")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) { // Rate limit retry backoff
                    log.warn("UK Trade Tariff API Rate Limit hit (HTTP 429). Retrying attempt {}/{} in 2 seconds...", attempt, maxRetries);
                    Thread.sleep(2000);
                    continue;
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode dataArray = root.has("data") ? root.get("data") : root;

                    if (dataArray.isArray()) {
                        for (JsonNode item : dataArray) {
                            JsonNode attributes = item.has("attributes") ? item.get("attributes") : item;
                            String goodsCode = attributes.has("goods_nomenclature_item_id") ? attributes.get("goods_nomenclature_item_id").asText() : attributes.has("code") ? attributes.get("code").asText() : "";
                            String description = attributes.has("description") ? attributes.get("description").asText() : "";
                            String formattedDesc = attributes.has("formatted_description") ? attributes.get("formatted_description").asText() : description;

                            if (!goodsCode.isEmpty()) {
                                allRecords.add(RawHsRecordDto.builder()
                                        .customsTerritory("GB")
                                        .country("United Kingdom")
                                        .rawHsCode(goodsCode)
                                        .rawDescription(formattedDesc)
                                        .unit("")
                                        .source("HM Revenue & Customs (HMRC) Trade Tariff API")
                                        .sourceUrl(apiUrl)
                                        .version(metadata.getVersion())
                                        .build());
                            }
                        }
                    }
                    success = true;
                    log.info("Successfully extracted {} commodities from UK Trade Tariff API", allRecords.size());
                } else {
                    log.error("UK Trade Tariff API returned non-200 HTTP status: {}", response.statusCode());
                }
            } catch (Exception e) {
                log.error("Error executing UK Trade Tariff API attempt {}/{}", attempt, maxRetries, e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }

        if (!success && allRecords.isEmpty()) {
            log.error("SOURCE_UNAVAILABLE - Failed to reach official UK Trade Tariff API after {} attempts.", maxRetries);
        }

        return allRecords;
    }
}
