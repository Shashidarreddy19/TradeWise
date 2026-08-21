package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class USHTSCsvPipelineProcessorService {

    private final HsRawRepository hsRawRepository;
    private final HsValidatedRepository hsValidatedRepository;
    private final HsMasterRepository hsMasterRepository;
    private final HsVersionRepository hsVersionRepository;
    private final RejectedRecordRepository rejectedRecordRepository;
    private final CategoryChapterRepository categoryChapterRepository;
    private final PipelineExecutionRepository pipelineExecutionRepository;

    // 21 Supported Export Chapters
    private static final Set<String> SUPPORTED_CHAPTERS = new HashSet<>(Arrays.asList(
            "03", "09", "10", "27", "29", "30", "33", "34", "41", "42",
            "57", "58", "61", "62", "63", "71", "72", "73", "84", "85", "87"
    ));

    public USHTSCsvPipelineProcessorService(
            HsRawRepository hsRawRepository,
            HsValidatedRepository hsValidatedRepository,
            HsMasterRepository hsMasterRepository,
            HsVersionRepository hsVersionRepository,
            RejectedRecordRepository rejectedRecordRepository,
            CategoryChapterRepository categoryChapterRepository,
            PipelineExecutionRepository pipelineExecutionRepository) {
        this.hsRawRepository = hsRawRepository;
        this.hsValidatedRepository = hsValidatedRepository;
        this.hsMasterRepository = hsMasterRepository;
        this.hsVersionRepository = hsVersionRepository;
        this.rejectedRecordRepository = rejectedRecordRepository;
        this.categoryChapterRepository = categoryChapterRepository;
        this.pipelineExecutionRepository = pipelineExecutionRepository;
    }

    @Data
    @Builder
    public static class UsCsvEtlSummary {
        private String csvFilePath;
        private long totalRowsRead;
        private long headingRowsIgnored;
        private long groupRowsProcessed;
        private long intermediateRowsProcessed;
        private long finalTariffRecordsExtracted;
        private long rawInserted;
        private long validatedInserted;
        private long rejections;
        private long masterLoaded;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Complete streaming CSV ETL pipeline for USITC HTS 2026 Basic Edition.
     */
    public UsCsvEtlSummary processUsHtsCsv(String csvPath, String datasetVersion) {
        long startTime = System.currentTimeMillis();
        String version = datasetVersion != null ? datasetVersion : "HTS_2026";
        String filePath = csvPath != null ? csvPath : "hts_2026_basic_edition_csv.csv";

        log.info("Starting streaming USITC HTS CSV ETL processing for file: {}", filePath);

        File csvFile = new File(filePath);
        if (!csvFile.exists()) {
            log.error("US HTS CSV file not found at path: {}", filePath);
            throw new IllegalArgumentException("US HTS CSV file not found at path: " + filePath);
        }

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("United States USITC HTS 2026 CSV ETL Processing")
                .country("United States")
                .source("USITC HTS 2026 Basic Edition CSV")
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .recordsFound(0L)
                .recordsInserted(0L)
                .recordsUpdated(0L)
                .duplicates(0L)
                .invalidRecords(0L)
                .build();
        execution = pipelineExecutionRepository.save(execution);
        Long executionId = execution.getId();

        long totalRowsRead = 0;
        long headingRowsIgnored = 0;
        long groupRowsProcessed = 0;
        long intermediateRowsProcessed = 0;
        long finalTariffRecordsExtracted = 0;
        long rawInserted = 0;

        Map<Integer, String> descriptionStack = new HashMap<>();
        List<HsRawEntity> rawBatch = new ArrayList<>();

        // Phase 1: Streaming Extraction into hs_raw
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile, StandardCharsets.UTF_8)).build()) {
            String[] line;
            boolean isHeader = true;

            while ((line = reader.readNext()) != null) {
                totalRowsRead++;
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header row
                }

                if (line.length < 3) continue;

                String rawHts = line[0] != null ? line[0].trim() : "";
                String rawIndent = line[1] != null ? line[1].trim() : "0";
                String rawDesc = line[2] != null ? line[2].trim() : "";
                String rawUnit = line.length > 3 && line[3] != null ? line[3].trim() : "";

                if (rawDesc.isEmpty()) continue;

                int indentLevel = 0;
                try {
                    indentLevel = Integer.parseInt(rawIndent);
                } catch (NumberFormatException ignored) {}

                final int currentIndent = indentLevel;
                // Clean description
                String cleanDesc = rawDesc.replaceAll(":\\s*$", "").trim();

                // Update description stack for hierarchy construction
                descriptionStack.put(currentIndent, cleanDesc);
                // Clear any deeper levels from previous branches
                descriptionStack.keySet().removeIf(k -> k > currentIndent);

                String cleanCode = rawHts.replaceAll("[^0-9]", "").trim();

                // Check Row Types
                if (cleanCode.length() == 4) {
                    headingRowsIgnored++;
                    continue;
                }

                if (cleanCode.isEmpty()) {
                    groupRowsProcessed++;
                    continue;
                }

                // If code length is 6 or 8 digits, pad with "00" to 8 or 10 digits
                if (cleanCode.length() == 6) {
                    cleanCode = cleanCode + "0000";
                } else if (cleanCode.length() == 8) {
                    cleanCode = cleanCode + "00";
                }

                if (cleanCode.length() < 6) {
                    intermediateRowsProcessed++;
                    continue;
                }

                // Final 10-digit National Tariff Record!
                finalTariffRecordsExtracted++;

                // Build inherited description
                String fullHierarchicalDesc = buildInheritedDescription(descriptionStack, indentLevel);
                String cleanUnitStr = cleanUnit(rawUnit);

                HsRawEntity rawEntity = HsRawEntity.builder()
                        .executionId(executionId)
                        .customsTerritory("US")
                        .country("United States")
                        .rawNationalCode(cleanCode)
                        .rawDescription(fullHierarchicalDesc)
                        .unit(cleanUnitStr)
                        .sourceName("USITC HTS 2026")
                        .datasetVersion(version)
                        .build();

                rawBatch.add(rawEntity);

                if (rawBatch.size() >= 500) {
                    hsRawRepository.saveAll(rawBatch);
                    rawInserted += rawBatch.size();
                    rawBatch.clear();
                }
            }

            if (!rawBatch.isEmpty()) {
                hsRawRepository.saveAll(rawBatch);
                rawInserted += rawBatch.size();
                rawBatch.clear();
            }

        } catch (Exception e) {
            log.error("Error reading US HTS CSV file", e);
            throw new RuntimeException("Failed reading US HTS CSV: " + e.getMessage(), e);
        }

        log.info("Phase 1 Complete. Read: {}, Headings: {}, Groups: {}, Intermediates: {}, Final Tariff Extracted: {}, Raw Inserted: {}",
                totalRowsRead, headingRowsIgnored, groupRowsProcessed, intermediateRowsProcessed, finalTariffRecordsExtracted, rawInserted);

        // Build chapter->category lookup map ONCE (eager fetch avoids lazy proxy issues)
        Map<String, String> chapterCategoryMap = new HashMap<>();
        categoryChapterRepository.findAll().forEach(cc -> {
            try {
                String catName = cc.getCategory() != null ? cc.getCategory().getCategoryName() : null;
                if (catName != null && cc.getChapter() != null) {
                    chapterCategoryMap.put(cc.getChapter(), catName);
                    // also map without leading zero
                    if (cc.getChapter().startsWith("0") && cc.getChapter().length() == 2) {
                        chapterCategoryMap.putIfAbsent(cc.getChapter().substring(1), catName);
                    }
                }
            } catch (Exception ignored) {}
        });
        log.info("Built chapter-category map with {} entries", chapterCategoryMap.size());

        // Phase 2: Validation Engine -> hs_validated & rejected_records
        List<HsRawEntity> usRawRecords = hsRawRepository.findByCountry("United States");
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long validatedInserted = 0;
        long rejections = 0;

        for (HsRawEntity raw : usRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            if (code == null || !code.matches("^\\d{8,10}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("INVALID_CODE_LENGTH: Code must be 8 to 10 digits")
                        .pipelineStage("Validation Stage 4")
                        .country("United States")
                        .source("USITC HTS 2026")
                        .build());
                rejections++;
                continue;
            }

            if (desc == null || desc.trim().isEmpty()) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("EMPTY_DESCRIPTION")
                        .pipelineStage("Validation Stage 4")
                        .country("United States")
                        .source("USITC HTS 2026")
                        .build());
                rejections++;
                continue;
            }

            if (seenCodes.contains(code)) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("DUPLICATE_CODE_SKIPPED")
                        .pipelineStage("Validation Stage 4")
                        .country("United States")
                        .source("USITC HTS 2026")
                        .build());
                rejections++;
                continue;
            }
            seenCodes.add(code);

            String chapter = code.substring(0, 2);
            String heading = code.substring(0, 4);
            String hs6 = code.substring(0, 6);

            String resolvedCategory = chapterCategoryMap.getOrDefault(chapter,
                    chapterCategoryMap.getOrDefault(chapter.startsWith("0") ? chapter.substring(1) : "0" + chapter,
                    "General Tariff Line"));

            HsValidatedEntity validated = HsValidatedEntity.builder()
                    .executionId(executionId)
                    .customsTerritory("US")
                    .country("United States")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(code.length())
                    .nomenclatureType("HTS")
                    .category(resolvedCategory)
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("USITC HTS 2026")
                    .datasetVersion(raw.getDatasetVersion() != null ? raw.getDatasetVersion() : version)
                    .build();

            validatedToSave.add(validated);

            if (validatedToSave.size() >= 500) {
                hsValidatedRepository.saveAll(validatedToSave);
                validatedInserted += validatedToSave.size();
                validatedToSave.clear();
            }
        }

        if (!validatedToSave.isEmpty()) {
            hsValidatedRepository.saveAll(validatedToSave);
            validatedInserted += validatedToSave.size();
            validatedToSave.clear();
        }

        if (!rejectionsToSave.isEmpty()) {
            rejectedRecordRepository.saveAll(rejectionsToSave);
        }

        log.info("Phase 2 Complete. Validated: {}, Rejections: {}", validatedInserted, rejections);

        log.info("Phase 3 skipped - category resolved inline in Phase 2");

        // Phase 4: Master Loader - read validated records by country
        List<HsValidatedEntity> usValidated = hsValidatedRepository.findByCountry("United States");
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long masterLoaded = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : usValidated) {
            if ("United States".equalsIgnoreCase(val.getCountry()) || "US".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (ch == null || ch.trim().isEmpty()) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("US")
                        .country("United States")
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(val.getCodeLength())
                        .nomenclatureType("HTS")
                        .category(val.getCategory())
                        .officialDescription(val.getOfficialDescription())
                        .unit(val.getUnit())
                        .datasetVersion(val.getDatasetVersion() != null ? val.getDatasetVersion() : version)
                        .isCurrent(true)
                        .isActive(true)
                        .recordHash(recordHash)
                        .effectiveFrom(now)
                        .lastVerified(now)
                        .build();

                masterBatch.add(master);

                if (masterBatch.size() >= 500) {
                    List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterBatch);
                    masterLoaded += savedMasters.size();

                    for (HsMasterEntity m : savedMasters) {
                        versionBatch.add(HsVersionEntity.builder()
                                .hsMasterId(m.getId())
                                .customsTerritory("US")
                                .country("United States")
                                .nationalCode(m.getNationalCode())
                                .officialDescription(m.getOfficialDescription())
                                .category(m.getCategory())
                                .version(m.getDatasetVersion())
                                .effectiveFrom(now)
                                .lastVerified(now)
                                .build());
                    }

                    hsVersionRepository.saveAll(versionBatch);
                    masterBatch.clear();
                    versionBatch.clear();
                }
            }
        }

        if (!masterBatch.isEmpty()) {
            List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterBatch);
            masterLoaded += savedMasters.size();

            for (HsMasterEntity m : savedMasters) {
                versionBatch.add(HsVersionEntity.builder()
                        .hsMasterId(m.getId())
                        .customsTerritory("US")
                        .country("United States")
                        .nationalCode(m.getNationalCode())
                        .officialDescription(m.getOfficialDescription())
                        .category(m.getCategory())
                        .version(m.getDatasetVersion())
                        .effectiveFrom(now)
                        .lastVerified(now)
                        .build());
            }

            hsVersionRepository.saveAll(versionBatch);
            masterBatch.clear();
            versionBatch.clear();
        }

        long duration = System.currentTimeMillis() - startTime;

        execution.setCompletedAt(LocalDateTime.now());
        execution.setExecutionTimeMs(duration);
        execution.setRecordsFound(finalTariffRecordsExtracted);
        execution.setRecordsInserted(masterLoaded);
        execution.setInvalidRecords(rejections);
        execution.setStatus("COMPLETED");
        pipelineExecutionRepository.save(execution);

        log.info("================================================================================");
        log.info("COMPLETED USITC HTS 2026 CSV ETL PIPELINE");
        log.info("Rows Read: {}, Final Tariff Extracted: {}, Raw Inserted: {}, Validated: {}, Master Loaded: {}, Time: {} ms",
                totalRowsRead, finalTariffRecordsExtracted, rawInserted, validatedInserted, masterLoaded, duration);
        log.info("================================================================================");

        return UsCsvEtlSummary.builder()
                .csvFilePath(filePath)
                .totalRowsRead(totalRowsRead)
                .headingRowsIgnored(headingRowsIgnored)
                .groupRowsProcessed(groupRowsProcessed)
                .intermediateRowsProcessed(intermediateRowsProcessed)
                .finalTariffRecordsExtracted(finalTariffRecordsExtracted)
                .rawInserted(rawInserted)
                .validatedInserted(validatedInserted)
                .rejections(rejections)
                .masterLoaded(masterLoaded)
                .executionTimeMs(duration)
                .status("COMPLETED")
                .executionId(executionId)
                .build();
    }

    private String buildInheritedDescription(Map<Integer, String> stack, int currentIndent) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i <= currentIndent; i++) {
            if (stack.containsKey(i)) {
                String val = stack.get(i);
                if (val != null && !val.trim().isEmpty()) {
                    parts.add(val.trim());
                }
            }
        }
        if (parts.isEmpty()) return "";
        return String.join(" > ", parts);
    }

    private String cleanUnit(String rawUnit) {
        if (rawUnit == null || rawUnit.trim().isEmpty()) return null;
        String cleaned = rawUnit.replaceAll("[\\[\\]\"\\\\]", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String resolveCategoryFromRelationalTable(String chapter) {
        if (chapter == null || chapter.trim().isEmpty()) return null;
        String normalizedCh = chapter.trim();
        if (normalizedCh.length() == 1) normalizedCh = "0" + normalizedCh;

        List<CategoryChapterEntity> matches = categoryChapterRepository.findByChapter(normalizedCh);
        if (!matches.isEmpty()) {
            return matches.get(0).getCategory().getCategoryName();
        }

        if (normalizedCh.startsWith("0")) {
            matches = categoryChapterRepository.findByChapter(normalizedCh.substring(1));
            if (!matches.isEmpty()) {
                return matches.get(0).getCategory().getCategoryName();
            }
        }
        return "General Tariff Line";
    }

    private String computeRecordHash(String code, String desc, String unit) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = (code != null ? code : "") + "|" + (desc != null ? desc : "") + "|" + (unit != null ? unit : "");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return "";
        }
    }
}
