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
public class ExplainableAiService {

    private final CountryRankingService rankingService;
    private final CountryComparisonService comparisonService;

    public ExplainableAiService(CountryRankingService rankingService, CountryComparisonService comparisonService) {
        this.rankingService = rankingService;
        this.comparisonService = comparisonService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExplainableAiResponseDto {
        private String targetCountry;
        private String hsCode;
        private int rank;
        private String question;
        private List<String> primaryDrivers;
        private Map<String, Object> comparativeMetrics;
        private String detailedExplanation;
    }

    public ExplainableAiResponseDto explainRanking(String targetCountry, String hsCode, List<String> candidateCountries) {
        log.info("Generating Explainable AI rationale for country '{}', HS Code '{}'", targetCountry, hsCode);

        CountryRankingService.CountryRankingResponseDto ranking = rankingService.rankCountries(hsCode, candidateCountries);
        CountryComparisonService.CountryComparisonResponseDto comparison = comparisonService.compareCountries(hsCode, candidateCountries);

        int rank = 1;
        for (CountryRankingService.CountryRankDto r : ranking.getRankings()) {
            if (r.getCountry().equalsIgnoreCase(targetCountry)) {
                rank = r.getRank();
                break;
            }
        }

        List<String> drivers = new ArrayList<>();
        drivers.add("Competitive Tariff Duty Rate structure");
        drivers.add("Streamlined documentary & certification requirements");
        drivers.add("Lower overall Compliance Complexity Index score");
        drivers.add("Efficient customs clearance procedures");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("rank", rank);
        metrics.put("totalCandidateCountries", candidateCountries.size());

        String explanation = String.format(
                "Country '%s' is ranked #%d among candidate markets for HS Code %s due to favorable customs tariffs, " +
                "a lower compliance risk score, and fewer mandatory non-tariff barriers compared to alternative jurisdictions.",
                targetCountry, rank, hsCode);

        return ExplainableAiResponseDto.builder()
                .targetCountry(targetCountry)
                .hsCode(hsCode)
                .rank(rank)
                .question("Why is " + targetCountry + " ranked #" + rank + "?")
                .primaryDrivers(drivers)
                .comparativeMetrics(metrics)
                .detailedExplanation(explanation)
                .build();
    }
}
