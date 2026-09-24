package com.trade.regulatory.service;

import com.trade.regulatory.entity.*;
import com.trade.regulatory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core regulatory retrieval service.
 * Implements hierarchical HS matching: EXACT_NATIONAL_CODE → HS6 → HS4_HEADING → HS2_CHAPTER.
 * NEVER fabricates data — returns actual database records with their real mapping levels.
 */
@Service
@Transactional(readOnly = true, transactionManager = "tradeDataTransactionManager")
public class RegulatoryRetrievalService {

    private final RegulationHsMappingRepository hsMappingRepo;
    private final RegulationMasterRepository regMasterRepo;
    private final RegulationDocumentRepository docRepo;
    private final RegulationCertificationRepository certRepo;
    private final RegulationLabelingRepository labelRepo;
    private final RegulationRestrictionRepository restrictRepo;
    private final RegulationProcedureRepository procRepo;
    private final RegulationSourceRepository sourceRepo;
    private final HsRegulatoryCoverageRepository coverageRepo;
    private final HsMasterRepository hsMasterRepo;
    private final CountryMasterRepository countryRepo;

    public RegulatoryRetrievalService(
            RegulationHsMappingRepository hsMappingRepo,
            RegulationMasterRepository regMasterRepo,
            RegulationDocumentRepository docRepo,
            RegulationCertificationRepository certRepo,
            RegulationLabelingRepository labelRepo,
            RegulationRestrictionRepository restrictRepo,
            RegulationProcedureRepository procRepo,
            RegulationSourceRepository sourceRepo,
            HsRegulatoryCoverageRepository coverageRepo,
            HsMasterRepository hsMasterRepo,
            CountryMasterRepository countryRepo) {
        this.hsMappingRepo = hsMappingRepo;
        this.regMasterRepo = regMasterRepo;
        this.docRepo = docRepo;
        this.certRepo = certRepo;
        this.labelRepo = labelRepo;
        this.restrictRepo = restrictRepo;
        this.procRepo = procRepo;
        this.sourceRepo = sourceRepo;
        this.coverageRepo = coverageRepo;
        this.hsMasterRepo = hsMasterRepo;
        this.countryRepo = countryRepo;
    }

    /**
     * Retrieve full regulatory profile for a country + HS code.
     * Uses hierarchical matching with transparent confidence reporting.
     */
    public RegulatoryResult getRegulations(String country, String hsCode) {
        RegulatoryResult result = new RegulatoryResult();
        result.country = country;
        result.hsCode = hsCode;

        // 1. Validate country
        Optional<CountryMasterEntity> countryEntity = resolveCountry(country);
        if (countryEntity.isEmpty()) {
            result.error = "Country not supported: " + country;
            result.supported = false;
            return result;
        }
        result.supported = true;
        String resolvedCountry = countryEntity.get().getCountryName();
        result.country = resolvedCountry;

        // 2. Resolve HS code hierarchy
        String nationalCode = hsCode.replaceAll("[^0-9]", "");
        String hs6 = nationalCode.length() >= 6 ? nationalCode.substring(0, 6) : nationalCode;
        String hs4 = nationalCode.length() >= 4 ? nationalCode.substring(0, 4) : nationalCode;
        String hs2 = nationalCode.length() >= 2 ? nationalCode.substring(0, 2) : nationalCode;

        // 3. Try HS master lookup for product description
        Optional<HsMasterEntity> hsEntity = hsMasterRepo.findByCountryAndNationalCodeAndIsCurrentTrue(resolvedCountry, nationalCode);
        if (hsEntity.isPresent()) {
            result.productDescription = hsEntity.get().getOfficialDescription();
            result.category = hsEntity.get().getCategory();
        } else {
            // Try partial matches
            List<HsMasterEntity> hs6Matches = hsMasterRepo.findByCountryAndHs6AndIsCurrentTrue(resolvedCountry, hs6);
            if (!hs6Matches.isEmpty()) {
                result.productDescription = hs6Matches.get(0).getOfficialDescription();
                result.category = hs6Matches.get(0).getCategory();
            }
        }

        // 4. Hierarchical regulation matching with STRICT country isolation
        String countryCode = countryEntity.get().getCountryCode();
        List<RegulationHsMappingEntity> mappings = hsMappingRepo.findHierarchicalByCountry(
                resolvedCountry, countryCode, nationalCode, hs6, hs4, hs2);

        if (mappings.isEmpty()) {
            // Check coverage audit for this country + HS
            Optional<HsRegulatoryCoverageEntity> coverage =
                    coverageRepo.findByCountryAndHsCode(resolvedCountry, nationalCode);
            if (coverage.isPresent()) {
                HsRegulatoryCoverageEntity cov = coverage.get();
                result.matchType = cov.getMappingMethod() != null ? cov.getMappingMethod() : "COVERAGE_AUDIT";
                result.confidence = cov.getConfidenceScore() != null ? cov.getConfidenceScore() : 0.0;
                result.regulationFound = cov.getRegulationFound() != null && cov.getRegulationFound();
                if (cov.getRegulationId() != null) {
                    loadRegulationDetails(result, List.of(cov.getRegulationId()), resolvedCountry, hs2);
                }
            } else {
                result.matchType = "NOT_FOUND";
                result.confidence = 0.0;
                result.regulationFound = false;
            }
        } else {
            // Determine best match level
            RegulationHsMappingEntity best = mappings.get(0);
            result.matchType = determineMatchType(best, nationalCode, hs6, hs4, hs2);
            result.confidence = best.getConfidenceScore() != null ? best.getConfidenceScore() : 0.7;
            result.regulationFound = true;

            List<Long> regIds = mappings.stream()
                    .map(RegulationHsMappingEntity::getRegulationId)
                    .distinct()
                    .collect(Collectors.toList());
            loadRegulationDetails(result, regIds, resolvedCountry, hs2);
        }

        // 5. Load sources for the country
        result.sources = sourceRepo.findByCountryAndStatus(resolvedCountry, "ACTIVE");

        return result;
    }

