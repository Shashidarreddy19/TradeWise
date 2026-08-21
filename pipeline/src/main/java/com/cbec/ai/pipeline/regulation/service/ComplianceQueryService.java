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
public class ComplianceQueryService {

    private final RegulationMasterRepository masterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final RegulationHsMappingRepository hsMappingRepository;
    private final RegulationSourceRepository sourceRepository;

    public ComplianceQueryService(
            RegulationMasterRepository masterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            RegulationHsMappingRepository hsMappingRepository,
            RegulationSourceRepository sourceRepository) {
        this.masterRepository = masterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
        this.hsMappingRepository = hsMappingRepository;
        this.sourceRepository = sourceRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheckRequestDto {
        private String country;
        private String hsCode;
        private String product;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheckResponseDto {
        private String country;
        private String hsCode;
        private String product;
        private List<RegulationMasterDto> regulations;
        private List<DocumentDto> documents;
        private List<CertificationDto> certifications;
        private List<LabelingDto> labeling;
        private List<RestrictionDto> restrictions;
        private List<ProcedureDto> procedures;
        private List<SourceDto> sources;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulationMasterDto {
        private Long id;
        private String title;
        private String authority;
        private String regulationType;
        private String summary;
        private String sourceUrl;
        private Double confidenceScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private String documentName;
        private Boolean mandatory;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificationDto {
        private String certificationName;
        private Boolean mandatory;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelingDto {
        private String requirement;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestrictionDto {
        private String restrictionType;
        private String description;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcedureDto {
        private String procedureName;
        private String description;
        private Integer stepOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDto {
        private String authority;
        private String title;
        private String sourceUrl;
        private String format;
    }

    public ComplianceCheckResponseDto checkCompliance(ComplianceCheckRequestDto request) {
        String country = request.getCountry();
        String cleanHs = request.getHsCode() != null ? request.getHsCode().replaceAll("[^0-9]", "") : "";
        String product = request.getProduct();

        log.info("Executing compliance check for country: '{}', hsCode: '{}', product: '{}'", country, cleanHs, product);

        String chapter = cleanHs.length() >= 2 ? cleanHs.substring(0, 2) : null;
        String heading = cleanHs.length() >= 4 ? cleanHs.substring(0, 4) : null;
        String hs6 = cleanHs.length() >= 6 ? cleanHs.substring(0, 6) : null;

        List<RegulationMasterEntity> countryMasters = masterRepository.findByCountry(country);
        Set<Long> matchedMasterIds = new LinkedHashSet<>();

        for (RegulationMasterEntity master : countryMasters) {
            List<RegulationHsMappingEntity> mappings = hsMappingRepository.findByRegulationId(master.getId());
            boolean match = false;

            if (mappings.isEmpty()) {
                // Default match all fallback for country broad regulatory domain
                match = true;
            } else {
                for (RegulationHsMappingEntity m : mappings) {
                    if (cleanHs.equals(m.getNationalCode()) ||
                            (hs6 != null && hs6.equals(m.getHs6())) ||
                            (heading != null && heading.equals(m.getHeading())) ||
                            (chapter != null && chapter.equals(m.getChapter()))) {
                        match = true;
                        break;
                    }
                }
            }

            if (match) {
                matchedMasterIds.add(master.getId());
            }
        }

        List<RegulationMasterDto> regulationsDto = new ArrayList<>();
        List<DocumentDto> docsDto = new ArrayList<>();
        List<CertificationDto> certsDto = new ArrayList<>();
        List<LabelingDto> labelsDto = new ArrayList<>();
        List<RestrictionDto> restrDto = new ArrayList<>();
        List<ProcedureDto> procsDto = new ArrayList<>();
        List<SourceDto> sourcesDto = new ArrayList<>();

        double totalConfidence = 0.0;
        int confCount = 0;

        for (Long masterId : matchedMasterIds) {
            Optional<RegulationMasterEntity> mOpt = masterRepository.findById(masterId);
            if (mOpt.isPresent()) {
                RegulationMasterEntity m = mOpt.get();
                regulationsDto.add(RegulationMasterDto.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .authority(m.getAuthority())
                        .regulationType(m.getRegulationType())
                        .summary(m.getSummary())
                        .sourceUrl(m.getSourceUrl())
                        .confidenceScore(m.getConfidenceScore() != null ? m.getConfidenceScore() : 0.95)
                        .build());

                if (m.getConfidenceScore() != null) {
                    totalConfidence += m.getConfidenceScore();
                    confCount++;
                }

                List<RegulationDocumentEntity> docs = documentRepository.findByRegulationId(masterId);
                for (RegulationDocumentEntity d : docs) {
                    docsDto.add(new DocumentDto(d.getDocumentName(), d.getMandatory(), d.getRemarks()));
                }

                List<RegulationCertificationEntity> certs = certificationRepository.findByRegulationId(masterId);
                for (RegulationCertificationEntity c : certs) {
                    certsDto.add(new CertificationDto(c.getCertificationName(), c.getMandatory(), c.getRemarks()));
                }

                List<RegulationLabelingEntity> labels = labelingRepository.findByRegulationId(masterId);
                for (RegulationLabelingEntity l : labels) {
                    labelsDto.add(new LabelingDto(l.getRequirement(), l.getRemarks()));
                }

                List<RegulationRestrictionEntity> restr = restrictionRepository.findByRegulationId(masterId);
                for (RegulationRestrictionEntity r : restr) {
                    restrDto.add(new RestrictionDto(r.getRestrictionType(), r.getDescription(), r.getRemarks()));
                }

                List<RegulationProcedureEntity> procs = procedureRepository.findByRegulationId(masterId);
                for (RegulationProcedureEntity p : procs) {
                    procsDto.add(new ProcedureDto(p.getProcedureName(), p.getDescription(), p.getStepOrder()));
                }
            }
        }

        List<RegulationSourceEntity> sources = sourceRepository.findByCountry(country);
        for (RegulationSourceEntity s : sources) {
            sourcesDto.add(new SourceDto(s.getAuthority(), s.getTitle(), s.getSourceUrl(), s.getFormat()));
        }

        double overallConfidence = confCount > 0 ? (totalConfidence / confCount) : 0.95;

        return ComplianceCheckResponseDto.builder()
                .country(country)
                .hsCode(cleanHs)
                .product(product)
                .regulations(regulationsDto)
                .documents(docsDto)
                .certifications(certsDto)
                .labeling(labelsDto)
                .restrictions(restrDto)
                .procedures(procsDto)
                .sources(sourcesDto)
                .confidence(Math.round(overallConfidence * 100.0) / 100.0)
                .build();
    }
}
