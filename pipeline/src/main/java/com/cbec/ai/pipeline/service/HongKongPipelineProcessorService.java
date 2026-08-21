package com.cbec.ai.pipeline.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class HongKongPipelineProcessorService {

    private final HongKongCsvExtractorService extractorService;
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

    public HongKongPipelineProcessorService(
            HongKongCsvExtractorService extractorService,
            HsRawRepository hsRawRepository,
            HsValidatedRepository hsValidatedRepository,
            HsMasterRepository hsMasterRepository,
            HsVersionRepository hsVersionRepository,
            RejectedRecordRepository rejectedRecordRepository,
            CategoryChapterRepository categoryChapterRepository,
            PipelineExecutionRepository pipelineExecutionRepository) {
        this.extractorService = extractorService;
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
    public static class HkEtlExecutionReport {
        private String fileName;
        private long rowsScanned;
        private long rowsExtracted;
        private long rowsRejected;
        private long rowsInsertedHsRaw;
        private long rowsInsertedHsValidated;
        private long rowsInsertedHsMaster;
        private long rowsInsertedHsVersions;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Complete Hong Kong HKHS CSV ETL Pipeline (B2XX00232026XXXXB0100 (1).csv)
     */
    public HkEtlExecutionReport processHongKongPipeline(String csvPath, String version) {
        long startTime = System.currentTimeMillis();
        String ver = version != null ? version : "HKHS_2026";
        String filePath = csvPath != null ? csvPath : "B2XX00232026XXXXB0100 (1).csv";

        log.info("Starting Hong Kong Harmonized System CSV ETL Pipeline...");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("Hong Kong Harmonized System CSV ETL Pipeline")
                .country("Hong Kong")
                .source("Hong Kong Harmonized System CSV (B2XX00232026XXXXB0100 (1).csv)")
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

        // 1. OpenCSV Extraction into hs_raw
        HongKongCsvExtractorService.HkExtractionResult extractResult =
                extractorService.extractHongKongCsvData(filePath, executionId, ver);

        long totalRawRead = extractResult.getRowsInserted();

        // 2. Read raw records for Hong Kong
        List<HsRawEntity> hkRawRecords = hsRawRepository.findByCountry("Hong Kong");

        // 3. Stage 2: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : hkRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code matches exactly 8 digits
            if (code == null || !code.matches("^\\d{8}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_HKHS_CODE: Code must be 8 digits")
                        .pipelineStage("Validation Stage 2")
                        .country("Hong Kong")
                        .source("Hong Kong Harmonized System CSV")
                        .build());
                totalRejected++;
                continue;
            }

            // Validate description not empty
            if (desc == null || desc.trim().isEmpty()) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("MISSING_DESCRIPTION: Empty description")
                        .pipelineStage("Validation Stage 2")
                        .country("Hong Kong")
                        .source("Hong Kong Harmonized System CSV")
                        .build());
                totalRejected++;
                continue;
            }

            // Deduplication
            if (seenCodes.contains(code)) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("DUPLICATE_CODE_SKIPPED")
                        .pipelineStage("Validation Stage 2")
                        .country("Hong Kong")
                        .source("Hong Kong Harmonized System CSV")
                        .build());
                totalRejected++;
                continue;
            }
            seenCodes.add(code);

            String chapter = code.substring(0, 2);
            String heading = code.substring(0, 4);
            String hs6 = code.substring(0, 6);

            HsValidatedEntity validated = HsValidatedEntity.builder()
                    .executionId(executionId)
                    .customsTerritory("HK")
                    .country("Hong Kong")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(8)
                    .nomenclatureType("NATIONAL_TARIFF")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("Hong Kong Harmonized System CSV")
                    .datasetVersion(raw.getDatasetVersion() != null ? raw.getDatasetVersion() : ver)
                    .build();

            validatedToSave.add(validated);

            if (validatedToSave.size() >= 500) {
                hsValidatedRepository.saveAll(validatedToSave);
                totalValidatedInserted += validatedToSave.size();
                validatedToSave.clear();
            }
        }

        if (!validatedToSave.isEmpty()) {
            hsValidatedRepository.saveAll(validatedToSave);
            totalValidatedInserted += validatedToSave.size();
            validatedToSave.clear();
        }

        if (!rejectionsToSave.isEmpty()) {
            rejectedRecordRepository.saveAll(rejectionsToSave);
        }

        // 4. Stage 3: Category Mapping via category_chapter & category_master
        List<HsValidatedEntity> hkValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : hkValidated) {
            if ("Hong Kong".equalsIgnoreCase(val.getCountry()) && "HK".equalsIgnoreCase(val.getCustomsTerritory())) {
                String catName = resolveCategoryFromRelationalTable(val.getChapter());
                if (catName != null) {
                    val.setCategory(catName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(hkValidated);

        // 5. Stage 4 & 5: Master Loader & Versioning (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        long totalVersionsInserted = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : hkValidated) {
            if ("Hong Kong".equalsIgnoreCase(val.getCountry()) && "HK".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("HK")
                        .country("Hong Kong")
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(8)
                        .nomenclatureType("NATIONAL_TARIFF")
                        .category(val.getCategory())
                        .officialDescription(val.getOfficialDescription())
                        .unit(val.getUnit())
                        .datasetVersion(val.getDatasetVersion() != null ? val.getDatasetVersion() : ver)
                        .isCurrent(true)
                        .isActive(true)
                        .recordHash(recordHash)
                        .effectiveFrom(now)
                        .lastVerified(now)
                        .build();

                masterBatch.add(master);

                if (masterBatch.size() >= 500) {
                    List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterBatch);
                    totalMasterLoaded += savedMasters.size();

                    for (HsMasterEntity m : savedMasters) {
                        versionBatch.add(HsVersionEntity.builder()
                                .hsMasterId(m.getId())
                                .customsTerritory("HK")
                                .country("Hong Kong")
                                .nationalCode(m.getNationalCode())
                                .officialDescription(m.getOfficialDescription())
                                .category(m.getCategory())
                                .version(m.getDatasetVersion())
                                .effectiveFrom(now)
                                .lastVerified(now)
                                .build());
                    }

                    hsVersionRepository.saveAll(versionBatch);
                    totalVersionsInserted += versionBatch.size();
                    masterBatch.clear();
                    versionBatch.clear();
                }
            }
        }

        if (!masterBatch.isEmpty()) {
            List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterBatch);
            totalMasterLoaded += savedMasters.size();

            for (HsMasterEntity m : savedMasters) {
                versionBatch.add(HsVersionEntity.builder()
                        .hsMasterId(m.getId())
                        .customsTerritory("HK")
                        .country("Hong Kong")
                        .nationalCode(m.getNationalCode())
                        .officialDescription(m.getOfficialDescription())
                        .category(m.getCategory())
                        .version(m.getDatasetVersion())
                        .effectiveFrom(now)
                        .lastVerified(now)
                        .build());
            }

            hsVersionRepository.saveAll(versionBatch);
            totalVersionsInserted += versionBatch.size();
            masterBatch.clear();
            versionBatch.clear();
        }

        long duration = System.currentTimeMillis() - startTime;

        execution.setCompletedAt(LocalDateTime.now());
        execution.setExecutionTimeMs(duration);
        execution.setRecordsFound(totalRawRead);
        execution.setRecordsInserted(totalMasterLoaded);
        execution.setInvalidRecords(totalRejected);
        execution.setStatus("COMPLETED");
        pipelineExecutionRepository.save(execution);

        log.info("================================================================================");
        log.info("COMPLETED HONG KONG HARMONIZED SYSTEM CSV ETL PIPELINE");
        log.info("File: {}, Scanned: {}, Extracted: {}, Validated: {}, Rejections: {}, Master Loaded: {}, Time: {} ms",
                extractResult.getFileName(), extractResult.getRowsScanned(),
                totalRawRead, totalValidatedInserted, totalRejected, totalMasterLoaded, duration);
        log.info("================================================================================");

        return HkEtlExecutionReport.builder()
                .fileName(extractResult.getFileName())
                .rowsScanned(extractResult.getRowsScanned())
                .rowsExtracted(totalRawRead)
                .rowsRejected(totalRejected)
                .rowsInsertedHsRaw(totalRawRead)
                .rowsInsertedHsValidated(totalValidatedInserted)
                .rowsInsertedHsMaster(totalMasterLoaded)
                .rowsInsertedHsVersions(totalVersionsInserted)
                .executionTimeMs(duration)
                .status("COMPLETED")
                .executionId(executionId)
                .build();
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
        return null;
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
