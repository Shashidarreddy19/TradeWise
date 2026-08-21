package com.cbec.ai.pipeline.intelligence.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CounterfactualAnalysisService {

    private final ExportGuideService exportGuideService;
    private final ComplianceComplexityService complexityService;

    public CounterfactualAnalysisService(
            ExportGuideService exportGuideService,
            ComplianceComplexityService complexityService) {
        this.exportGuideService = exportGuideService;
        this.complexityService = complexityService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CounterfactualAnalysisDto {
        private String currentCountry;
        private String alternativeCountry;
        private String hsCode;
        private String tariffDifference;
        private List<String> extraDocuments;
        private List<String> extraCertificates;
        private List<String> extraRestrictions;
        private int complianceScoreDifference;
        private String complianceComparison;
        private String recommendation;
    }

    public CounterfactualAnalysisDto analyzeCounterfactual(String currentCountry, String alternativeCountry, String hsCode) {
        log.info("Performing counterfactual analysis: Current='{}', Alternative='{}', HS Code='{}'",
                currentCountry, alternativeCountry, hsCode);

        ExportGuideService.ExportGuideDto currentGuide = exportGuideService.getExportGuide(currentCountry, hsCode);
        ExportGuideService.ExportGuideDto altGuide = exportGuideService.getExportGuide(alternativeCountry, hsCode);

        ComplianceComplexityService.ComplexityReportDto currentComplexity = complexityService.getComplexity(currentCountry, hsCode);
        ComplianceComplexityService.ComplexityReportDto altComplexity = complexityService.getComplexity(alternativeCountry, hsCode);

        String currentDutyStr = currentGuide.getTariff().getOrDefault("duty", "5%");
        String altDutyStr = altGuide.getTariff().getOrDefault("duty", "5%");

        double currentDuty = parseDuty(currentDutyStr);
        double altDuty = parseDuty(altDutyStr);
        double dutyDiff = altDuty - currentDuty;

        String tariffDiffStr = String.format("Alternative market duty is %+.1f%% relative to %s (%s vs %s)",
                dutyDiff, currentCountry, altDutyStr, currentDutyStr);

        List<String> extraDocs = new ArrayList<>(altGuide.getDocuments());
        extraDocs.removeAll(currentGuide.getDocuments());

        List<String> extraCerts = new ArrayList<>(altGuide.getCertifications());
        extraCerts.removeAll(currentGuide.getCertifications());

        List<String> extraRestr = new ArrayList<>(altGuide.getRestrictions());
        extraRestr.removeAll(currentGuide.getRestrictions());

        int scoreDiff = altComplexity.getScore() - currentComplexity.getScore();
        String scoreComp = String.format("%s compliance score is %d vs %s score of %d (%+d point delta)",
                alternativeCountry, altComplexity.getScore(), currentCountry, currentComplexity.getScore(), scoreDiff);

        String recommendation;
        if (scoreDiff < 0 && dutyDiff <= 0) {
            recommendation = String.format("Switching to %s is RECOMMENDED due to lower tariff duties and reduced compliance complexity.", alternativeCountry);
        } else if (scoreDiff > 0) {
            recommendation = String.format("Remaining in %s is RECOMMENDED as %s imposes %d additional compliance points and extra trade barriers.",
                    currentCountry, alternativeCountry, scoreDiff);
        } else {
            recommendation = String.format("Both markets offer comparable regulatory complexity and tariff structures.", alternativeCountry);
        }

        return CounterfactualAnalysisDto.builder()
                .currentCountry(currentCountry)
                .alternativeCountry(alternativeCountry)
                .hsCode(hsCode)
                .tariffDifference(tariffDiffStr)
                .extraDocuments(extraDocs)
                .extraCertificates(extraCerts)
                .extraRestrictions(extraRestr)
                .complianceScoreDifference(scoreDiff)
                .complianceComparison(scoreComp)
                .recommendation(recommendation)
                .build();
    }

    private double parseDuty(String dutyStr) {
        if (dutyStr == null) return 5.0;
        try {
            String clean = dutyStr.replaceAll("[^0-9.]", "");
            return clean.isEmpty() ? 5.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 5.0;
        }
    }
}
