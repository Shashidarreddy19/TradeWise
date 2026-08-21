package com.cbec.ai.pipeline.regulation.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ComplianceGuidanceService {

    private final ComplianceRetrievalService retrievalService;

    public ComplianceGuidanceService(ComplianceRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuidanceStepDto {
        private Integer stepNumber;
        private String stepName;
        private String description;
        private List<String> evidenceCitations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportGuidanceResponseDto {
        private String country;
        private String hsCode;
        private String productDescription;
        private Integer totalSteps;
        private List<GuidanceStepDto> workflowSteps;
    }

    public ExportGuidanceResponseDto generateExportGuidance(String country, String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        List<GuidanceStepDto> steps = new ArrayList<>();

        // Step 1: Product Classification
        List<String> s1Citations = List.of("HS Classification Code: " + bundle.getHsCode() + " (" + bundle.getHsMatch().getMatchType() + ")");
        steps.add(GuidanceStepDto.builder()
                .stepNumber(1)
                .stepName("Product Classification & Tariff Determination")
                .description("Verify national tariff code " + bundle.getHsCode() + " under " + country + " Customs Nomenclature: " + bundle.getProductDescription())
                .evidenceCitations(s1Citations)
                .build());

        // Step 2: Import Eligibility & Restrictions
        if (bundle.getRestrictions() != null && !bundle.getRestrictions().isEmpty()) {
            List<String> s2Citations = bundle.getRestrictions().stream()
                    .map(r -> r.getRestrictionType() + ": " + r.getDescription())
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(2)
                    .stepName("Import Eligibility & Restriction Check")
                    .description("Comply with " + bundle.getRestrictions().size() + " active trade restrictions & licensing rules prior to dispatch.")
                    .evidenceCitations(s2Citations)
                    .build());
        }

        // Step 3: Required Registrations
        if (bundle.getSources() != null && !bundle.getSources().isEmpty()) {
            List<String> s3Citations = bundle.getSources().stream()
                    .map(s -> s.getAuthority() + " (" + s.getSourceUrl() + ")")
                    .distinct()
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(3)
                    .stepName("Importer/Exporter Regulatory Authority Registration")
                    .description("Register with official regulatory authorities governing imports in " + country)
                    .evidenceCitations(s3Citations)
                    .build());
        }

        // Step 4: Required Documents
        if (bundle.getDocuments() != null && !bundle.getDocuments().isEmpty()) {
            List<String> s4Citations = bundle.getDocuments().stream()
                    .map(d -> d.getDocumentName() + " (Mandatory: " + d.getMandatory() + ")")
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(4)
                    .stepName("Commercial & Customs Document Preparation")
                    .description("Assemble " + bundle.getDocuments().size() + " mandatory commercial documents including Bill of Lading, Commercial Invoice & Customs Filing Form.")
                    .evidenceCitations(s4Citations)
                    .build());
        }

        // Step 5: Required Certifications
        if (bundle.getCertifications() != null && !bundle.getCertifications().isEmpty()) {
            List<String> s5Citations = bundle.getCertifications().stream()
                    .map(c -> c.getCertificationName() + " (Mandatory: " + c.getMandatory() + ")")
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(5)
                    .stepName("Conformity & Quality Certification Filing")
                    .description("Obtain " + bundle.getCertifications().size() + " mandatory conformity certificates from authorized testing bodies.")
                    .evidenceCitations(s5Citations)
                    .build());
        }

        // Step 6: Labeling Requirements
        if (bundle.getLabelingRequirements() != null && !bundle.getLabelingRequirements().isEmpty()) {
            List<String> s6Citations = bundle.getLabelingRequirements().stream()
                    .map(l -> l.getRequirement())
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(6)
                    .stepName("Product Labeling Standard Compliance")
                    .description("Apply mandatory consumer & regulatory labels in destination country language.")
                    .evidenceCitations(s6Citations)
                    .build());
        }

        // Step 7: Packaging Requirements
        steps.add(GuidanceStepDto.builder()
                .stepNumber(7)
                .stepName("ISPM 15 Packaging & Container Marking")
                .description("Ensure wood packaging materials are heat-treated or fumigated with ISPM 15 stamp.")
                .evidenceCitations(List.of(country + " Phytosanitary & Packaging Import Protocol"))
                .build());

        // Step 8: Customs Declaration
        steps.add(GuidanceStepDto.builder()
                .stepNumber(8)
                .stepName("Electronic Customs Declaration & Tariff Payment")
                .description("File Single Administrative Document / Entry Summary Declaration with " + country + " Customs.")
                .evidenceCitations(List.of(country + " Customs Electronic Entry Guidelines"))
                .build());

        // Step 9: Inspection Requirements
        if (bundle.getProcedures() != null && !bundle.getProcedures().isEmpty()) {
            List<String> s9Citations = bundle.getProcedures().stream()
                    .map(p -> p.getProcedureName() + ": " + p.getDescription())
                    .toList();
            steps.add(GuidanceStepDto.builder()
                    .stepNumber(9)
                    .stepName("Physical & Quarantine Border Inspection")
                    .description("Undergo scheduled border inspection steps as mandated by " + country + " authorities.")
                    .evidenceCitations(s9Citations)
                    .build());
        }

        // Step 10: Final Clearance
        steps.add(GuidanceStepDto.builder()
                .stepNumber(10)
                .stepName("Final Customs Release & Market Entry")
                .description("Receive official Out-of-Charge / Customs Release Order for free circulation in " + country + ".")
                .evidenceCitations(List.of(country + " Port Customs Final Release Authorization"))
                .build());

        return ExportGuidanceResponseDto.builder()
                .country(country)
                .hsCode(bundle.getHsCode())
                .productDescription(bundle.getProductDescription())
                .totalSteps(steps.size())
                .workflowSteps(steps)
                .build();
    }
}