    private void loadRegulationDetails(RegulatoryResult result, List<Long> regIds, String country, String chapter) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<RegulationMasterEntity> allRegs = regMasterRepo.findAllById(regIds);

        // Filter: country match + not expired
        List<RegulationMasterEntity> validRegs = allRegs.stream()
                .filter(r -> r.getCountry() != null && (
                        r.getCountry().equalsIgnoreCase(country) ||
                        country.toLowerCase().contains(r.getCountry().toLowerCase()) ||
                        r.getCountry().toLowerCase().contains(country.toLowerCase())
                ))
                .filter(r -> r.getExpiryDate() == null || r.getExpiryDate().isAfter(now))
                .filter(r -> isProductApplicable(r, chapter))
                .collect(Collectors.toList());

        List<Long> validRegIds = validRegs.stream().map(RegulationMasterEntity::getId).collect(Collectors.toList());

        result.regulations = validRegs;
        if (!validRegIds.isEmpty()) {
            result.documents = docRepo.findByRegulationIdIn(validRegIds);
            result.certifications = certRepo.findByRegulationIdIn(validRegIds);
            result.labeling = labelRepo.findByRegulationIdIn(validRegIds);
            result.restrictions = restrictRepo.findByRegulationIdIn(validRegIds);
            result.procedures = procRepo.findByRegulationIdIn(validRegIds);
        } else {
            result.documents = new ArrayList<>();
            result.certifications = new ArrayList<>();
            result.labeling = new ArrayList<>();
            result.restrictions = new ArrayList<>();
            result.procedures = new ArrayList<>();
        }
    }

    /**
     * Hard validation guard: rejects regulations belonging exclusively to unrelated categories.
     * e.g., For food/rice (chapters 01-24), reject FCC, CPSC, TSCA, Lacey Act, telecom, medical devices.
     */
    private boolean isProductApplicable(RegulationMasterEntity reg, String chapter) {
        if (reg == null) return false;
        String auth = (reg.getAuthority() != null ? reg.getAuthority() : "").toUpperCase();
        String title = (reg.getTitle() != null ? reg.getTitle() : "").toUpperCase();
        String summary = (reg.getSummary() != null ? reg.getSummary() : "").toUpperCase();
        String text = auth + " " + title + " " + summary;

        // If food/cereals/agricultural (Ch 01-24)
        if (chapter != null && chapter.matches("0[1-9]|1[0-9]|2[0-4]")) {
            if (text.contains("FCC") || text.contains("TELECOM") || text.contains("RADIO") ||
                text.contains("CPSC") || text.contains("CHILDREN") || text.contains("TOY") ||
                text.contains("TSCA") || text.contains("CHEMICAL SUBSTANCE") ||
                text.contains("LACEY ACT") || text.contains("TIMBER") || text.contains("WOOD") ||
                text.contains("MEDICAL DEVICE") || text.contains("PHARMACEUTICAL") ||
                text.contains("AUTOMOTIVE") || text.contains("VEHICLE")) {
                return false;
            }
        }
        return true;
    }

    private String determineMatchType(RegulationHsMappingEntity mapping,
                                       String nationalCode, String hs6, String hs4, String hs2) {
        if (nationalCode.equals(mapping.getNationalCode())) return "EXACT_NATIONAL_CODE";
        if (hs6.equals(mapping.getHs6())) return "HS6";
        if (hs4.equals(mapping.getHeading())) return "HS4_HEADING";
        if (hs2.equals(mapping.getChapter())) return "HS2_CHAPTER";
        return mapping.getMappingMethod() != null ? mapping.getMappingMethod() : "UNKNOWN";
    }

    private Optional<CountryMasterEntity> resolveCountry(String country) {
        // Try by name first, then by code
        Optional<CountryMasterEntity> result = countryRepo.findByCountryName(country);
        if (result.isEmpty()) {
            result = countryRepo.findByCountryCode(country.toUpperCase());
        }
        return result;
    }

    /**
     * Calculate compliance score based on regulatory burden.
     * Deterministic formula — never LLM-driven.
     */
    public ComplianceScore calculateCompliance(RegulatoryResult regResult) {
        ComplianceScore score = new ComplianceScore();

        int docsCount = regResult.documents != null ? regResult.documents.size() : 0;
        int certsCount = regResult.certifications != null ? regResult.certifications.size() : 0;
        int labelCount = regResult.labeling != null ? regResult.labeling.size() : 0;
        int restrictCount = regResult.restrictions != null ? regResult.restrictions.size() : 0;
        int procCount = regResult.procedures != null ? regResult.procedures.size() : 0;

        score.documentsCount = docsCount;
        score.certificationsCount = certsCount;
        score.labelingCount = labelCount;
        score.restrictionsCount = restrictCount;
        score.proceduresCount = procCount;

        // Deterministic scoring formula:
        // Base 100, subtract penalties for regulatory burden
        // Documents: -2 per document (essential but manageable)
        // Certifications: -4 per cert (significant effort)
        // Restrictions: -6 per restriction (can be blocking)
        // Labeling: -1.5 per requirement (routine)
        // Procedures: -1 per step (sequential effort)
        double penalty = docsCount * 2.0
                + certsCount * 4.0
                + restrictCount * 6.0
                + labelCount * 1.5
                + procCount * 1.0;

        // Confidence adjustment: lower confidence = higher uncertainty penalty
        double confidenceMultiplier = regResult.confidence != null ? regResult.confidence : 0.5;
        if (confidenceMultiplier < 0.5) penalty += 10; // Unknown regulatory landscape penalty

        score.numericScore = (int) Math.max(10, Math.min(100, 100 - penalty));

        // Complexity classification
        if (score.numericScore >= 80) score.complexity = "LOW";
        else if (score.numericScore >= 50) score.complexity = "MEDIUM";
        else score.complexity = "HIGH";

        score.matchType = regResult.matchType;
        score.confidence = regResult.confidence;
        score.regulationFound = regResult.regulationFound;

        return score;
    }

    // ── Result DTOs ─────────────────────────────────────────────────────────

    public static class RegulatoryResult {
        public String country;
        public String hsCode;
        public String productDescription;
        public String category;
        public boolean supported = true;
        public boolean regulationFound = false;
        public String matchType;
        public Double confidence;
        public String error;
        public List<RegulationMasterEntity> regulations = new ArrayList<>();
        public List<RegulationDocumentEntity> documents = new ArrayList<>();
        public List<RegulationCertificationEntity> certifications = new ArrayList<>();
        public List<RegulationLabelingEntity> labeling = new ArrayList<>();
        public List<RegulationRestrictionEntity> restrictions = new ArrayList<>();
        public List<RegulationProcedureEntity> procedures = new ArrayList<>();
        public List<RegulationSourceEntity> sources = new ArrayList<>();
    }

    public static class ComplianceScore {
        public int numericScore;
        public String complexity;
        public String matchType;
        public Double confidence;
        public boolean regulationFound;
        public int documentsCount;
        public int certificationsCount;
        public int labelingCount;
        public int restrictionsCount;
        public int proceduresCount;
    }
}
