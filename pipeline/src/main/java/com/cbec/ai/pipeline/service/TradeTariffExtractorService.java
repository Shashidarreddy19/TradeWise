package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TradeTariffExtractorService {

    private final HsRawRepository hsRawRepository;
    private final ObjectMapper objectMapper;

    public TradeTariffExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Data
    @Builder
    public static class UkExtractionSummary {
        private String country;
        private long totalExtracted;
        private long totalInserted;
        private long totalSkipped;
        private String datasetVersion;
    }

    /**
     * Extracts official UK Trade Tariff 10-digit tariff records from downloaded HMRC JSON payload into `hs_raw`.
     */
    public UkExtractionSummary extractUkTariffData(String filePath, Long executionId, String datasetVersion) {
        String version = datasetVersion != null ? datasetVersion : "UK_TARIFF_2026";
        log.info("Starting UK Trade Tariff raw extraction from HMRC payload file: {}", filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("UK Trade Tariff JSON file not found at: " + filePath);
        }

        List<HsRawEntity> batch = new ArrayList<>();
        long totalExtracted = 0;
        long totalInserted = 0;
        long totalSkipped = 0;

        try {
            JsonNode root = objectMapper.readTree(file);
            JsonNode dataArray = root.has("data") ? root.get("data") : root;

            if (dataArray.isArray()) {
                for (JsonNode node : dataArray) {
                    JsonNode attr = node.has("attributes") ? node.get("attributes") : node;

                    String codeRaw = attr.has("goods_nomenclature_item_id") ? attr.get("goods_nomenclature_item_id").asText() :
                            (node.has("id") ? node.get("id").asText() : null);

                    String description = attr.has("description_plain") && !attr.get("description_plain").isNull() ? attr.get("description_plain").asText() :
                            (attr.has("description") && !attr.get("description").isNull() ? attr.get("description").asText() :
                            (attr.has("formatted_description") && !attr.get("formatted_description").isNull() ? attr.get("formatted_description").asText() : null));

                    String unit = attr.has("unit") && !attr.get("unit").isNull() ? attr.get("unit").asText() : null;
                    String suppUnit = attr.has("supplementary_unit") && !attr.get("supplementary_unit").isNull() ? attr.get("supplementary_unit").asText() : null;

                    if (codeRaw == null || description == null || description.trim().isEmpty()) {
                        totalSkipped++;
                        continue;
                    }

                    String cleanCode = codeRaw.replaceAll("[^0-9]", "").trim();

                    // Pad 8-digit CN codes to 10 digits for standard UK National Code
                    if (cleanCode.length() == 8) {
                        cleanCode = cleanCode + "00";
                    }

                    if (cleanCode.length() != 10) {
                        totalSkipped++;
                        continue;
                    }

                    totalExtracted++;
                    String finalUnit = unit != null ? unit.trim() : suppUnit;

                    HsRawEntity raw = HsRawEntity.builder()
                            .executionId(executionId != null ? executionId : 1L)
                            .customsTerritory("GB")
                            .country("United Kingdom")
                            .rawNationalCode(cleanCode)
                            .rawDescription(description.trim())
                            .unit(finalUnit)
                            .sourceName("HMRC UK Trade Tariff API v2")
                            .datasetVersion(version)
                            .build();

                    batch.add(raw);

                    if (batch.size() >= 500) {
                        hsRawRepository.saveAll(batch);
                        totalInserted += batch.size();
                        batch.clear();
                    }
                }
            }

            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                totalInserted += batch.size();
                batch.clear();
            }

            log.info("UK Trade Tariff Extraction Complete. Extracted: {}, Inserted into hs_raw: {}, Skipped: {}",
                    totalExtracted, totalInserted, totalSkipped);

            return UkExtractionSummary.builder()
                    .country("United Kingdom")
                    .totalExtracted(totalExtracted)
                    .totalInserted(totalInserted)
                    .totalSkipped(totalSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during UK Trade Tariff raw extraction", e);
            throw new RuntimeException("Failed extracting UK Trade Tariff raw data: " + e.getMessage(), e);
        }
    }
}
