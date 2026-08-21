package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComplianceDataQualityService {

    private final RegulationMasterRepository masterRepository;
    private final RegulationHsMappingRepository hsMappingRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final RegulationSourceRepository sourceRepository;

    public ComplianceDataQualityService(
            RegulationMasterRepository masterRepository,
            RegulationHsMappingRepository hsMappingRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            RegulationSourceRepository sourceRepository) {
        this.masterRepository = masterRepository;
        this.hsMappingRepository = hsMappingRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
        this.sourceRepository = sourceRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityReportResponseDto {
        private Long totalRecords;
        private Long validRecords;
        private Long invalidRecords;
        private Long orphanRecords;
        private Long missingEvidence;
        private Long duplicateMappings;
        private Double qualityScore; // 0.0 to 100.0%
        private Map<String, Long> issueBreakdown;
    }

    public DataQualityReportResponseDto runDataQualityAudit() {
        log.info("Executing automated database hygiene & regulatory data quality audit");

        List<RegulationMasterEntity> masters = masterRepository.findAll();
        List<RegulationHsMappingEntity> mappings = hsMappingRepository.findAll();
        List<RegulationDocumentEntity> docs = documentRepository.findAll();
        List<RegulationCertificationEntity> certs = certificationRepository.findAll();
        List<RegulationLabelingEntity> labels = labelingRepository.findAll();
        List<RegulationRestrictionEntity> restrs = restrictionRepository.findAll();
        List<RegulationProcedureEntity> procs = procedureRepository.findAll();
        List<RegulationSourceEntity> sources = sourceRepository.findAll();

        Set<Long> masterIds = masters.stream().map(RegulationMasterEntity::getId).collect(Collectors.toSet());

        long orphanMappings = mappings.stream().filter(m -> !masterIds.contains(m.getRegulationId())).count();
        long orphanDocs = docs.stream().filter(d -> !masterIds.contains(d.getRegulationId())).count();
        long orphanCerts = certs.stream().filter(c -> !masterIds.contains(c.getRegulationId())).count();
        long orphanLabels = labels.stream().filter(l -> !masterIds.contains(l.getRegulationId())).count();
        long orphanRestrs = restrs.stream().filter(r -> !masterIds.contains(r.getRegulationId())).count();
        long orphanProcs = procs.stream().filter(p -> !masterIds.contains(p.getRegulationId())).count();
        long totalOrphanRecords = orphanMappings + orphanDocs + orphanCerts + orphanLabels + orphanRestrs + orphanProcs;

        long missingSourceUrls = sources.stream().filter(s -> s.getSourceUrl() == null || s.getSourceUrl().isBlank()).count();
        long missingAuthorities = sources.stream().filter(s -> s.getAuthority() == null || s.getAuthority().isBlank()).count();
        long missingConfidence = masters.stream().filter(m -> m.getConfidenceScore() == null).count();
        long missingSourceRef = mappings.stream().filter(m -> m.getSourceReference() == null || m.getSourceReference().isBlank()).count();
        long totalMissingEvidence = missingSourceUrls + missingAuthorities + missingConfidence + missingSourceRef;

        // Check for duplicate HS mappings
        Set<String> seenMappings = new HashSet<>();
        long duplicateMappingsCount = 0;
        for (RegulationHsMappingEntity m : mappings) {
            String key = m.getRegulationId() + "_" + m.getChapter() + "_" + m.getHeading() + "_" + m.getHs6() + "_" + m.getNationalCode();
            if (!seenMappings.add(key)) {
                duplicateMappingsCount++;
            }
        }

        // Invalid HS codes (contain non-digits or length > 12)
        long invalidHsCodes = mappings.stream()
                .filter(m -> m.getNationalCode() != null && (!m.getNationalCode().matches("^[0-9]+$") || m.getNationalCode().length() > 12))
                .count();

        long totalRecordsChecked = masters.size() + mappings.size() + docs.size() + certs.size() + labels.size() + restrs.size() + procs.size() + sources.size();
        long invalidRecords = totalOrphanRecords + totalMissingEvidence + duplicateMappingsCount + invalidHsCodes;
        long validRecords = Math.max(0, totalRecordsChecked - invalidRecords);

        double qualityScore = totalRecordsChecked > 0
                ? Math.round(((double) validRecords / totalRecordsChecked * 100.0) * 100.0) / 100.0
                : 100.0;

        Map<String, Long> issueBreakdown = new LinkedHashMap<>();
        issueBreakdown.put("orphanMappings", orphanMappings);
        issueBreakdown.put("orphanChildRequirements", orphanDocs + orphanCerts + orphanLabels + orphanRestrs + orphanProcs);
        issueBreakdown.put("missingSourceUrls", missingSourceUrls);
        issueBreakdown.put("missingAuthorities", missingAuthorities);
        issueBreakdown.put("missingSourceReferences", missingSourceRef);
        issueBreakdown.put("duplicateMappings", duplicateMappingsCount);
        issueBreakdown.put("invalidHsCodes", invalidHsCodes);

        return DataQualityReportResponseDto.builder()
                .totalRecords(totalRecordsChecked)
                .validRecords(validRecords)
                .invalidRecords(invalidRecords)
                .orphanRecords(totalOrphanRecords)
                .missingEvidence(totalMissingEvidence)
                .duplicateMappings(duplicateMappingsCount)
                .qualityScore(qualityScore)
                .issueBreakdown(issueBreakdown)
                .build();
    }
}
