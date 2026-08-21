package com.cbec.ai.pipeline.regulation.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class ComplianceComparisonService {

    private final ComplianceRetrievalService retrievalService;
    private final ComplianceScoringService scoringService;

    public ComplianceComparisonService(
            ComplianceRetrievalService retrievalService,
            ComplianceScoringService scoringService) {
        this.retrievalService = retrievalService;
        this.scoringService = scoringService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareRequestDto {
        private String hsCode;
        private List<String> countries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryComparisonItemDto {
        private Integer rank;
        private String country;
        private Integer complianceScore;
        private String complexityLevel;
        private Integer documentCount;
        private Integer certificationCount;
        private Integer restrictionCount;
        private Integer labelingCount;
        private Integer procedureCount;
        private Double hsMatchConfidence;
        private Integer evidenceCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareResponseDto {
        private String hsCode;
        private Integer countriesCompared;
        private List<CountryComparisonItemDto> rankings;
    }

    public CompareResponseDto compareCountries(CompareRequestDto request) {
        String hsCode = request.getHsCode();
        List<String> targetCountries = request.getCountries();

        if (targetCountries == null || targetCountries.isEmpty()) {
            targetCountries = List.of("United States", "United Kingdom", "Germany", "Japan", "Australia");
        }

        log.info("Executing objective multi-country compliance comparison for HS Code '{}' across {} markets", hsCode, targetCountries.size());

        List<CountryComparisonItemDto> items = new ArrayList<>();

        for (String country : targetCountries) {
            ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
            ComplianceScoringService.ScoringResultDto scoring = scoringService.calculateComplianceScore(bundle);

            items.add(CountryComparisonItemDto.builder()
                    .country(country)
                    .complianceScore(scoring.getScore())
                    .complexityLevel(scoring.getLevel())
                    .documentCount(bundle.getDocuments() != null ? bundle.getDocuments().size() : 0)
                    .certificationCount(bundle.getCertifications() != null ? bundle.getCertifications().size() : 0)
                    .restrictionCount(bundle.getRestrictions() != null ? bundle.getRestrictions().size() : 0)
                    .labelingCount(bundle.getLabelingRequirements() != null ? bundle.getLabelingRequirements().size() : 0)
                    .procedureCount(bundle.getProcedures() != null ? bundle.getProcedures().size() : 0)
                    .hsMatchConfidence(bundle.getHsMatch() != null ? bundle.getHsMatch().getConfidence() : 0.90)
                    .evidenceCount(bundle.getEvidence() != null ? bundle.getEvidence().size() : 0)
                    .build());
        }

        // Rank countries objectively by compliance score (lowest complexity first)
        items.sort(Comparator.comparingInt(CountryComparisonItemDto::getComplianceScore));

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }

        return CompareResponseDto.builder()
                .hsCode(hsCode)
                .countriesCompared(items.size())
                .rankings(items)
                .build();
    }
}
