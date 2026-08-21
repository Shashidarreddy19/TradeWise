package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Steps 2–7: HS-level Regulatory Coverage Pipeline Service.
 *
 * For every HS code in hs_master × country, determines applicable regulation
 * by matching against regulation_hs_mapping using strict priority:
 *
 *   1. EXACT_NATIONAL_CODE — regulation_hs_mapping.national_code = hs_master.national_code
 *   2. HS6               — regulation_hs_mapping.hs6 = hs_master.hs6
 *   3. HS4_HEADING       — regulation_hs_mapping.heading = hs_master.heading
 *   4. HS2_CHAPTER       — regulation_hs_mapping.chapter = hs_master.chapter
 *   5. NOT_FOUND         → stores NO_SPECIFIC_HS_REGULATION_FOUND (no fabrication)
 *
 * Results are persisted to hs_regulatory_coverage_audit with UPSERT semantics
 * (UNIQUE on country + hs_code prevents duplicate rows).
 */
@Service
@Slf4j
public class HsRegulatoryCoverageService {

    // Coverage status constants
    public static final String STATUS_COMPLETE = "COMPLETE";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_NOT_FOUND = "NO_SPECIFIC_REGULATION_FOUND";
    public static final String STATUS_UNAVAILABLE = "DATA_UNAVAILABLE";

    // Mapping method constants
    public static final String METHOD_EXACT = "EXACT_NATIONAL_CODE";
    public static final String METHOD_HS6 = "HS6";
    public static final String METHOD_HS4 = "HS4_HEADING";
    public static final String METHOD_HS2 = "HS2_CHAPTER";
    public static final String METHOD_NOT_FOUND = "NOT_FOUND";

    private final HsMasterRepository hsMasterRepository;
    private final RegulationHsMappingRepository hsMappingRepository;
    private final RegulationMasterRepository regulationMasterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final HsRegulatoryCoverageAuditRepository coverageAuditRepository;

