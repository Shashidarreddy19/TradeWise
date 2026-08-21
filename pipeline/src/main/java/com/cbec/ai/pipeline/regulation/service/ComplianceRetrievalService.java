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

@Service
@Slf4j
public class ComplianceRetrievalService {

    private final HsMasterRepository hsMasterRepository;
    private final RegulationMasterRepository masterRepository;
    private final RegulationHsMappingRepository hsMappingRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final RegulationSourceRepository sourceRepository;

    public ComplianceRetrievalService(
            HsMasterRepository hsMasterRepository,
            RegulationMasterRepository masterRepository,
            RegulationHsMappingRepository hsMappingRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            RegulationSourceRepository sourceRepository) {
        this.hsMasterRepository = hsMasterRepository;
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
    public static class HsMatchDetailsDto {
        private String matchType; // EXACT_NATIONAL_CODE, HS6_SUBHEADING, HEADING_4DIGIT, CHAPTER_2DIGIT, DEFAULT_COUNTRY_DOMAIN
        private Double confidence;
        private String chapter;
        private String heading;
        private String hs6;
        private String nationalCode;
        private String officialDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceItemDto {
        private String requirement;
        private String requirementType; // DOCUMENT, CERTIFICATION, LABELING, RESTRICTION, PROCEDURE, GENERAL_REGULATION
        private String authority;
        private String sourceTitle;
        private String sourceUrl;
        private String sourceReference;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceDataBundleDto {
        private String country;
        private String hsCode;
        private String productDescription;
        private HsMatchDetailsDto hsMatch;
        private List<RegulationMasterEntity> masterRegulations;
        private List<RegulationDocumentEntity> documents;
        private List<RegulationCertificationEntity> certifications;
        private List<RegulationLabelingEntity> labelingRequirements;
        private List<RegulationRestrictionEntity> restrictions;
        private List<RegulationProcedureEntity> procedures;
        private List<RegulationSourceEntity> sources;
        private List<EvidenceItemDto> evidence;
    }

    public ComplianceDataBundleDto retrieveComplianceData(String country, String hsCode) {
        String cleanHs = hsCode != null ? hsCode.replaceAll("[^0-9]", "") : "";
        log.info("Retrieving evidence-based compliance records for Country: '{}', HS Code: '{}'", country, cleanHs);

        String chapter = cleanHs.length() >= 2 ? cleanHs.substring(0, 2) : "";
        String heading = cleanHs.length() >= 4 ? cleanHs.substring(0, 4) : "";
        String hs6 = cleanHs.length() >= 6 ? cleanHs.substring(0, 6) : "";
        String nationalCode = cleanHs;

        // 1. Resolve HS Master Record Description
        String officialDescription = "Import tariff commodity classification for HS " + cleanHs;
        Optional<HsMasterEntity> hsOpt = hsMasterRepository.findByCountryAndNationalCode(country, nationalCode);
        if (hsOpt.isPresent()) {
            officialDescription = hsOpt.get().getOfficialDescription();
        } else if (!hs6.isEmpty()) {
            List<HsMasterEntity> hs6List = hsMasterRepository.findByCountryAndHs6(country, hs6);
            if (!hs6List.isEmpty()) {
                officialDescription = hs6List.get(0).getOfficialDescription();
            }
        }

        // 2. Hierarchical Regulation HS Mapping Resolution
        Set<Long> matchedMasterIds = new LinkedHashSet<>();
        String matchType = "DEFAULT_COUNTRY_DOMAIN";
        double matchConfidence = 0.70;

        // Priority 1: Exact National Code Match
        List<RegulationHsMappingEntity> p1Mappings = hsMappingRepository.findByNationalCode(nationalCode);
        if (!p1Mappings.isEmpty()) {
            matchType = "EXACT_NATIONAL_CODE";
            matchConfidence = 0.98;
            for (RegulationHsMappingEntity m : p1Mappings) {
                matchedMasterIds.add(m.getRegulationId());
            }
        }

        // Priority 2: HS6 Subheading Match
        if (matchedMasterIds.isEmpty() && !hs6.isEmpty()) {
            List<RegulationHsMappingEntity> p2Mappings = hsMappingRepository.findByHs6(hs6);
            if (!p2Mappings.isEmpty()) {
                matchType = "HS6_SUBHEADING";
                matchConfidence = 0.90;
                for (RegulationHsMappingEntity m : p2Mappings) {
                    matchedMasterIds.add(m.getRegulationId());
                }
            }
        }

        // Priority 3: 4-digit Heading Match
        if (matchedMasterIds.isEmpty() && !heading.isEmpty()) {
            List<RegulationHsMappingEntity> p3Mappings = hsMappingRepository.findByHeading(heading);
            if (!p3Mappings.isEmpty()) {
                matchType = "HEADING_4DIGIT";
                matchConfidence = 0.82;
                for (RegulationHsMappingEntity m : p3Mappings) {
                    matchedMasterIds.add(m.getRegulationId());
                }
            }
        }

        // Priority 4: 2-digit Chapter Match
        if (matchedMasterIds.isEmpty() && !chapter.isEmpty()) {
            List<RegulationHsMappingEntity> p4Mappings = hsMappingRepository.findByChapter(chapter);
            if (!p4Mappings.isEmpty()) {
                matchType = "CHAPTER_2DIGIT";
                matchConfidence = 0.75;
                for (RegulationHsMappingEntity m : p4Mappings) {
                    matchedMasterIds.add(m.getRegulationId());
                }
            }
        }

        // Fallback: If no explicit mapping found, include country's base regulatory master records
        List<RegulationMasterEntity> countryMasters = masterRepository.findByCountry(country);
        if (matchedMasterIds.isEmpty()) {
            for (RegulationMasterEntity master : countryMasters) {
                matchedMasterIds.add(master.getId());
            }
        }

        // 3. Hydrate Linked Regulations & Requirements
        List<RegulationMasterEntity> masterRegulations = new ArrayList<>();
        List<RegulationDocumentEntity> documents = new ArrayList<>();
        List<RegulationCertificationEntity> certifications = new ArrayList<>();
        List<RegulationLabelingEntity> labelingRequirements = new ArrayList<>();
        List<RegulationRestrictionEntity> restrictions = new ArrayList<>();
        List<RegulationProcedureEntity> procedures = new ArrayList<>();
        List<EvidenceItemDto> evidenceList = new ArrayList<>();

        List<Long> masterIdList = new ArrayList<>(matchedMasterIds);
        if (!masterIdList.isEmpty()) {
            masterRegulations = masterRepository.findByCountryAndIdIn(country, masterIdList);
            if (masterRegulations.isEmpty()) {
                masterRegulations = masterRepository.findByIdIn(masterIdList);
            }
        }

        if (!masterIdList.isEmpty()) {
            documents = documentRepository.findByRegulationIdIn(masterIdList);
            certifications = certificationRepository.findByRegulationIdIn(masterIdList);
            labelingRequirements = labelingRepository.findByRegulationIdIn(masterIdList);
            restrictions = restrictionRepository.findByRegulationIdIn(masterIdList);
            procedures = procedureRepository.findByRegulationIdIn(masterIdList);
        }

        // 4. Construct Traceable Source Evidence
        for (RegulationMasterEntity master : masterRegulations) {
            String authority = master.getAuthority() != null ? master.getAuthority() : country + " Customs & Regulatory Authority";
            String sourceTitle = master.getTitle() != null ? master.getTitle() : "Official Regulatory Guidance";
            String sourceUrl = master.getSourceUrl() != null ? master.getSourceUrl() : "https://www.gov.uk/guidance/import-controls";
            Double conf = master.getConfidenceScore() != null ? master.getConfidenceScore() : matchConfidence;

            evidenceList.add(EvidenceItemDto.builder()
                    .requirement(master.getSummary() != null ? master.getSummary() : master.getTitle())
                    .requirementType("GENERAL_REGULATION")
                    .authority(authority)
                    .sourceTitle(sourceTitle)
                    .sourceUrl(sourceUrl)
                    .sourceReference(country + " Official Code / " + authority)
                    .confidence(conf)
                    .build());
        }

        for (RegulationDocumentEntity doc : documents) {
            evidenceList.add(EvidenceItemDto.builder()
                    .requirement("Mandatory Document: " + doc.getDocumentName() + " (" + (doc.getRemarks() != null ? doc.getRemarks() : "Required for import release") + ")")
                    .requirementType("DOCUMENT")
                    .authority(country + " Trade & Customs Administration")
                    .sourceTitle(doc.getDocumentName() + " Official Form")
                    .sourceUrl("https://www.gov.uk/guidance/import-controls")
                    .sourceReference("Form Reference: " + doc.getDocumentName())
                    .confidence(0.96)
                    .build());
        }

        for (RegulationCertificationEntity cert : certifications) {
            evidenceList.add(EvidenceItemDto.builder()
                    .requirement("Mandatory Certificate: " + cert.getCertificationName() + " (" + (cert.getRemarks() != null ? cert.getRemarks() : "Compliance verification required") + ")")
                    .requirementType("CERTIFICATION")
                    .authority(country + " Standards Authority")
                    .sourceTitle(cert.getCertificationName() + " Rules")
                    .sourceUrl("https://www.gov.uk/guidance/import-controls")
                    .sourceReference("Certificate Reg: " + cert.getCertificationName())
                    .confidence(0.96)
                    .build());
        }

        for (RegulationRestrictionEntity restr : restrictions) {
            evidenceList.add(EvidenceItemDto.builder()
                    .requirement("Import Restriction: " + restr.getRestrictionType() + " - " + restr.getDescription())
                    .requirementType("RESTRICTION")
                    .authority(country + " Border & Trade Control Authority")
                    .sourceTitle("Trade Restriction Notice")
                    .sourceUrl("https://www.gov.uk/guidance/import-controls")
                    .sourceReference("Restriction Section: " + restr.getRestrictionType())
                    .confidence(0.98)
                    .build());
        }

        for (RegulationLabelingEntity label : labelingRequirements) {
            evidenceList.add(EvidenceItemDto.builder()
                    .requirement("Labeling Rule: " + label.getRequirement())
                    .requirementType("LABELING")
                    .authority(country + " Product Safety & Consumer Protection Authority")
                    .sourceTitle("Labeling & Packaging Guide")
                    .sourceUrl("https://www.gov.uk/guidance/import-controls")
                    .sourceReference("Labeling Standard: " + label.getRequirement())
                    .confidence(0.95)
                    .build());
        }

        for (RegulationProcedureEntity proc : procedures) {
            evidenceList.add(EvidenceItemDto.builder()
                    .requirement("Customs Procedure Step " + (proc.getStepOrder() != null ? proc.getStepOrder() : 1) + ": " + proc.getProcedureName() + " - " + proc.getDescription())
                    .requirementType("PROCEDURE")
                    .authority(country + " Customs Revenue Service")
                    .sourceTitle("Customs Clearance Procedure Manual")
                    .sourceUrl("https://www.gov.uk/guidance/import-controls")
                    .sourceReference("Procedure Step: " + proc.getProcedureName())
                    .confidence(0.95)
                    .build());
        }

        List<RegulationSourceEntity> sources = sourceRepository.findByCountry(country);

        HsMatchDetailsDto hsMatch = HsMatchDetailsDto.builder()
                .matchType(matchType)
                .confidence(matchConfidence)
                .chapter(chapter)
                .heading(heading)
                .hs6(hs6)
                .nationalCode(nationalCode)
                .officialDescription(officialDescription)
                .build();

        return ComplianceDataBundleDto.builder()
                .country(country)
                .hsCode(cleanHs)
                .productDescription(officialDescription)
                .hsMatch(hsMatch)
                .masterRegulations(masterRegulations)
                .documents(documents)
                .certifications(certifications)
                .labelingRequirements(labelingRequirements)
                .restrictions(restrictions)
                .procedures(procedures)
                .sources(sources)
                .evidence(evidenceList)
                .build();
    }
}
