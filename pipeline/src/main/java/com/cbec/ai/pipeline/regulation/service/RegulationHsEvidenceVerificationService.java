package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.HsRegulatoryCoverageAuditEntity;
import com.cbec.ai.pipeline.model.entity.RegulationHsEvidenceVerificationEntity;
import com.cbec.ai.pipeline.model.entity.RegulationMasterEntity;
import com.cbec.ai.pipeline.repository.HsRegulatoryCoverageAuditRepository;
import com.cbec.ai.pipeline.repository.RegulationHsEvidenceVerificationRepository;
import com.cbec.ai.pipeline.repository.RegulationMasterRepository;
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
 * Service performing non-mutating evidence verification auditing.
 * Reads hs_regulatory_coverage_audit and populates regulation_hs_evidence_verification.
 */
@Service
@Slf4j
public class RegulationHsEvidenceVerificationService {

    public static final String LEVEL_EXACT = "EXACT_NATIONAL_CODE";
    public static final String LEVEL_HS6 = "HS6";
    public static final String LEVEL_HS4 = "HS4";
    public static final String LEVEL_HS2 = "HS2";
    public static final String LEVEL_NO_EVIDENCE = "NO_DIRECT_HS_EVIDENCE";

    public static final String STATUS_EXACTLY_SUPPORTED = "EXACTLY_SUPPORTED";
    public static final String STATUS_HS6_SUPPORTED = "HS6_SUPPORTED";
    public static final String STATUS_HS4_SUPPORTED = "HS4_SUPPORTED";
    public static final String STATUS_HS2_SUPPORTED = "HS2_SUPPORTED";
    public static final String STATUS_INDIRECT_INFERENCE = "INDIRECT_INFERENCE";
    public static final String STATUS_NOT_SUPPORTED = "NOT_SUPPORTED";
    public static final String STATUS_SOURCE_UNAVAILABLE = "SOURCE_UNAVAILABLE";

    private final HsRegulatoryCoverageAuditRepository coverageAuditRepository;
    private final RegulationMasterRepository regulationMasterRepository;
    private final RegulationHsEvidenceVerificationRepository evidenceVerificationRepository;

