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
public class USHTSEtlProcessorService {

    private final USHTSDatasetDownloaderService downloaderService;
    private final USHTSExtractorService extractorService;
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

    public USHTSEtlProcessorService(
            USHTSDatasetDownloaderService downloaderService,
            USHTSExtractorService extractorService,
            HsRawRepository hsRawRepository,
            HsValidatedRepository hsValidatedRepository,
            HsMasterRepository hsMasterRepository,
            HsVersionRepository hsVersionRepository,
            RejectedRecordRepository rejectedRecordRepository,
            CategoryChapterRepository categoryChapterRepository,
            PipelineExecutionRepository pipelineExecutionRepository) {
        this.downloaderService = downloaderService;
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
    public static class UsEtlExecutionSummary {
        private String sourceUrl;
        private long totalDownloadedBytes;
        private long totalRawExtracted;
        private long totalValidatedInserted;
        private long totalRejected;
        private long totalCategoryMapped;
        private long totalMasterLoaded;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Executes complete United States HTS ETL Pipeline:
     * Download -> Extract to hs_raw -> Validate to hs_validated -> Category Map -> Master Load to hs_master & hs_versions
     */
    public UsEtlExecutionSummary processUsPipeline(String sourceUrl, String version) {
        long startTime = System.currentTimeMillis();
        log.info("Starting United States HTS ETL Pipeline");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("United States USITC HTS ETL Processing")
                .country("United States")
                .source("USITC HTS Official API")
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

        // 1. Download official dataset payload
        USHTSDatasetDownloaderService.DownloadResult downloadResult = downloaderService.downloadOfficialHtsDataset(sourceUrl, version);

        // 2. Extract into hs_raw
        USHTSExtractorService.USHTSExtractionResult extractionResult = extractorService.extractUsHtsData(
                downloadResult.getFilePath(), executionId, downloadResult.getDatasetVersion());

        long totalRawRead = extractionResult.getTotalExtracted();

        // 3. Read raw records for United States
        List<HsRawEntity> usRawRecords = hsRawRepository.findByCountry("United States");

        // 4. Phase 1: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenNationalCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : usRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code matches 10 digits
            if (code == null || !code.matches("^\\d{10}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_US_HTS_CODE: Code must be 10 digits")
                        .pipelineStage("Validation Stage 4")
                        .country("United States")
                        .source("US Harmonized Tariff Schedule (USITC)")
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
                        .country("United States")
                        .source("US Harmonized Tariff Schedule (USITC)")
                        .build());
                totalRejected++;
                continue;
            }

            // Deduplication
            if (seenNationalCodes.contains(code)) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("DUPLICATE_CODE: Skipped duplicate US code in batch")
                        .pipelineStage("Validation Stage 4")
                        .country("United States")
                        .source("US Harmonized Tariff Schedule (USITC)")
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
                    .customsTerritory("US")
                    .country("United States")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(10)
                    .nomenclatureType("HTS")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("US Harmonized Tariff Schedule (USITC)")
                    .datasetVersion(raw.getDatasetVersion() != null ? raw.getDatasetVersion() : "US_HTS_2026")
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

        // 5. Phase 2: Category Mapping
        List<HsValidatedEntity> usValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : usValidated) {
            if ("United States".equalsIgnoreCase(val.getCountry()) || "US".equalsIgnoreCase(val.getCustomsTerritory())) {
                String categoryName = resolveCategoryFromRelationalTable(val.getChapter());
                if (categoryName != null) {
                    val.setCategory(categoryName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(usValidated);

        // 6. Phase 3: Master Loader (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : usValidated) {
            if ("United States".equalsIgnoreCase(val.getCountry()) || "US".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
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
                        .codeLength(10)
                        .nomenclatureType("HTS")
                        .category(val.getCategory())
                        .officialDescription(val.getOfficialDescription())
                        .unit(val.getUnit())
                        .datasetVersion(val.getDatasetVersion() != null ? val.getDatasetVersion() : "US_HTS_2026")
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
            totalMasterLoaded += savedMasters.size();

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
        execution.setRecordsFound(totalRawRead);
        execution.setRecordsInserted(totalMasterLoaded);
        execution.setInvalidRecords(totalRejected);
        execution.setStatus("COMPLETED");
        pipelineExecutionRepository.save(execution);

        log.info("================================================================================");
        log.info("COMPLETED UNITED STATES HTS ETL PIPELINE");
        log.info("Raw Extracted: {}, Validated: {}, Rejections: {}, Mapped: {}, Master Loaded: {}, Time: {} ms",
                totalRawRead, totalValidatedInserted, totalRejected, totalCategoryMapped, totalMasterLoaded, duration);
        log.info("================================================================================");

        return UsEtlExecutionSummary.builder()
                .sourceUrl(downloadResult.getFilePath())
                .totalDownloadedBytes(downloadResult.getFileSize())
                .totalRawExtracted(totalRawRead)
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
