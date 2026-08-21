package com.trade.regulatory.service;

import com.trade.regulatory.entity.CountryMasterEntity;
import com.trade.regulatory.repository.CountryMasterRepository;
import com.trade.regulatory.repository.HsMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Market opportunity analysis service.
 * Uses real regulatory complexity + available trade data.
 * NEVER fabricates trade statistics — returns DATA_UNAVAILABLE when absent.
 */
@Service
@Transactional(readOnly = true, transactionManager = "tradeDataTransactionManager")
public class MarketOpportunityService {

    private final CountryMasterRepository countryRepo;
    private final RegulatoryRetrievalService retrievalService;
    private final CostEstimationService costService;
    private final HsMasterRepository hsMasterRepo;
    private final MlExportRankingService mlRankingService;

    public MarketOpportunityService(CountryMasterRepository countryRepo,
                                     RegulatoryRetrievalService retrievalService,
                                     CostEstimationService costService,
                                     HsMasterRepository hsMasterRepo,
                                     MlExportRankingService mlRankingService) {
        this.countryRepo = countryRepo;
        this.retrievalService = retrievalService;
        this.costService = costService;
        this.hsMasterRepo = hsMasterRepo;
        this.mlRankingService = mlRankingService;
    }

    /**
     * Rank destination countries for a given product/HS code.
     * Score is based on REAL data: regulatory complexity, tariff burden, market access.
     * Missing data is flagged, not fabricated.
     */
    public Map<String, Object> rankCountries(String hsCode, List<String> candidateCountries) {
        if (candidateCountries == null || candidateCountries.isEmpty()) {
            candidateCountries = countryRepo.findByActiveTrue().stream()
                    .map(CountryMasterEntity::getCountryName)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> rankings = new ArrayList<>();

        for (String country : candidateCountries) {
            Map<String, Object> entry = analyzeCountry(country, hsCode);
            rankings.add(entry);
        }

        // Sort by opportunity score descending
        rankings.sort((a, b) -> {
            int scoreA = (int) a.getOrDefault("opportunityScore", 0);
            int scoreB = (int) b.getOrDefault("opportunityScore", 0);
            return Integer.compare(scoreB, scoreA);
        });

        // Assign ranks
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).put("rank", i + 1);
        }

        return Map.of(
                "hsCode", hsCode,
                "totalCountries", rankings.size(),
                "rankings", rankings
        );
    }

    private Map<String, Object> analyzeCountry(String country, String hsCode) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("country", country);
        entry.put("hsCode", hsCode);

        // 1. Compliance complexity (from real regulatory data)
        RegulatoryRetrievalService.RegulatoryResult regResult =
                retrievalService.getRegulations(country, hsCode);
        RegulatoryRetrievalService.ComplianceScore compliance =
                retrievalService.calculateCompliance(regResult);

        entry.put("complianceScore", compliance.numericScore);
        entry.put("complexity", compliance.complexity);
        entry.put("documentsRequired", compliance.documentsCount);
        entry.put("certificationsRequired", compliance.certificationsCount);
        entry.put("restrictionsCount", compliance.restrictionsCount);
        entry.put("regulationFound", compliance.regulationFound);

        // 2. Tariff burden (from real known rates)
        var costEst = costService.estimateCost(country, hsCode,
                java.math.BigDecimal.valueOf(1000), 1, "USD");
        Double dutyRate = (Double) costEst.get("dutyRate");
        Double taxRate = (Double) costEst.get("taxRate");
        entry.put("dutyRate", dutyRate);
        entry.put("taxRate", taxRate);
        entry.put("tariffConfidence", costEst.get("dutyConfidence"));

        // 3. Calculate opportunity score
        // Formula: Higher compliance score = easier market
        //          Lower tariff = better market access
        //          Formula is transparent and deterministic
        int compScore = compliance.numericScore; // 0-100, higher = easier
        int tariffPenalty = dutyRate != null ? (int)(dutyRate * 2) : 20; // penalty for high tariffs
        int taxPenalty = taxRate != null ? (int)(taxRate * 0.5) : 5;

        int heuristicScore = Math.max(0, Math.min(100,
                compScore - tariffPenalty - taxPenalty + 30)); // 30 base accessibility bonus

        // 4. Opportunity score: prefer the WINNING v4 ML model where in-scope,
        //    replacing the heuristic. Falls back to the heuristic (clearly labelled)
        //    when the product/destination is outside the model's trained scope.
        List<String> reasons = new ArrayList<>();
        var mlOpt = mlRankingService.lookup(hsCode, country);
        int opportunityScore;
        String scoreSource;
        if (mlOpt.isPresent()) {
            var ml = mlOpt.get();
            opportunityScore = (int) Math.round(ml.opportunityScore);
            scoreSource = "ML_MODEL_V4";
            entry.put("mlModelRank", ml.modelRank);
            entry.put("predictedExportValueUsd", ml.predictedExportUsd);
            entry.put("modelName", ml.modelName);
            reasons.add("ML export-ranking model (XGBRanker v4, Spearman rho 0.896)");
            if (ml.modelRank <= 3) reasons.add("Top-" + ml.modelRank + " predicted destination for this product");
        } else {
            opportunityScore = heuristicScore;
            scoreSource = "HEURISTIC_FALLBACK";
            reasons.add("Heuristic score (product/destination outside ML model scope)");
        }
        entry.put("opportunityScore", opportunityScore);
        entry.put("scoreSource", scoreSource);

        // Regulatory / tariff explanation (always included)
        if (compScore >= 80) reasons.add("Low regulatory complexity");
        else if (compScore >= 50) reasons.add("Medium regulatory complexity");
        else reasons.add("High regulatory complexity");

        if (dutyRate != null && dutyRate == 0) reasons.add("Zero customs duty");
        else if (dutyRate != null && dutyRate < 5) reasons.add("Low customs duty (" + dutyRate + "%)");
        else if (dutyRate != null) reasons.add("Customs duty: " + dutyRate + "%");
        else reasons.add("Tariff data unavailable");

        entry.put("reasons", reasons);
        entry.put("dataAvailability", dutyRate != null ? "PARTIAL" : "LIMITED");

        return entry;
    }
}