    public HsRegulatoryCoverageService(
            HsMasterRepository hsMasterRepository,
            RegulationHsMappingRepository hsMappingRepository,
            RegulationMasterRepository regulationMasterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            HsRegulatoryCoverageAuditRepository coverageAuditRepository) {
        this.hsMasterRepository = hsMasterRepository;
        this.hsMappingRepository = hsMappingRepository;
        this.regulationMasterRepository = regulationMasterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
        this.coverageAuditRepository = coverageAuditRepository;
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CountryCoverageReportDto {
        private String country;
        private long totalHsCodes;
        private long mappedHsCodes;
        private long unmappedHsCodes;
        private long completeRecords;
        private long partialRecords;
        private long noSpecificRegulationFound;
        private long dataUnavailable;
        private double coveragePercent;
        private long tariffCoverage;
        private long documentCoverage;
        private long certificationCoverage;
        private long labelingCoverage;
        private long restrictionCoverage;
        private long spsCoverage;
        private long tbtCoverage;
        private long evidenceBackedRecords;
        private long notFoundRecords;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GlobalHsCoverageReportDto {
        private int countriesAudited;
        private long globalTotalHsCodes;
        private long globalMappedHsCodes;
        private long globalUnmappedHsCodes;
        private double globalCoveragePercent;
        private List<CountryCoverageReportDto> countryReports;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HsCodeComplianceProfileDto {
        private String country;
        private String hsCode;
        private String productDescription;
        private String coverageStatus;
        private String mappingMethod;
        private Double confidenceScore;
        private Long regulationId;
        private String regulationTitle;
        private String regulationType;
        private String authority;
        private String sourceUrl;
        private String sourceReference;
        private Integer documentsFound;
        private Integer certificationsFound;
        private Integer restrictionsFound;
        private Integer labelingFound;
        private Integer proceduresFound;
        private Boolean tariffFound;
        private Boolean evidenceComplete;
        private LocalDateTime effectiveDate;
        private LocalDateTime auditTimestamp;
    }

    // -------------------------------------------------------------------------
    // Core Processing
    // -------------------------------------------------------------------------

    /**
     * Build HS regulatory coverage for a single country.
     * Processes ALL hs_master entries for this country.
     */
    @Transactional
    public CountryCoverageReportDto buildCoverageForCountry(String country) {
        log.info("Building HS regulatory coverage for [{}]...", country);

        List<HsMasterEntity> allHsCodes = hsMasterRepository.findByCountry(country);
        if (allHsCodes.isEmpty()) {
            log.warn("No HS codes found in hs_master for country: {}", country);
            return CountryCoverageReportDto.builder()
                    .country(country).totalHsCodes(0).coveragePercent(0.0)
                    .noSpecificRegulationFound(0).unmappedHsCodes(0).build();
        }
        List<RegulationMasterEntity> countryRegulations = regulationMasterRepository.findByCountry(country);

        // Build lookup caches to avoid per-row N+1 queries
        // Cache: regulation_id → list of mapping entities
        List<Long> regIds = countryRegulations.stream().map(RegulationMasterEntity::getId).toList();

        // Build hs-mapping caches by level
        Map<String, List<RegulationHsMappingEntity>> byNationalCode = new HashMap<>();
        Map<String, List<RegulationHsMappingEntity>> byHs6 = new HashMap<>();
        Map<String, List<RegulationHsMappingEntity>> byHeading = new HashMap<>();
        Map<String, List<RegulationHsMappingEntity>> byChapter = new HashMap<>();

        if (!regIds.isEmpty()) {
            List<RegulationHsMappingEntity> allMappings = hsMappingRepository.findByRegulationIdIn(regIds);
            for (RegulationHsMappingEntity m : allMappings) {
                if (m.getNationalCode() != null && !m.getNationalCode().isBlank())
                    byNationalCode.computeIfAbsent(m.getNationalCode().trim(), k -> new ArrayList<>()).add(m);
                if (m.getHs6() != null && !m.getHs6().isBlank())
                    byHs6.computeIfAbsent(m.getHs6().trim(), k -> new ArrayList<>()).add(m);
                if (m.getHeading() != null && !m.getHeading().isBlank())
                    byHeading.computeIfAbsent(m.getHeading().trim(), k -> new ArrayList<>()).add(m);
                if (m.getChapter() != null && !m.getChapter().isBlank())
                    byChapter.computeIfAbsent(m.getChapter().trim(), k -> new ArrayList<>()).add(m);
            }
        }

        // Cache regulation_master by id
        Map<Long, RegulationMasterEntity> regulationById = countryRegulations.stream()
                .collect(Collectors.toMap(RegulationMasterEntity::getId, r -> r, (a, b) -> a));

        // Build child-table count caches by regulation_id (avoid per-row queries)
        Map<Long, Integer> docCountByRegId = new HashMap<>();
        Map<Long, Integer> certCountByRegId = new HashMap<>();
        Map<Long, Integer> labelCountByRegId = new HashMap<>();
        Map<Long, Integer> restrCountByRegId = new HashMap<>();
        Map<Long, Integer> procCountByRegId = new HashMap<>();

        if (!regIds.isEmpty()) {
            documentRepository.findByRegulationIdIn(regIds).forEach(d ->
                    docCountByRegId.merge(d.getRegulationId(), 1, Integer::sum));
            certificationRepository.findByRegulationIdIn(regIds).forEach(c ->
                    certCountByRegId.merge(c.getRegulationId(), 1, Integer::sum));
            labelingRepository.findByRegulationIdIn(regIds).forEach(l ->
                    labelCountByRegId.merge(l.getRegulationId(), 1, Integer::sum));
            restrictionRepository.findByRegulationIdIn(regIds).forEach(r ->
                    restrCountByRegId.merge(r.getRegulationId(), 1, Integer::sum));
            procedureRepository.findByRegulationIdIn(regIds).forEach(p ->
                    procCountByRegId.merge(p.getRegulationId(), 1, Integer::sum));
        }

        long processed = 0;
        for (HsMasterEntity hs : allHsCodes) {
            processHsCode(hs, country, byNationalCode, byHs6, byHeading, byChapter,
                    regulationById, docCountByRegId, certCountByRegId, labelCountByRegId,
                    restrCountByRegId, procCountByRegId);
            processed++;
            if (processed % 500 == 0) {
                log.info("  [{}] Processed {}/{} HS codes...", country, processed, allHsCodes.size());
            }
        }

        log.info("HS coverage build complete for [{}]: {} HS codes processed.", country, allHsCodes.size());
        return buildCountryReport(country, allHsCodes.size());
    }

    private void processHsCode(
            HsMasterEntity hs, String country,
            Map<String, List<RegulationHsMappingEntity>> byNationalCode,
            Map<String, List<RegulationHsMappingEntity>> byHs6,
            Map<String, List<RegulationHsMappingEntity>> byHeading,
            Map<String, List<RegulationHsMappingEntity>> byChapter,
            Map<Long, RegulationMasterEntity> regulationById,
            Map<Long, Integer> docCount, Map<Long, Integer> certCount,
            Map<Long, Integer> labelCount, Map<Long, Integer> restrCount,
            Map<Long, Integer> procCount) {

        String hsCode = hs.getNationalCode();
        if (hsCode == null || hsCode.isBlank()) return;

        // UPSERT: skip if already audited
        Optional<HsRegulatoryCoverageAuditEntity> existing = coverageAuditRepository.findByCountryAndHsCode(country, hsCode);
        HsRegulatoryCoverageAuditEntity audit = existing.orElseGet(HsRegulatoryCoverageAuditEntity::new);

        audit.setCountry(country);
        audit.setHsCode(hsCode);
        audit.setHs2(hs.getChapter());
        audit.setHs4(hs.getHeading());
        audit.setHs6(hs.getHs6());
        audit.setNationalCode(hs.getNationalCode());
        audit.setProductDescription(hs.getOfficialDescription());

        // Strict hierarchy matching
        RegulationHsMappingEntity match = null;
        String method = METHOD_NOT_FOUND;
        double confidence = 0.0;

        // 1. Exact national code
        List<RegulationHsMappingEntity> matches = byNationalCode.getOrDefault(hsCode.trim(), List.of());
        if (!matches.isEmpty()) { match = matches.get(0); method = METHOD_EXACT; confidence = 0.95; }

        // 2. HS6
        if (match == null && hs.getHs6() != null) {
            matches = byHs6.getOrDefault(hs.getHs6().trim(), List.of());
            if (!matches.isEmpty()) { match = matches.get(0); method = METHOD_HS6; confidence = 0.80; }
        }

        // 3. HS4 heading
        if (match == null && hs.getHeading() != null) {
            matches = byHeading.getOrDefault(hs.getHeading().trim(), List.of());
            if (!matches.isEmpty()) { match = matches.get(0); method = METHOD_HS4; confidence = 0.65; }
        }

        // 4. HS2 chapter
        if (match == null && hs.getChapter() != null) {
            matches = byChapter.getOrDefault(hs.getChapter().trim(), List.of());
            if (!matches.isEmpty()) { match = matches.get(0); method = METHOD_HS2; confidence = 0.50; }
        }

        audit.setMappingMethod(method);
        audit.setConfidenceScore(confidence);

        if (match != null) {
            Long regId = match.getRegulationId();
            RegulationMasterEntity reg = regulationById.get(regId);

            audit.setRegulationFound(true);
            audit.setRegulationId(regId);
            audit.setRegulationTitle(reg != null ? reg.getTitle() : null);
            audit.setRegulationType(reg != null ? reg.getRegulationType() : null);
            audit.setAuthority(reg != null ? reg.getAuthority() : null);
            audit.setSourceUrl(reg != null ? reg.getSourceUrl() : null);
            audit.setSourceReference(match.getSourceReference());
            audit.setEffectiveDate(reg != null ? reg.getEffectiveDate() : null);

            // Category counts from cached child tables
            int docs = docCount.getOrDefault(regId, 0);
            int certs = certCount.getOrDefault(regId, 0);
            int labels = labelCount.getOrDefault(regId, 0);
            int restr = restrCount.getOrDefault(regId, 0);
            int procs = procCount.getOrDefault(regId, 0);

            audit.setDocumentsFound(docs);
            audit.setCertificationsFound(certs);
            audit.setLabelingFound(labels);
            audit.setRestrictionsFound(restr);
            audit.setCustomsProcedureFound(procs);
            // SPS/TBT/packaging/licensing/inspection/origin inferred from regulation_type
            String regType = reg != null ? (reg.getRegulationType() != null ? reg.getRegulationType().toUpperCase() : "") : "";
            audit.setSpsFound(regType.contains("SPS") || regType.contains("FOOD") || regType.contains("AGRI") ? 1 : 0);
            audit.setTbtFound(regType.contains("TBT") || regType.contains("STANDARD") || regType.contains("TECHNICAL") ? 1 : 0);
            audit.setLicensingFound(regType.contains("LICENS") || regType.contains("PERMIT") ? 1 : 0);
            audit.setInspectionFound(regType.contains("INSPECT") || regType.contains("BORDER") ? 1 : 0);
            audit.setOriginRulesFound(regType.contains("ORIGIN") || regType.contains("COO") ? 1 : 0);
            audit.setSectorRulesFound(regType.contains("SECTOR") || regType.contains("INDUSTRY") || regType.contains("CHEMICAL") || regType.contains("PHARMA") ? 1 : 0);
            audit.setPackagingFound(regType.contains("PACKAG") ? 1 : 0);

            audit.setTariffFound(false); // Tariff from hs_master, not from regulation_hs_mapping
            audit.setSourceCount(1);

            // Evidence completeness: at least docs or certs or restrictions must exist
            boolean complete = (docs > 0 || certs > 0) && reg != null && reg.getSourceUrl() != null;
            audit.setEvidenceComplete(complete);

            int totalCategories = docs + certs + labels + restr + procs;
            audit.setCoverageStatus(complete ? STATUS_COMPLETE : (totalCategories > 0 ? STATUS_PARTIAL : STATUS_PARTIAL));

        } else {
            // No matching regulation found at any hierarchy level
            audit.setRegulationFound(false);
            audit.setRegulationId(null);
            audit.setRegulationTitle(null);
            audit.setRegulationType(null);
            audit.setAuthority(null);
            audit.setSourceUrl(null);
            audit.setSourceReference(null);
            audit.setDocumentsFound(0);
            audit.setCertificationsFound(0);
            audit.setLabelingFound(0);
            audit.setRestrictionsFound(0);
            audit.setCustomsProcedureFound(0);
            audit.setSpsFound(0);
            audit.setTbtFound(0);
            audit.setLicensingFound(0);
            audit.setInspectionFound(0);
            audit.setOriginRulesFound(0);
            audit.setSectorRulesFound(0);
            audit.setPackagingFound(0);
            audit.setTariffFound(false);
            audit.setEvidenceComplete(false);
            audit.setSourceCount(0);
            audit.setCoverageStatus(STATUS_NOT_FOUND);
        }

        coverageAuditRepository.save(audit);
    }

    public CountryCoverageReportDto buildCountryReport(String country, int totalHsCodes) {
        long mapped = coverageAuditRepository.countByCountryAndRegulationFound(country, true);
        long complete = coverageAuditRepository.countByCountryAndCoverageStatus(country, STATUS_COMPLETE);
        long partial = coverageAuditRepository.countByCountryAndCoverageStatus(country, STATUS_PARTIAL);
        long notFound = coverageAuditRepository.countByCountryAndCoverageStatus(country, STATUS_NOT_FOUND);
        long unavailable = coverageAuditRepository.countByCountryAndCoverageStatus(country, STATUS_UNAVAILABLE);
        long unmapped = totalHsCodes - mapped;
        double coveragePct = totalHsCodes == 0 ? 0.0 : Math.round((mapped * 100.0 / totalHsCodes) * 100.0) / 100.0;

        return CountryCoverageReportDto.builder()
                .country(country)
                .totalHsCodes(totalHsCodes)
                .mappedHsCodes(mapped)
                .unmappedHsCodes(unmapped)
                .completeRecords(complete)
                .partialRecords(partial)
                .noSpecificRegulationFound(notFound)
                .dataUnavailable(unavailable)
                .coveragePercent(coveragePct)
                .evidenceBackedRecords(mapped)
                .notFoundRecords(notFound)
                .build();
    }

    /**
     * Build HS regulatory coverage for ALL 11 supported countries.
     */
    @Transactional
    public GlobalHsCoverageReportDto buildCoverageForAllCountries() {
        log.info("==========================================================================================");
        log.info("STARTING CBEC-AI HS REGULATORY COVERAGE PIPELINE (ALL 11 COUNTRIES)");
        log.info("==========================================================================================");

        List<CountryCoverageReportDto> reports = new ArrayList<>();
        long globalTotal = 0;
        long globalMapped = 0;

        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            try {
                CountryCoverageReportDto report = buildCoverageForCountry(country);
                reports.add(report);
                globalTotal += report.getTotalHsCodes();
                globalMapped += report.getMappedHsCodes();
            } catch (Exception e) {
                log.error("Coverage build failed for {}: {}", country, e.getMessage(), e);
            }
        }

        double globalCoveragePct = globalTotal == 0 ? 0.0 :
                Math.round((globalMapped * 100.0 / globalTotal) * 100.0) / 100.0;

        log.info("==========================================================================================");
        log.info("HS REGULATORY COVERAGE COMPLETE: {}/{} HS codes mapped ({} %)",
                globalMapped, globalTotal, globalCoveragePct);
        log.info("==========================================================================================");

        return GlobalHsCoverageReportDto.builder()
                .countriesAudited(reports.size())
                .globalTotalHsCodes(globalTotal)
                .globalMappedHsCodes(globalMapped)
                .globalUnmappedHsCodes(globalTotal - globalMapped)
                .globalCoveragePercent(globalCoveragePct)
                .countryReports(reports)
                .build();
    }

    /**
     * Query full compliance profile for a specific country + HS code.
     * Returns evidence-backed data from hs_regulatory_coverage_audit.
     */
    public HsCodeComplianceProfileDto queryCoverageByHsCode(String country, String hsCode) {
        Optional<HsRegulatoryCoverageAuditEntity> opt = coverageAuditRepository.findByCountryAndHsCode(country, hsCode);

        if (opt.isEmpty()) {
            // Try by hs6 if national code not found
            List<HsRegulatoryCoverageAuditEntity> byHs6 = coverageAuditRepository.findByCountryAndHs6(country, hsCode);
            if (!byHs6.isEmpty()) {
                return toComplianceProfileDto(byHs6.get(0));
            }
            return HsCodeComplianceProfileDto.builder()
                    .country(country)
                    .hsCode(hsCode)
                    .coverageStatus(STATUS_UNAVAILABLE)
                    .mappingMethod(METHOD_NOT_FOUND)
                    .build();
        }

        return toComplianceProfileDto(opt.get());
    }

    private HsCodeComplianceProfileDto toComplianceProfileDto(HsRegulatoryCoverageAuditEntity e) {
        return HsCodeComplianceProfileDto.builder()
                .country(e.getCountry())
                .hsCode(e.getHsCode())
                .productDescription(e.getProductDescription())
                .coverageStatus(e.getCoverageStatus())
                .mappingMethod(e.getMappingMethod())
                .confidenceScore(e.getConfidenceScore())
                .regulationId(e.getRegulationId())
                .regulationTitle(e.getRegulationTitle())
                .regulationType(e.getRegulationType())
                .authority(e.getAuthority())
                .sourceUrl(e.getSourceUrl())
                .sourceReference(e.getSourceReference())
                .documentsFound(e.getDocumentsFound())
                .certificationsFound(e.getCertificationsFound())
                .restrictionsFound(e.getRestrictionsFound())
                .labelingFound(e.getLabelingFound())
                .proceduresFound(e.getCustomsProcedureFound())
                .tariffFound(e.getTariffFound())
                .evidenceComplete(e.getEvidenceComplete())
                .effectiveDate(e.getEffectiveDate())
                .auditTimestamp(e.getAuditTimestamp())
                .build();
    }

    private void seedDefaultHsMasterForCountry(String country) {
        String[][] sampleProducts = {
            {"07020000", "07", "0702", "070200", "Tomatoes, fresh or chilled", "VEGETABLES"},
            {"08039010", "08", "0803", "080390", "Fresh Bananas", "FRUIT"},
            {"09041100", "09", "0904", "090411", "Black Pepper, neither crushed nor ground", "SPICES"},
            {"09023000", "09", "0902", "090230", "Black Tea in immediate packings", "TEAS"},
            {"27111200", "27", "2711", "271112", "Liquefied Propane", "ENERGY"},
            {"29023000", "29", "2902", "290230", "Toluene", "CHEMICALS"},
            {"30049010", "30", "3004", "300490", "Medicaments consisting of mixed or unmixed products", "PHARMACEUTICALS"},
            {"30042010", "30", "3004", "300420", "Antibiotics formulated for therapeutic use", "PHARMACEUTICALS"},
            {"33049900", "33", "3304", "330499", "Beauty or make-up preparations for skin care", "COSMETICS"},
            {"33041000", "33", "3304", "330410", "Lip make-up preparations", "COSMETICS"},
            {"38089100", "38", "3808", "380891", "Insecticides for agricultural use", "CHEMICALS"},
            {"39011000", "39", "3901", "390110", "Polyethylene having a specific gravity of less than 0.94", "POLYMERS"},
            {"42022100", "42", "4202", "420221", "Handbags with outer surface of leather", "LEATHER_GOODS"},
            {"61091000", "61", "6109", "610910", "T-shirts, singlets and other vests, knitted or crocheted, of cotton", "APPAREL"},
            {"61099010", "61", "6109", "610990", "T-shirts of synthetic fibers", "APPAREL"},
            {"62034200", "62", "6203", "620342", "Men's or boys' trousers, bib and brace overalls, of cotton", "APPAREL"},
            {"62046200", "62", "6204", "620462", "Women's or girls' trousers, bib and brace overalls, of cotton", "APPAREL"},
            {"63026000", "63", "6302", "630260", "Toilet linen and kitchen linen, of terry towelling, of cotton", "TEXTILES"},
            {"64039990", "64", "6403", "640399", "Footwear with outer soles of rubber/plastics and uppers of leather", "FOOTWEAR"},
            {"72081000", "72", "7208", "720810", "Flat-rolled products of iron or non-alloy steel", "METALS"},
            {"73181500", "73", "7318", "731815", "Other screws and bolts, whether or not with their nuts or washers", "ARTICLES_OF_IRON_STEEL"},
            {"76041000", "76", "7604", "760410", "Bars, rods and profiles of aluminum", "ALUMINUM"},
            {"84713000", "84", "8471", "847130", "Portable automatic data processing machines, weighing not more than 10 kg", "ELECTRONICS"},
            {"85171200", "85", "8517", "851712", "Telephones for cellular networks or for other wireless networks", "TELECOM"},
            {"85285200", "85", "8528", "852852", "Monitors capable of directly connecting to data processing machine", "ELECTRONICS"},
            {"85044090", "85", "8504", "850440", "Static converters, power supply units", "ELECTRICAL"},
            {"87032300", "87", "8703", "870323", "Motor cars for passenger transport, cylinder capacity 1,500cc - 3,000cc", "AUTOMOTIVE"},
            {"87082990", "87", "8708", "870829", "Other parts and accessories of bodies for motor vehicles", "AUTOMOTIVE"},
            {"90189090", "90", "9018", "901890", "Instruments and appliances used in medical, surgical, or veterinary sciences", "MEDICAL_DEVICES"},
            {"95030030", "95", "9503", "950300", "Tricycles, scooters, pedal cars and similar wheeled toys", "TOYS"}
        };

        List<HsMasterEntity> list = new ArrayList<>();
        for (String[] p : sampleProducts) {
            HsMasterEntity ent = HsMasterEntity.builder()
                    .country(country)
                    .customsTerritory(country)
                    .nationalCode(p[0])
                    .chapter(p[1])
                    .heading(p[2])
                    .hs6(p[3])
                    .codeLength(p[0].length())
                    .nomenclatureType("NATIONAL_HTS")
                    .category(p[5])
                    .officialDescription(p[4])
                    .datasetVersion("2026.1")
                    .build();
            list.add(ent);
        }
        hsMasterRepository.saveAll(list);
        log.info("Saved {} HS master inventory entries for [{}]", list.size(), country);
    }
}
