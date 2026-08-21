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
public class UkTradeTariffEtlProcessorService {

    private final TradeTariffDownloaderService downloaderService;
    private final TradeTariffExtractorService extractorService;
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

    public UkTradeTariffEtlProcessorService(
            TradeTariffDownloaderService downloaderService,
            TradeTariffExtractorService extractorService,
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
    public static class UkEtlSummary {
        private String country;
        private String sourceUrl;
        private long totalDownloadedBytes;
        private int totalHeadingsDownloaded;
        private int totalCommoditiesDownloaded;
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
     * Complete United Kingdom Trade Tariff ETL Pipeline (100% Live HMRC OAuth2 API Download -> Extract -> Validate -> Category Map -> Master Load)
     */
    public UkEtlSummary processUkPipeline(String version) {
        long startTime = System.currentTimeMillis();
        String ver = version != null ? version : "UK_TARIFF_2026";

        log.info("Starting live official UK Trade Tariff OAuth2 & ETL Pipeline from HMRC API...");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("United Kingdom Trade Tariff OAuth2 & ETL Pipeline")
                .country("United Kingdom")
                .source("HMRC UK Trade Tariff API v2")
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

        // 1. Live Download official HMRC payload
        TradeTariffDownloaderService.DownloadSummary downloadSummary =
                downloaderService.downloadOfficialUkTariffPayload(ver);

        // 2. Extract into hs_raw
        TradeTariffExtractorService.UkExtractionSummary extractSummary =
                extractorService.extractUkTariffData(downloadSummary.getFilePath(), executionId, ver);

        long totalRawRead = extractSummary.getTotalExtracted();

        // 3. Read raw records for United Kingdom
        List<HsRawEntity> ukRawRecords = hsRawRepository.findByCountry("United Kingdom");

        // 4. Phase 1: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : ukRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code is 10 digits
            if (code == null || !code.matches("^\\d{10}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_UK_TARIFF_CODE: Code must be 10 digits")
                        .pipelineStage("Validation Stage 4")
                        .country("United Kingdom")
                        .source("HMRC UK Trade Tariff API v2")
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
                        .country("United Kingdom")
                        .source("HMRC UK Trade Tariff API v2")
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
                        .pipelineStage("Validation Stage 4")
                        .country("United Kingdom")
                        .source("HMRC UK Trade Tariff API v2")
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
                    .customsTerritory("GB")
                    .country("United Kingdom")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(10)
                    .nomenclatureType("UK_TARIFF")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("HMRC UK Trade Tariff API v2")
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

        // 5. Phase 2: Category Mapping
        List<HsValidatedEntity> ukValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : ukValidated) {
            if ("United Kingdom".equalsIgnoreCase(val.getCountry()) || "GB".equalsIgnoreCase(val.getCustomsTerritory())) {
                String catName = resolveCategoryFromRelationalTable(val.getChapter());
                if (catName != null) {
                    val.setCategory(catName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(ukValidated);

        // 6. Phase 3: Master Loader (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : ukValidated) {
            if ("United Kingdom".equalsIgnoreCase(val.getCountry()) && "GB".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("GB")
                        .country("United Kingdom")
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(10)
                        .nomenclatureType("UK_TARIFF")
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
                                .customsTerritory("GB")
                                .country("United Kingdom")
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
                        .customsTerritory("GB")
                        .country("United Kingdom")
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
        log.info("COMPLETED LIVE UK TRADE TARIFF OAUTH2 & ETL PIPELINE");
        log.info("Downloaded Bytes: {}, Headings: {}, Commodities: {}, Inserted hs_raw: {}, Validated: {}, Mapped: {}, Master Loaded: {}, Time: {} ms",
                downloadSummary.getFileSize(), downloadSummary.getTotalHeadingsDownloaded(), downloadSummary.getTotalCommoditiesDownloaded(),
                totalRawRead, totalValidatedInserted, totalCategoryMapped, totalMasterLoaded, duration);
        log.info("================================================================================");

        return UkEtlSummary.builder()
                .country("United Kingdom")
                .sourceUrl(downloadSummary.getFilePath())
                .totalDownloadedBytes(downloadSummary.getFileSize())
                .totalHeadingsDownloaded(downloadSummary.getTotalHeadingsDownloaded())
                .totalCommoditiesDownloaded(downloadSummary.getTotalCommoditiesDownloaded())
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
