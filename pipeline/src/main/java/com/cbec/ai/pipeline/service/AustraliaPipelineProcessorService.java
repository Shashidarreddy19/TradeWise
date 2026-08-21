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
public class AustraliaPipelineProcessorService {

    private final AustraliaScheduleCrawlerService scheduleCrawlerService;
    private final AustraliaChapterCrawlerService chapterCrawlerService;
    private final AustraliaHtmlExtractorService htmlExtractorService;
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

    public AustraliaPipelineProcessorService(
            AustraliaScheduleCrawlerService scheduleCrawlerService,
            AustraliaChapterCrawlerService chapterCrawlerService,
            AustraliaHtmlExtractorService htmlExtractorService,
            HsRawRepository hsRawRepository,
            HsValidatedRepository hsValidatedRepository,
            HsMasterRepository hsMasterRepository,
            HsVersionRepository hsVersionRepository,
            RejectedRecordRepository rejectedRecordRepository,
            CategoryChapterRepository categoryChapterRepository,
            PipelineExecutionRepository pipelineExecutionRepository) {
        this.scheduleCrawlerService = scheduleCrawlerService;
        this.chapterCrawlerService = chapterCrawlerService;
        this.htmlExtractorService = htmlExtractorService;
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
    public static class AustraliaEtlExecutionReport {
        private String country;
        private int sectionsDiscovered;
        private int chaptersDiscovered;
        private int pagesDownloaded;
        private long recordsExtracted;
        private long recordsValidated;
        private long recordsLoaded;
        private long executionTimeMs;
        private String status;
        private Long executionId;
    }

    /**
     * Complete Australian Border Force Web Crawling ETL Pipeline
     */
    public AustraliaEtlExecutionReport processAustraliaPipeline(String version) {
        long startTime = System.currentTimeMillis();
        String ver = version != null ? version : "AU_TARIFF_2026";

        log.info("Starting Australian Border Force Web Crawling ETL Pipeline...");

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("Australia Customs Tariff Web Crawling ETL Pipeline")
                .country("Australia")
                .source("Australian Border Force Schedule 3")
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

        // 1. Stage 1: Schedule 3 Discovery
        AustraliaScheduleCrawlerService.ScheduleDiscoveryResult discovery = scheduleCrawlerService.discoverSchedule3Chapters();
        List<AustraliaScheduleCrawlerService.DiscoveredChapter> chapters = discovery.getChapters();

        int pagesDownloaded = 0;
        long totalRawExtracted = 0;

        // 2. Stage 2 & 3: Chapter Crawling & HTML Extraction into hs_raw
        for (AustraliaScheduleCrawlerService.DiscoveredChapter ch : chapters) {
            try {
                AustraliaChapterCrawlerService.ChapterCrawlResult crawlResult =
                        chapterCrawlerService.crawlChapterPage(ch, ver);

                if (crawlResult.isSuccess() && crawlResult.getDocument() != null) {
                    pagesDownloaded++;
                    AustraliaHtmlExtractorService.HtmlExtractionResult extractResult =
                            htmlExtractorService.extractTariffRecordsFromHtml(crawlResult.getDocument(), ch.getChapterNumber(), executionId, ver);

                    totalRawExtracted += extractResult.getRowsInserted();
                }
            } catch (Exception e) {
                log.warn("Failed processing Chapter {} ({}): {}", ch.getChapterNumber(), ch.getChapterUrl(), e.getMessage());
            }
        }

        // 3. Read raw records for Australia
        List<HsRawEntity> auRawRecords = hsRawRepository.findByCountry("Australia");
        long totalRawRead = auRawRecords.size();

        // 4. Stage 5: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : auRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            // Validate code is 8 to 10 digits
            if (code == null || !code.matches("^\\d{8,10}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_AU_TARIFF_CODE: Code must be 8 or 10 digits")
                        .pipelineStage("Validation Stage 5")
                        .country("Australia")
                        .source("Australian Border Force Schedule 3")
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
                        .pipelineStage("Validation Stage 5")
                        .country("Australia")
                        .source("Australian Border Force Schedule 3")
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
                        .pipelineStage("Validation Stage 5")
                        .country("Australia")
                        .source("Australian Border Force Schedule 3")
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
                    .customsTerritory("AU")
                    .country("Australia")
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(code.length())
                    .nomenclatureType("AU_TARIFF")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("Australian Border Force Schedule 3")
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

        // 5. Stage 6: Category Mapping via category_chapter & category_master
        List<HsValidatedEntity> auValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : auValidated) {
            if ("Australia".equalsIgnoreCase(val.getCountry()) && "AU".equalsIgnoreCase(val.getCustomsTerritory())) {
                String catName = resolveCategoryFromRelationalTable(val.getChapter());
                if (catName != null) {
                    val.setCategory(catName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(auValidated);

        // 6. Stage 7: Master Loader & Versioning (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : auValidated) {
            if ("Australia".equalsIgnoreCase(val.getCountry()) && "AU".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("AU")
                        .country("Australia")
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(val.getCodeLength())
                        .nomenclatureType("AU_TARIFF")
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
                                .customsTerritory("AU")
                                .country("Australia")
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
                        .customsTerritory("AU")
                        .country("Australia")
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
        log.info("COMPLETED AUSTRALIA CUSTOMS TARIFF WEB CRAWLING ETL PIPELINE");
        log.info("Sections: {}, Chapters: {}, Downloaded: {}, Extracted: {}, Validated: {}, Master Loaded: {}, Time: {} ms",
                discovery.getSectionsDiscovered(), discovery.getChaptersDiscovered(), pagesDownloaded,
                totalRawRead, totalValidatedInserted, totalMasterLoaded, duration);
        log.info("================================================================================");

        return AustraliaEtlExecutionReport.builder()
                .country("Australia")
                .sectionsDiscovered(discovery.getSectionsDiscovered())
                .chaptersDiscovered(discovery.getChaptersDiscovered())
                .pagesDownloaded(pagesDownloaded)
                .recordsExtracted(totalRawRead)
                .recordsValidated(totalValidatedInserted)
                .recordsLoaded(totalMasterLoaded)
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
