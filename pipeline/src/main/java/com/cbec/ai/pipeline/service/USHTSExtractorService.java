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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class USHTSExtractorService {

    private final HsRawRepository hsRawRepository;
    private final ObjectMapper objectMapper;

    public USHTSExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Data
    @Builder
    public static class USHTSExtractionResult {
        private long totalExtracted;
        private long totalInserted;
        private long totalSkipped;
        private String datasetVersion;
    }

    /**
     * Extracts valid 10-digit US HTS tariff lines from JSON/CSV payload into `hs_raw`.
     */
    public USHTSExtractionResult extractUsHtsData(String filePath, Long executionId, String datasetVersion) {
        log.info("Starting US HTS raw extraction from: {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found at: " + filePath);
        }

        List<HsRawEntity> rawBatch = new ArrayList<>();
        long totalExtracted = 0;
        long totalInserted = 0;
        long totalSkipped = 0;

        String version = datasetVersion != null ? datasetVersion : "US_HTS_2026";

        try (InputStream is = new FileInputStream(file)) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode items = root.has("items") ? root.get("items") : root;

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String htsno = item.has("htsno") ? item.get("htsno").asText() : null;
                    String description = item.has("description") ? item.get("description").asText() : null;
                    String unit = item.has("unit") ? item.get("unit").asText() : null;

                    if (htsno == null || description == null || description.trim().isEmpty()) {
                        totalSkipped++;
                        continue;
                    }

                    // Normalize HTS code: remove non-digit characters
                    String cleanedCode = htsno.replaceAll("[^0-9]", "").trim();

                    // If 8 digits, pad "00" to make 10 digits
                    if (cleanedCode.length() == 8) {
                        cleanedCode = cleanedCode + "00";
                    }

                    // Validate exactly 10 numeric digits
                    if (cleanedCode.length() != 10 || !cleanedCode.matches("^\\d{10}$")) {
                        log.warn("Skipping invalid US HTS code length: {}", htsno);
                        totalSkipped++;
                        continue;
                    }

                    totalExtracted++;

                    HsRawEntity raw = HsRawEntity.builder()
                            .executionId(executionId != null ? executionId : 1L)
                            .customsTerritory("US")
                            .country("United States")
                            .rawNationalCode(cleanedCode)
                            .rawDescription(description.trim())
                            .unit(unit != null ? unit.trim() : null)
                            .sourceName("US Harmonized Tariff Schedule (USITC)")
                            .datasetVersion(version)
                            .build();

                    rawBatch.add(raw);

                    if (rawBatch.size() >= 500) {
                        hsRawRepository.saveAll(rawBatch);
                        totalInserted += rawBatch.size();
                        rawBatch.clear();
                    }
                }
            }

            if (!rawBatch.isEmpty()) {
                hsRawRepository.saveAll(rawBatch);
                totalInserted += rawBatch.size();
                rawBatch.clear();
            }

            log.info("US HTS Extraction Complete. Extracted: {}, Inserted into hs_raw: {}, Skipped: {}",
                    totalExtracted, totalInserted, totalSkipped);

            return USHTSExtractionResult.builder()
                    .totalExtracted(totalExtracted)
                    .totalInserted(totalInserted)
                    .totalSkipped(totalSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during US HTS raw extraction", e);
            throw new RuntimeException("Failed extracting US HTS raw data: " + e.getMessage(), e);
        }
    }
}
