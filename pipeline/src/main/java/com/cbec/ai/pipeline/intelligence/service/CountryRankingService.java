package com.cbec.ai.pipeline.intelligence.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class CountryRankingService {

    private final CountryComparisonService comparisonService;

    public CountryRankingService(CountryComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryRankDto {
        private int rank;
        private String country;
        private double dutyRateNumeric;
        private int complianceScore;
        private String complianceLevel;
        private int totalBarriers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryRankingResponseDto {
        private String hsCode;
        private List<CountryRankDto> rankings;
        private String topRankedCountry;
    }

    public CountryRankingResponseDto rankCountries(String hsCode, List<String> candidateCountries) {
        log.info("Ranking candidate countries for HS Code: {}", hsCode);

        CountryComparisonService.CountryComparisonResponseDto comparison = comparisonService.compareCountries(hsCode, candidateCountries);
        List<CountryRankDto> rankList = new ArrayList<>();

        for (CountryComparisonService.CountryComparisonItemDto item : comparison.getComparison()) {
            double duty = parseDutyRate(item.getTariff().get("duty"));
            int score = item.getComplianceComplexity() != null ? item.getComplianceComplexity().getScore() : 50;
            String level = item.getComplianceComplexity() != null ? item.getComplianceComplexity().getLevel() : "Medium";
            int totalBarriers = item.getDocumentCount() + item.getCertificationCount() + item.getRestrictionCount();

            rankList.add(CountryRankDto.builder()
                    .country(item.getCountry())
                    .dutyRateNumeric(duty)
                    .complianceScore(score)
                    .complianceLevel(level)
                    .totalBarriers(totalBarriers)
                    .build());
        }

        // Sort by Lowest Duty Rate -> Lowest Compliance Score -> Fewest Barriers
        rankList.sort(Comparator.comparingDouble(CountryRankDto::getDutyRateNumeric)
                .thenComparingInt(CountryRankDto::getComplianceScore)
                .thenComparingInt(CountryRankDto::getTotalBarriers));

        int rankNum = 1;
        for (CountryRankDto r : rankList) {
            r.setRank(rankNum++);
        }

        String topCountry = !rankList.isEmpty() ? rankList.getFirst().getCountry() : "UAE";

        return CountryRankingResponseDto.builder()
                .hsCode(hsCode)
                .rankings(rankList)
                .topRankedCountry(topCountry)
                .build();
    }

    private double parseDutyRate(String dutyStr) {
        if (dutyStr == null) return 5.0;
        try {
            String clean = dutyStr.replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return 5.0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 5.0;
        }
    }
}
