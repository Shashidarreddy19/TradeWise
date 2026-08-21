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
public class IndiaEtlPipelineProcessorService {

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

    public IndiaEtlPipelineProcessorService(
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
    public static class EtlExecutionSummary {
        private long totalRawRead;
        private long totalValidatedInserted;
        private long totalRejected;
        private long totalCategoryMapped;
        private long totalMasterLoaded;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Executes the complete India ETL Pipeline from `hs_raw` -> `hs_validated` -> `hs_master`.
     */
    public EtlExecutionSummary processIndiaPipeline() {
        long startTime = System.currentTimeMillis();
        log.info("Starting India ETL Pipeline: hs_raw -> hs_validated -> hs_master");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("India DGFT ITC(HS) 2022 ETL Processing")
                .country("India")
                .source("hs_raw Audit Layer")
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

        // 1. Read all raw records from hs_raw for ITC_HS_2022
        List<HsRawEntity> rawRecords = hsRawRepository.findByCountry("India");
        if (rawRecords.isEmpty()) {
            rawRecords = hsRawRepository.findAll();
        }

        long totalRawRead = rawRecords.size();
        log.info("Phase 1: Read {} audit records from hs_raw", totalRawRead);

        // 2. Phase 1: Stage 4 Validation -> populate hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenNationalCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : rawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code matches 8 digits
            if (code == null || !code.matches("^\\d{8}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_NATIONAL_CODE: Code must be 8 digits")
                        .pipelineStage("Validation Stage 4")
                        .country("India")
                        .source("DGFT ITC(HS)")
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
                        .pipelineStage("Validation Stage 4")
                        .country("India")
                        .source("DGFT ITC(HS)")
                        .build());
                totalRejected++;
                continue;
            }

            // Deduplication per batch
            if (seenNationalCodes.contains(code)) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("DUPLICATE_CODE: Skipped duplicate in batch")
                        .pipelineStage("Validation Stage 4")
                        .country("India")
                        .source("DGFT ITC(HS)")
                        .build());
                totalRejected++;
                continue;
            }
            seenNationalCodes.add(code);

            String chapter = code.substring(0, 2);
            String heading = code.substring(0, 4);
            String hs6 = code.substring(0, 6);

            HsValidatedEntity validated = HsValidatedEntity.builder()
                    .executionId(executionId)
                    .customsTerritory("IN")
                    .country("India")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(8)
                    .nomenclatureType("ITC_HS")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("DGFT ITC(HS)")
                    .datasetVersion(raw.getDatasetVersion() != null ? raw.getDatasetVersion() : "ITC_HS_2022")
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

        log.info("Phase 1 Complete: Populated hs_validated with {} records. Rejections: {}", totalValidatedInserted, totalRejected);

        // 3. Phase 2: Stage 5 Category Mapping
        List<HsValidatedEntity> allValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : allValidated) {
            String categoryName = resolveCategoryFromRelationalTable(val.getChapter());
            if (categoryName != null) {
                val.setCategory(categoryName);
                totalCategoryMapped++;
            } else {
                val.setCategory("UNSUPPORTED");
            }
        }
        hsValidatedRepository.saveAll(allValidated);
        log.info("Phase 2 Complete: Mapped {} records into target categories", totalCategoryMapped);

        // 4. Phase 3: Stage 6 Master Loader (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;

        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : allValidated) {
            String ch = val.getChapter();
            // Filter ONLY the 21 supported export chapters
            if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                continue;
            }

            String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

            HsMasterEntity master = HsMasterEntity.builder()
                    .customsTerritory("IN")
                    .country("India")
                    .chapter(val.getChapter())
                    .heading(val.getHeading())
                    .hs6(val.getHs6())
                    .nationalCode(val.getNationalCode())
                    .codeLength(8)
                    .nomenclatureType("ITC_HS")
                    .category(val.getCategory())
                    .officialDescription(val.getOfficialDescription())
                    .unit(val.getUnit())
                    .datasetVersion(val.getDatasetVersion() != null ? val.getDatasetVersion() : "ITC_HS_2022")
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
                            .customsTerritory("IN")
                            .country("India")
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

        if (!masterBatch.isEmpty()) {
            List<HsMasterEntity> savedMasters = hsMasterRepository.saveAll(masterBatch);
            totalMasterLoaded += savedMasters.size();

            for (HsMasterEntity m : savedMasters) {
                versionBatch.add(HsVersionEntity.builder()
                        .hsMasterId(m.getId())
                        .customsTerritory("IN")
                        .country("India")
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
        execution.setRecordsFound(totalRawRead);
        execution.setRecordsInserted(totalMasterLoaded);
        execution.setInvalidRecords(totalRejected);
        execution.setStatus("COMPLETED");
        pipelineExecutionRepository.save(execution);

        log.info("================================================================================");
        log.info("COMPLETED INDIA ETL PIPELINE: hs_raw -> hs_validated -> hs_master");
        log.info("Raw Read: {}, Validated: {}, Rejections: {}, Mapped: {}, Master Loaded: {}, Time: {} ms",
                totalRawRead, totalValidatedInserted, totalRejected, totalCategoryMapped, totalMasterLoaded, duration);
        log.info("================================================================================");

        return EtlExecutionSummary.builder()
                .totalRawRead(totalRawRead)
                .totalValidatedInserted(totalValidatedInserted)
                .totalRejected(totalRejected)
                .totalCategoryMapped(totalCategoryMapped)
                .totalMasterLoaded(totalMasterLoaded)
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
