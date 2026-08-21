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
public class SouthKoreaPipelineProcessorService {

    private final SouthKoreaExcelExtractorService extractorService;
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

    public SouthKoreaPipelineProcessorService(
            SouthKoreaExcelExtractorService extractorService,
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
    public static class SouthKoreaEtlExecutionReport {
        private String country;
        private String classificationFileName;
        private String tariffFileName;
        private long classificationRowsRead;
        private long tariffRowsRead;
        private long mergedRowsExtracted;
        private long recordsInsertedHsRaw;
        private long recordsValidated;
        private long recordsRejected;
        private long recordsCategoryMapped;
        private long recordsLoadedHsMaster;
        private long recordsLoadedHsVersions;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Complete South Korea Customs Tariff Excel ETL Pipeline
     */
    public SouthKoreaEtlExecutionReport processSouthKoreaPipeline(
            String classificationPath, String tariffPath, String version) {
        long startTime = System.currentTimeMillis();
        String ver = version != null ? version : "HSK_2026";
        String classFile = classificationPath != null ? classificationPath : "관세청_HS부호_20260101.xlsx";
        String tariffFile = tariffPath != null ? tariffPath : "관세청_품목번호별 관세율표_20260211.xlsx";

        log.info("Starting South Korea Customs Tariff Excel ETL Pipeline...");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("South Korea Customs Tariff Excel ETL Pipeline")
                .country("South Korea")
                .source("Korea Customs Service Official Excel")
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

        // 1. Stage 1: Excel Streaming & In-Memory Join -> hs_raw
        SouthKoreaExcelExtractorService.KoreaExcelExtractionResult extractResult =
                extractorService.extractSouthKoreaTariffData(classFile, tariffFile, executionId, ver);

        long totalRawRead = extractResult.getTotalInsertedHsRaw();

        // 2. Read raw records for South Korea
        List<HsRawEntity> krRawRecords = hsRawRepository.findByCountry("South Korea");

        // 3. Stage 6: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : krRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code is exactly 10 digits
            if (code == null || !code.matches("^\\d{10}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_HSK_CODE: Code must be exactly 10 digits")
                        .pipelineStage("Validation Stage 6")
                        .country("South Korea")
                        .source("Korea Customs Service Official Excel")
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
                        .pipelineStage("Validation Stage 6")
                        .country("South Korea")
                        .source("Korea Customs Service Official Excel")
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
                        .pipelineStage("Validation Stage 6")
                        .country("South Korea")
                        .source("Korea Customs Service Official Excel")
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
                    .customsTerritory("KR")
                    .country("South Korea")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(code.length())
                    .nomenclatureType("HSK")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("Korea Customs Service Official Excel")
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

        // 4. Stage 7: Category Mapping via category_chapter & category_master
        List<HsValidatedEntity> krValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : krValidated) {
            if ("South Korea".equalsIgnoreCase(val.getCountry()) && "KR".equalsIgnoreCase(val.getCustomsTerritory())) {
                String catName = resolveCategoryFromRelationalTable(val.getChapter());
                if (catName != null) {
                    val.setCategory(catName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(krValidated);

        // 5. Stage 8: Master Loader & Versioning (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        long totalVersionsInserted = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : krValidated) {
            if ("South Korea".equalsIgnoreCase(val.getCountry()) && "KR".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("KR")
                        .country("South Korea")
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(val.getCodeLength())
                        .nomenclatureType("HSK")
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
                                .customsTerritory("KR")
                                .country("South Korea")
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
                        .customsTerritory("KR")
                        .country("South Korea")
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
        log.info("COMPLETED SOUTH KOREA CUSTOMS TARIFF EXCEL ETL PIPELINE");
        log.info("Classification Read: {}, Tariff Read: {}, Merged: {}, Extracted: {}, Validated: {}, Master Loaded: {}, Time: {} ms",
                extractResult.getClassificationRowsRead(), extractResult.getTariffRowsRead(), extractResult.getMergedRowsExtracted(),
                totalRawRead, totalValidatedInserted, totalMasterLoaded, duration);
        log.info("================================================================================");

        return SouthKoreaEtlExecutionReport.builder()
                .country("South Korea")
                .classificationFileName(extractResult.getClassificationFileName())
                .tariffFileName(extractResult.getTariffFileName())
                .classificationRowsRead(extractResult.getClassificationRowsRead())
                .tariffRowsRead(extractResult.getTariffRowsRead())
                .mergedRowsExtracted(extractResult.getMergedRowsExtracted())
                .recordsInsertedHsRaw(totalRawRead)
                .recordsValidated(totalValidatedInserted)
                .recordsRejected(totalRejected)
                .recordsCategoryMapped(totalCategoryMapped)
                .recordsLoadedHsMaster(totalMasterLoaded)
                .recordsLoadedHsVersions(totalVersionsInserted)
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
