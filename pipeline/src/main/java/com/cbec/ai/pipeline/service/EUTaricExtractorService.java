package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
public class EUTaricExtractorService {

    private final HsRawRepository hsRawRepository;
    private final XmlMapper xmlMapper;

    public EUTaricExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
        this.xmlMapper = new XmlMapper();
    }

    @Data
    @Builder
    public static class EUTaricExtractionResult {
        private String country;
        private long totalExtracted;
        private long totalInserted;
        private long totalSkipped;
        private String datasetVersion;
    }

    /**
     * Extracts valid 8-digit CN or 10-digit TARIC codes from XML/CSV file into `hs_raw`.
     */
    public EUTaricExtractionResult extractEuTaricData(String country, String filePath, Long executionId, String datasetVersion) {
        String targetCountry = country != null ? country : "Germany";
        String version = datasetVersion != null ? datasetVersion : "EU_TARIC_2026";
        log.info("Starting EU TARIC raw extraction for {} from file: {}", targetCountry, filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("EU TARIC file not found at: " + filePath);
        }

        List<HsRawEntity> rawBatch = new ArrayList<>();
        long totalExtracted = 0;
        long totalInserted = 0;
        long totalSkipped = 0;

        try (InputStream is = new FileInputStream(file)) {
            JsonNode root = xmlMapper.readTree(is);
            JsonNode items = root.has("goodsItems") ? root.get("goodsItems") : root;

            if (items.has("goodsItem")) {
                items = items.get("goodsItem");
            }

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String goodsCode = item.has("goodsCode") ? item.get("goodsCode").asText() : null;
                    String description = item.has("goodsDescription") ? item.get("goodsDescription").asText() : null;
                    String unit = item.has("unit") ? item.get("unit").asText() : null;

                    if (goodsCode == null || description == null || description.trim().isEmpty()) {
                        totalSkipped++;
                        continue;
                    }

                    // Clean non-digit characters (dots, spaces)
                    String cleanCode = goodsCode.replaceAll("[^0-9]", "").trim();

                    // Code length must be 8 (Combined Nomenclature) or 10 (TARIC)
                    if (cleanCode.length() != 8 && cleanCode.length() != 10) {
                        log.warn("Skipping unsupported EU TARIC code length: {}", goodsCode);
                        totalSkipped++;
                        continue;
                    }

                    totalExtracted++;

                    HsRawEntity raw = HsRawEntity.builder()
                            .executionId(executionId != null ? executionId : 1L)
                            .customsTerritory("EU")
                            .country(targetCountry)
                            .rawNationalCode(cleanCode)
                            .rawDescription(description.trim())
                            .unit(unit != null ? unit.trim() : null)
                            .sourceName("EU Combined Nomenclature & TARIC (" + targetCountry + ")")
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

            log.info("EU TARIC Extraction Complete for {}. Extracted: {}, Inserted into hs_raw: {}, Skipped: {}",
                    targetCountry, totalExtracted, totalInserted, totalSkipped);

            return EUTaricExtractionResult.builder()
                    .country(targetCountry)
                    .totalExtracted(totalExtracted)
                    .totalInserted(totalInserted)
                    .totalSkipped(totalSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during EU TARIC raw extraction for {}", targetCountry, e);
            throw new RuntimeException("Failed extracting EU TARIC raw data for " + targetCountry + ": " + e.getMessage(), e);
        }
    }
}
