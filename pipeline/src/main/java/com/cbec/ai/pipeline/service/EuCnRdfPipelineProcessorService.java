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
public class EuCnRdfPipelineProcessorService {

    private final EuCnRdfExtractorService extractorService;
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

    public EuCnRdfPipelineProcessorService(
            EuCnRdfExtractorService extractorService,
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
    public static class EuCnRdfSummary {
        private String country;
        private String rdfFilePath;
        private long totalDescriptionsParsed;
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
     * Executes StAX Streaming EU CN 2026 RDF/XML ETL Pipeline for Germany, Netherlands, or any EU member state.
     */
    public EuCnRdfSummary processEuCnRdfPipeline(String country, String rdfPath, String version) {
        long startTime = System.currentTimeMillis();
        String targetCountry = country != null ? country : "Germany";
        String ver = version != null ? version : "CN_2026";
        String filePath = rdfPath != null ? rdfPath : "ESTAT-CN2026.rdf";

        log.info("Starting StAX streaming EU CN 2026 RDF/XML ETL Pipeline for country: {}", targetCountry);

        PipelineExecutionEntity execution = PipelineExecutionEntity.builder()
                .pipelineName("EU Combined Nomenclature CN 2026 RDF/XML Processing (" + targetCountry + ")")
                .country(targetCountry)
                .source("EU Combined Nomenclature ESTAT-CN2026.rdf")
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

        // 1. StAX Streaming Extraction into hs_raw
        EuCnRdfExtractorService.RdfExtractionSummary extractResult =
                extractorService.extractRdfCnData(targetCountry, filePath, executionId, ver);

        long totalRawRead = extractResult.getTotalConceptsExtracted();

        // 2. Read extracted raw records for target country
        List<HsRawEntity> euRawRecords = hsRawRepository.findByCountry(targetCountry);

        // 3. Phase 1: Validation -> hs_validated & rejected_records
        List<HsValidatedEntity> validatedToSave = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        long totalValidatedInserted = 0;
        long totalRejected = 0;

        for (HsRawEntity raw : euRawRecords) {
            String code = raw.getRawNationalCode();
            String desc = raw.getRawDescription();

            if (code == null || !code.matches("^\\d{8}$")) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code + " | Desc: " + desc)
                        .reason("INVALID_CN_CODE: Code must be 8 digits")
                        .pipelineStage("Validation Stage 4")
                        .country(targetCountry)
                        .source("EU Combined Nomenclature")
                        .build());
                totalRejected++;
                continue;
            }

            if (desc == null || desc.trim().isEmpty()) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("MISSING_DESCRIPTION: Empty description")
                        .pipelineStage("Validation Stage 4")
                        .country(targetCountry)
                        .source("EU Combined Nomenclature")
                        .build());
                totalRejected++;
                continue;
            }

            if (seenCodes.contains(code)) {
                rejectionsToSave.add(RejectedRecordEntity.builder()
                        .executionId(executionId)
                        .rawRecord("Code: " + code)
                        .reason("DUPLICATE_CODE_SKIPPED")
                        .pipelineStage("Validation Stage 4")
                        .country(targetCountry)
                        .source("EU Combined Nomenclature")
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
                    .customsTerritory("EU")
                    .country(targetCountry)
                    .chapter(chapter)
                    .heading(heading)
                    .hs6(hs6)
                    .nationalCode(code)
                    .codeLength(8)
                    .nomenclatureType("CN")
                    .category("PENDING")
                    .officialDescription(desc.trim())
                    .unit(raw.getUnit())
                    .sourceName("EU Combined Nomenclature")
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

        log.info("Phase 1 Complete for {}. Validated: {}, Rejections: {}", targetCountry, totalValidatedInserted, totalRejected);

        // 4. Phase 2: Category Mapping
        List<HsValidatedEntity> euValidated = hsValidatedRepository.findAll();
        long totalCategoryMapped = 0;

        for (HsValidatedEntity val : euValidated) {
            if (targetCountry.equalsIgnoreCase(val.getCountry()) && "EU".equalsIgnoreCase(val.getCustomsTerritory())) {
                String catName = resolveCategoryFromRelationalTable(val.getChapter());
                if (catName != null) {
                    val.setCategory(catName);
                    totalCategoryMapped++;
                } else {
                    val.setCategory("UNSUPPORTED");
                }
            }
        }
        hsValidatedRepository.saveAll(euValidated);

        log.info("Phase 2 Complete for {}. Category Mapped: {}", targetCountry, totalCategoryMapped);

        // 5. Phase 3: Master Loader (Filtered by 21 Supported Export Chapters)
        List<HsMasterEntity> masterBatch = new ArrayList<>();
        List<HsVersionEntity> versionBatch = new ArrayList<>();
        long totalMasterLoaded = 0;
        LocalDateTime now = LocalDateTime.now();

        for (HsValidatedEntity val : euValidated) {
            if (targetCountry.equalsIgnoreCase(val.getCountry()) && "EU".equalsIgnoreCase(val.getCustomsTerritory())) {
                String ch = val.getChapter();
                if (!SUPPORTED_CHAPTERS.contains(ch) || "UNSUPPORTED".equals(val.getCategory())) {
                    continue;
                }

                String recordHash = computeRecordHash(val.getNationalCode(), val.getOfficialDescription(), val.getUnit());

                HsMasterEntity master = HsMasterEntity.builder()
                        .customsTerritory("EU")
                        .country(targetCountry)
                        .chapter(val.getChapter())
                        .heading(val.getHeading())
                        .hs6(val.getHs6())
                        .nationalCode(val.getNationalCode())
                        .codeLength(8)
                        .nomenclatureType("CN")
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
                                .customsTerritory("EU")
                                .country(targetCountry)
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
                        .customsTerritory("EU")
                        .country(targetCountry)
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
        log.info("COMPLETED EU CN 2026 RDF/XML ETL PIPELINE FOR {}", targetCountry);
        log.info("Concepts Parsed: {}, Raw Extracted: {}, Validated: {}, Rejections: {}, Mapped: {}, Master Loaded: {}, Time: {} ms",
                extractResult.getTotalConceptsExtracted(), totalRawRead, totalValidatedInserted, totalRejected, totalCategoryMapped, totalMasterLoaded, duration);
        log.info("================================================================================");

        return EuCnRdfSummary.builder()
                .country(targetCountry)
                .rdfFilePath(filePath)
                .totalDescriptionsParsed(extractResult.getTotalDescriptionsParsed())
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