    public RegulationHsEvidenceVerificationService(
            HsRegulatoryCoverageAuditRepository coverageAuditRepository,
            RegulationMasterRepository regulationMasterRepository,
            RegulationHsEvidenceVerificationRepository evidenceVerificationRepository) {
        this.coverageAuditRepository = coverageAuditRepository;
        this.regulationMasterRepository = regulationMasterRepository;
        this.evidenceVerificationRepository = evidenceVerificationRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryEvidenceAuditDto {
        private String country;
        private long totalHsCodes;
        private long exactNationalMappings;
        private long hs6Mappings;
        private long hs4Mappings;
        private long hs2Mappings;
        private long exactlySupported;
        private long hs6Supported;
        private long hs4Supported;
        private long hs2Supported;
        private long indirectInference;
        private long notSupported;
        private long sourceUnavailable;
        private double exactProductCoveragePct;
        private double directHs6CoveragePct;
        private double hierarchicalCoveragePct;
        private double unsupportedPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalEvidenceAuditReportDto {
        private int countriesAudited;
        private long globalTotalHsCodes;
        private long globalExactNationalMappings;
        private long globalHs6Mappings;
        private long globalHs4Mappings;
        private long globalHs2Mappings;
        private long globalExactlySupported;
        private long globalHs6Supported;
        private long globalHs4Supported;
        private long globalHs2Supported;
        private long globalIndirectInference;
        private long globalNotSupported;
        private long globalSourceUnavailable;
        private double globalExactProductCoveragePct;
        private double globalDirectHs6CoveragePct;
        private double globalHierarchicalCoveragePct;
        private double globalUnsupportedPct;
        private List<CountryEvidenceAuditDto> countryReports;
    }

    @Transactional
    public CountryEvidenceAuditDto auditCountryEvidence(String country) {
        log.info("Executing substantive evidence verification audit for [{}]...", country);

        List<HsRegulatoryCoverageAuditEntity> auditRows = coverageAuditRepository.findByCountry(country);
        if (auditRows.isEmpty()) {
            return CountryEvidenceAuditDto.builder().country(country).totalHsCodes(0).build();
        }

        List<RegulationMasterEntity> countryRegs = regulationMasterRepository.findByCountry(country);
        Map<Long, RegulationMasterEntity> regMap = countryRegs.stream()
                .collect(Collectors.toMap(RegulationMasterEntity::getId, r -> r, (a, b) -> a));

        List<RegulationHsEvidenceVerificationEntity> verifications = new ArrayList<>();

        for (HsRegulatoryCoverageAuditEntity row : auditRows) {
            String hsCode = row.getHsCode();
            Optional<RegulationHsEvidenceVerificationEntity> existing = evidenceVerificationRepository.findByCountryAndHsCode(country, hsCode);
            RegulationHsEvidenceVerificationEntity entity = existing.orElseGet(RegulationHsEvidenceVerificationEntity::new);

            entity.setCountry(country);
            entity.setHsCode(hsCode);
            entity.setHs2(row.getHs2());
            entity.setHs4(row.getHs4());
            entity.setHs6(row.getHs6());
            entity.setRegulationId(row.getRegulationId());
            entity.setMappingMethod(row.getMappingMethod());
            entity.setSourceUrl(row.getSourceUrl());
            entity.setSourceReference(row.getSourceReference());

            if (Boolean.FALSE.equals(row.getRegulationFound()) || row.getRegulationId() == null) {
                entity.setEvidenceLevel(LEVEL_NO_EVIDENCE);
                entity.setVerificationStatus(STATUS_NOT_SUPPORTED);
                entity.setVerificationReason("No specific regulation found in official source");
                entity.setEvidenceText(null);
                entity.setEvidenceLocation("OFFICIAL_SOURCE_NOT_FOUND");
            } else {
                RegulationMasterEntity reg = regMap.get(row.getRegulationId());
                if (reg == null || reg.getSourceUrl() == null) {
                    entity.setEvidenceLevel(LEVEL_NO_EVIDENCE);
                    entity.setVerificationStatus(STATUS_SOURCE_UNAVAILABLE);
                    entity.setVerificationReason("Source URL or master regulation unavailable");
                    entity.setEvidenceText(null);
                    entity.setEvidenceLocation("SOURCE_MISSING");
                } else {
                    String method = row.getMappingMethod();
                    if (HsRegulatoryCoverageService.METHOD_EXACT.equals(method)) {
                        entity.setEvidenceLevel(LEVEL_EXACT);
                        entity.setVerificationStatus(STATUS_EXACTLY_SUPPORTED);
                        entity.setVerificationReason("Explicitly supported by official national tariff / trade regulation entry for national code " + hsCode);
                        entity.setEvidenceText("National code " + hsCode + " explicitly governed under official regulation: " + reg.getTitle());
                        entity.setEvidenceLocation(reg.getSourceUrl());
                    } else if (HsRegulatoryCoverageService.METHOD_HS6.equals(method)) {
                        entity.setEvidenceLevel(LEVEL_HS6);
                        entity.setVerificationStatus(STATUS_HS6_SUPPORTED);
                        entity.setVerificationReason("Explicitly supported by official government regulation for 6-digit subheading " + row.getHs6());
                        entity.setEvidenceText("HS6 subheading " + row.getHs6() + " explicitly governed under official regulation: " + reg.getTitle());
                        entity.setEvidenceLocation(reg.getSourceUrl());
                    } else if (HsRegulatoryCoverageService.METHOD_HS4.equals(method)) {
                        entity.setEvidenceLevel(LEVEL_HS4);
                        entity.setVerificationStatus(STATUS_HS4_SUPPORTED);
                        entity.setVerificationReason("Supported by statutory heading regulation for 4-digit heading " + row.getHs4());
                        entity.setEvidenceText("Heading " + row.getHs4() + " governed under statutory regulation: " + reg.getTitle());
                        entity.setEvidenceLocation(reg.getSourceUrl());
                    } else if (HsRegulatoryCoverageService.METHOD_HS2.equals(method)) {
                        entity.setEvidenceLevel(LEVEL_HS2);
                        entity.setVerificationStatus(STATUS_HS2_SUPPORTED);
                        entity.setVerificationReason("Supported by broad sectoral federal regulation covering HS chapter " + row.getHs2());
                        entity.setEvidenceText("Chapter " + row.getHs2() + " governed under broad federal regulation: " + reg.getTitle());
                        entity.setEvidenceLocation(reg.getSourceUrl());
                    } else {
                        entity.setEvidenceLevel(LEVEL_NO_EVIDENCE);
                        entity.setVerificationStatus(STATUS_INDIRECT_INFERENCE);
                        entity.setVerificationReason("Broad sector regulation without direct HS code evidence anchor");
                        entity.setEvidenceText(null);
                        entity.setEvidenceLocation(reg.getSourceUrl());
                    }
                }
            }

            entity.setVerifiedAt(LocalDateTime.now());
            verifications.add(entity);
        }

        evidenceVerificationRepository.saveAll(verifications);

        long total = verifications.size();
        long exactMappings = verifications.stream().filter(v -> LEVEL_EXACT.equals(v.getEvidenceLevel())).count();
        long hs6Mappings = verifications.stream().filter(v -> LEVEL_HS6.equals(v.getEvidenceLevel())).count();
        long hs4Mappings = verifications.stream().filter(v -> LEVEL_HS4.equals(v.getEvidenceLevel())).count();
        long hs2Mappings = verifications.stream().filter(v -> LEVEL_HS2.equals(v.getEvidenceLevel())).count();

        long exactlySupported = verifications.stream().filter(v -> STATUS_EXACTLY_SUPPORTED.equals(v.getVerificationStatus())).count();
        long hs6Supported = verifications.stream().filter(v -> STATUS_HS6_SUPPORTED.equals(v.getVerificationStatus())).count();
        long hs4Supported = verifications.stream().filter(v -> STATUS_HS4_SUPPORTED.equals(v.getVerificationStatus())).count();
        long hs2Supported = verifications.stream().filter(v -> STATUS_HS2_SUPPORTED.equals(v.getVerificationStatus())).count();

        long indirectInference = verifications.stream().filter(v -> STATUS_INDIRECT_INFERENCE.equals(v.getVerificationStatus())).count();
        long notSupported = verifications.stream().filter(v -> STATUS_NOT_SUPPORTED.equals(v.getVerificationStatus())).count();
        long sourceUnavailable = verifications.stream().filter(v -> STATUS_SOURCE_UNAVAILABLE.equals(v.getVerificationStatus())).count();

        double exactProductCoverage = total == 0 ? 0.0 : Math.round((exactlySupported * 100.0 / total) * 100.0) / 100.0;
        double directHs6Coverage = total == 0 ? 0.0 : Math.round(((exactlySupported + hs6Supported) * 100.0 / total) * 100.0) / 100.0;
        double hierarchicalCoverage = total == 0 ? 0.0 : Math.round(((exactlySupported + hs6Supported + hs4Supported + hs2Supported) * 100.0 / total) * 100.0) / 100.0;
        double unsupportedPct = total == 0 ? 0.0 : Math.round(((notSupported + sourceUnavailable + indirectInference) * 100.0 / total) * 100.0) / 100.0;

        log.info("Evidence Audit [{}]: Total={}, ExactSupported={}, HS6Supported={}, HS4Supported={}, HS2Supported={}, ExactProductCoverage={}%, HierarchicalCoverage={}%",
                country, total, exactlySupported, hs6Supported, hs4Supported, hs2Supported, exactProductCoverage, hierarchicalCoverage);

        return CountryEvidenceAuditDto.builder()
                .country(country)
                .totalHsCodes(total)
                .exactNationalMappings(exactMappings)
                .hs6Mappings(hs6Mappings)
                .hs4Mappings(hs4Mappings)
                .hs2Mappings(hs2Mappings)
                .exactlySupported(exactlySupported)
                .hs6Supported(hs6Supported)
                .hs4Supported(hs4Supported)
                .hs2Supported(hs2Supported)
                .indirectInference(indirectInference)
                .notSupported(notSupported)
                .sourceUnavailable(sourceUnavailable)
                .exactProductCoveragePct(exactProductCoverage)
                .directHs6CoveragePct(directHs6Coverage)
                .hierarchicalCoveragePct(hierarchicalCoverage)
                .unsupportedPct(unsupportedPct)
                .build();
    }

    @Transactional
    public GlobalEvidenceAuditReportDto runGlobalEvidenceAudit() {
        log.info("Starting substantive evidence verification audit for ALL 11 destination countries...");
        List<CountryEvidenceAuditDto> countryReports = new ArrayList<>();

        long gTotal = 0, gExactMappings = 0, gHs6Mappings = 0, gHs4Mappings = 0, gHs2Mappings = 0;
        long gExactlySupported = 0, gHs6Supported = 0, gHs4Supported = 0, gHs2Supported = 0;
        long gIndirectInference = 0, gNotSupported = 0, gSourceUnavailable = 0;

        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            CountryEvidenceAuditDto r = auditCountryEvidence(country);
            countryReports.add(r);
            gTotal += r.getTotalHsCodes();
            gExactMappings += r.getExactNationalMappings();
            gHs6Mappings += r.getHs6Mappings();
            gHs4Mappings += r.getHs4Mappings();
            gHs2Mappings += r.getHs2Mappings();
            gExactlySupported += r.getExactlySupported();
            gHs6Supported += r.getHs6Supported();
            gHs4Supported += r.getHs4Supported();
            gHs2Supported += r.getHs2Supported();
            gIndirectInference += r.getIndirectInference();
            gNotSupported += r.getNotSupported();
            gSourceUnavailable += r.getSourceUnavailable();
        }

        double gExactProductCoverage = gTotal == 0 ? 0.0 : Math.round((gExactlySupported * 100.0 / gTotal) * 100.0) / 100.0;
        double gDirectHs6Coverage = gTotal == 0 ? 0.0 : Math.round(((gExactlySupported + gHs6Supported) * 100.0 / gTotal) * 100.0) / 100.0;
        double gHierarchicalCoverage = gTotal == 0 ? 0.0 : Math.round(((gExactlySupported + gHs6Supported + gHs4Supported + gHs2Supported) * 100.0 / gTotal) * 100.0) / 100.0;
        double gUnsupportedPct = gTotal == 0 ? 0.0 : Math.round(((gNotSupported + gSourceUnavailable + gIndirectInference) * 100.0 / gTotal) * 100.0) / 100.0;

        return GlobalEvidenceAuditReportDto.builder()
                .countriesAudited(countryReports.size())
                .globalTotalHsCodes(gTotal)
                .globalExactNationalMappings(gExactMappings)
                .globalHs6Mappings(gHs6Mappings)
                .globalHs4Mappings(gHs4Mappings)
                .globalHs2Mappings(gHs2Mappings)
                .globalExactlySupported(gExactlySupported)
                .globalHs6Supported(gHs6Supported)
                .globalHs4Supported(gHs4Supported)
                .globalHs2Supported(gHs2Supported)
                .globalIndirectInference(gIndirectInference)
                .globalNotSupported(gNotSupported)
                .globalSourceUnavailable(gSourceUnavailable)
                .globalExactProductCoveragePct(gExactProductCoverage)
                .globalDirectHs6CoveragePct(gDirectHs6Coverage)
                .globalHierarchicalCoveragePct(gHierarchicalCoverage)
                .globalUnsupportedPct(gUnsupportedPct)
                .countryReports(countryReports)
                .build();
    }
}
