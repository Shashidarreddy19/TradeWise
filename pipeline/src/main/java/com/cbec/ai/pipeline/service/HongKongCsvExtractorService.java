package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HongKongCsvExtractorService {

    private final HsRawRepository hsRawRepository;

    public HongKongCsvExtractorService(HsRawRepository hsRawRepository) {
        this.hsRawRepository = hsRawRepository;
    }

    @Data
    @Builder
    public static class HkExtractionResult {
        private String fileName;
        private long rowsScanned;
        private long rowsExtracted;
        private long rowsInserted;
        private long rowsSkipped;
        private String datasetVersion;
    }

    /**
     * OpenCSV Streaming reader extracting all official HKHS tariff lines from B2XX00232026XXXXB0100 (1).csv into `hs_raw`.
     */
    public HkExtractionResult extractHongKongCsvData(String csvPath, Long executionId, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String filePath = csvPath != null ? csvPath : "B2XX00232026XXXXB0100 (1).csv";
        String version = datasetVersion != null ? datasetVersion : "HKHS_2026";

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("Hong Kong CSV file not found at path: {}", filePath);
            throw new IllegalArgumentException("Hong Kong CSV file not found at path: " + filePath);
        }

        log.info("Starting OpenCSV streaming extraction for Hong Kong HKHS from: {}", file.getAbsolutePath());

        List<HsRawEntity> batch = new ArrayList<>();
        long rowsScanned = 0;
        long rowsExtracted = 0;
        long rowsInserted = 0;
        long rowsSkipped = 0;

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(isr).build()) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                rowsScanned++;

                if (line.length < 3) {
                    rowsSkipped++;
                    continue;
                }

                String col0 = line[0] != null ? line[0].trim() : "";
                String col1 = line.length > 1 && line[1] != null ? line[1].trim() : "";
                String col2 = line.length > 2 && line[2] != null ? line[2].trim() : "";
                String col5 = line.length > 5 && line[5] != null ? line[5].trim() : "";

                // Automatically skip header rows
                if (col0.contains("港貨協制編號") || col0.contains("HKHS Code") || col2.contains("Goods Description")) {
                    rowsSkipped++;
                    continue;
                }

                String cleanCode = col0.replaceAll("[^0-9]", "").trim();

                // If leading zero was lost e.g. "1012100" (7 digits) -> format to "01012100" (8 digits)
                if (cleanCode.length() == 7) {
                    cleanCode = "0" + cleanCode;
                }

                // National code must contain exactly 8 digits
                if (cleanCode.length() != 8) {
                    log.warn("Skipping row {}: Invalid HKHS code length (must be 8 digits): {}", rowsScanned, col0);
                    rowsSkipped++;
                    continue;
                }

                String descEnglish = col2.isEmpty() ? col1 : col2;
                if (descEnglish.isEmpty()) {
                    rowsSkipped++;
                    continue;
                }

                rowsExtracted++;

                HsRawEntity raw = HsRawEntity.builder()
                        .executionId(executionId != null ? executionId : 1L)
                        .customsTerritory("HK")
                        .country("Hong Kong")
                        .rawNationalCode(cleanCode)
                        .rawDescription(descEnglish)
                        .unit(!col5.isEmpty() ? col5 : null)
                        .sourceName("Hong Kong Harmonized System CSV")
                        .datasetVersion(version)
                        .build();

                batch.add(raw);

                if (batch.size() >= 500) {
                    hsRawRepository.saveAll(batch);
                    rowsInserted += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                hsRawRepository.saveAll(batch);
                rowsInserted += batch.size();
                batch.clear();
            }

            log.info("OpenCSV Extraction Complete for Hong Kong. Scanned: {}, Extracted: {}, Inserted into hs_raw: {}, Skipped: {}, Time: {} ms",
                    rowsScanned, rowsExtracted, rowsInserted, rowsSkipped, (System.currentTimeMillis() - startTime));

            return HkExtractionResult.builder()
                    .fileName(file.getName())
                    .rowsScanned(rowsScanned)
                    .rowsExtracted(rowsExtracted)
                    .rowsInserted(rowsInserted)
                    .rowsSkipped(rowsSkipped)
                    .datasetVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Fatal error during OpenCSV extraction of Hong Kong file {}", filePath, e);
            throw new RuntimeException("Failed extracting Hong Kong CSV file: " + e.getMessage(), e);
        }
    }
}
