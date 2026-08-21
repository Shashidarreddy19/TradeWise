package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.*;
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
public class ComplianceScoringService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactorDto {
        private String category;
        private String description;
        private Integer count;
        private Integer weightPerItem;
        private Integer totalPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringResultDto {
        private Integer score;
        private String level; // LOW, MEDIUM, HIGH, VERY_HIGH
        private List<FactorDto> factors;
    }

    public ScoringResultDto calculateComplianceScore(ComplianceRetrievalService.ComplianceDataBundleDto bundle) {
        List<FactorDto> factors = new ArrayList<>();

        int docCount = bundle.getDocuments() != null ? bundle.getDocuments().size() : 0;
        int certCount = bundle.getCertifications() != null ? bundle.getCertifications().size() : 0;
        int labelCount = bundle.getLabelingRequirements() != null ? bundle.getLabelingRequirements().size() : 0;
        int restrCount = bundle.getRestrictions() != null ? bundle.getRestrictions().size() : 0;
        int procCount = bundle.getProcedures() != null ? bundle.getProcedures().size() : 0;

        int docWeight = 10;
        int certWeight = 15;
        int restrWeight = 20;
        int labelWeight = 8;
        int procWeight = 5;

        int docPts = docCount * docWeight;
        int certPts = certCount * certWeight;
        int restrPts = restrCount * restrWeight;
        int labelPts = labelCount * labelWeight;
        int procPts = procCount * procWeight;

        factors.add(FactorDto.builder()
                .category("MANDATORY_DOCUMENTS")
                .description(docCount + " mandatory import declaration & filing document(s) required")
                .count(docCount)
                .weightPerItem(docWeight)
                .totalPoints(docPts)
                .build());

        factors.add(FactorDto.builder()
                .category("CERTIFICATIONS")
                .description(certCount + " official compliance & quality certification(s) required")
                .count(certCount)
                .weightPerItem(certWeight)
                .totalPoints(certPts)
                .build());

        factors.add(FactorDto.builder()
                .category("RESTRICTIONS")
                .description(restrCount + " trade restriction & regulatory control measure(s) enforced")
                .count(restrCount)
                .weightPerItem(restrWeight)
                .totalPoints(restrPts)
                .build());

        factors.add(FactorDto.builder()
                .category("LABELING")
                .description(labelCount + " labeling & packaging standard requirement(s) applicable")
                .count(labelCount)
                .weightPerItem(labelWeight)
                .totalPoints(labelPts)
                .build());

        factors.add(FactorDto.builder()
                .category("PROCEDURES")
                .description(procCount + " sequential customs clearance procedure step(s) mandated")
                .count(procCount)
                .weightPerItem(procWeight)
                .totalPoints(procPts)
                .build());

        int rawScore = docPts + certPts + restrPts + labelPts + procPts;
        int normalizedScore = Math.min(100, Math.max(0, rawScore));

        String level = "LOW";
        if (normalizedScore >= 90) {
            level = "VERY_HIGH";
        } else if (normalizedScore >= 70) {
            level = "HIGH";
        } else if (normalizedScore >= 40) {
            level = "MEDIUM";
        }

        return ScoringResultDto.builder()
                .score(normalizedScore)
                .level(level)
                .factors(factors)
                .build();
    }
}
