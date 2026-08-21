package com.trade.regulatory.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Serves export-destination ranking predictions from the WINNING v4 model
 * (XGBRanker rank:pairwise for ordering + XGBRegressor for the calibrated export
 * value), replacing the previous hand-crafted heuristic opportunity score.
 *
 * Predictions are precomputed offline (see 05(data)/src/generate_serving_table_v4.py)
 * from the most recent feature year and shipped as a classpath resource. This keeps
 * the JVM free of native ML dependencies while serving genuine model outputs.
 *
 * Scope: the model's 55 HS6 products x 10 destination countries. Outside that scope
 * the service returns DATA_UNAVAILABLE — it NEVER fabricates a prediction.
 */
@Service
public class MlExportRankingService {

    private static final Logger log = LoggerFactory.getLogger(MlExportRankingService.class);
    private static final String RESOURCE = "ml/ml_export_rankings_v4.csv";

    /** Prediction row for a (hs6, iso3) pair. */
    public static class MlPrediction {
        public final String hs6;
        public final String iso3;
        public final int modelRank;
        public final double opportunityScore;   // 0..100 within product
        public final double rankerScore;         // ordinal ranking score
        public final double predictedExportUsd;  // calibrated (regressor)
        public final String modelName;

        MlPrediction(String hs6, String iso3, int modelRank, double opportunityScore,
                     double rankerScore, double predictedExportUsd, String modelName) {
            this.hs6 = hs6; this.iso3 = iso3; this.modelRank = modelRank;
            this.opportunityScore = opportunityScore; this.rankerScore = rankerScore;
            this.predictedExportUsd = predictedExportUsd; this.modelName = modelName;
        }
    }

    private final Map<String, MlPrediction> table = new HashMap<>();
    private final Map<String, Integer> productCountryCount = new HashMap<>();
    private String modelName = "XGBRanker v4";

    /** Country name / alias (lower-case) -> ISO3, covering app + dataset naming. */
    private static final Map<String, String> NAME_TO_ISO3 = new HashMap<>();
    static {
        put("USA", "usa", "us", "united states", "united states of america", "america");
        put("DEU", "germany", "deutschland", "de");
        put("NLD", "netherlands", "the netherlands", "holland", "nl");
        put("GBR", "united kingdom", "uk", "great britain", "britain", "england", "gb");
        put("ARE", "united arab emirates", "uae", "u.a.e.", "emirates", "ae");
        put("HKG", "hong kong", "china, hong kong sar", "hong kong sar", "hk");
        put("CHN", "china", "people's republic of china", "cn");
        put("SAU", "saudi arabia", "ksa", "sa");
        put("SGP", "singapore", "sg");
        put("BGD", "bangladesh", "bd");
        // App countries outside the model's scope — mapped so we can report DATA_UNAVAILABLE cleanly.
        put("AUS", "australia", "au");
        put("CAN", "canada", "ca");
        put("JPN", "japan", "jp");
        put("KOR", "south korea", "korea", "republic of korea", "kr");
    }
    private static void put(String iso3, String... names) {
        for (String n : names) NAME_TO_ISO3.put(n, iso3);
    }

    @PostConstruct
    public void load() {
        try {
            ClassPathResource res = new ClassPathResource(RESOURCE);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String header = br.readLine(); // hs6,destination_iso3,model_rank,opportunity_score,ranker_score,predicted_export_usd,model_name
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] p = line.split(",");
                    if (p.length < 7) continue;
                    String hs6 = p[0].trim();
                    String iso3 = p[1].trim().toUpperCase();
                    int rank = (int) Double.parseDouble(p[2].trim());
                    double opp = Double.parseDouble(p[3].trim());
                    double rankerScore = Double.parseDouble(p[4].trim());
                    double usd = Double.parseDouble(p[5].trim());
                    String mname = p[6].trim();
                    this.modelName = mname;
                    table.put(key(hs6, iso3),
                            new MlPrediction(hs6, iso3, rank, opp, rankerScore, usd, mname));
                    productCountryCount.merge(hs6, 1, Integer::sum);
                }
            }
            log.info("MlExportRankingService loaded {} predictions across {} products (model: {})",
                    table.size(), productCountryCount.size(), modelName);
        } catch (Exception e) {
            log.error("Failed to load ML ranking table '{}': {} — ML predictions will be DATA_UNAVAILABLE",
                    RESOURCE, e.getMessage());
        }
    }

    public boolean isLoaded() {
        return !table.isEmpty();
    }

    public String getModelName() {
        return modelName;
    }

    public int size() {
        return table.size();
    }

    /** Convert an HS national code (any length) to its HS6 stem, digits only. */
    public static String toHs6(String hsCode) {
        if (hsCode == null) return null;
        String digits = hsCode.replaceAll("[^0-9]", "");
        if (digits.length() < 6) return null;
        return String.valueOf(Long.parseLong(digits.substring(0, 6))); // strip leading zeros to match CSV int form
    }

    public static String toIso3(String countryNameOrIso) {
        if (countryNameOrIso == null) return null;
        String c = countryNameOrIso.trim();
        if (c.length() == 3 && c.chars().allMatch(Character::isLetter)) {
            return c.toUpperCase();
        }
        return NAME_TO_ISO3.get(c.toLowerCase());
    }

    private static String key(String hs6, String iso3) {
        return hs6 + "|" + iso3.toUpperCase();
    }

    /** Look up the model prediction for a product + destination. */
    public Optional<MlPrediction> lookup(String hsCode, String countryNameOrIso) {
        String hs6 = toHs6(hsCode);
        String iso3 = toIso3(countryNameOrIso);
        if (hs6 == null || iso3 == null) return Optional.empty();
        return Optional.ofNullable(table.get(key(hs6, iso3)));
    }

    /**
     * Build the API-facing ML prediction block for a single destination.
     * Returns DATA_UNAVAILABLE (never fabricated) when the pair is out of model scope.
     */
    public Map<String, Object> predictBlock(String hsCode, String countryNameOrIso) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<MlPrediction> opt = lookup(hsCode, countryNameOrIso);
        if (opt.isEmpty()) {
            String hs6 = toHs6(hsCode);
            String iso3 = toIso3(countryNameOrIso);
            out.put("available", false);
            out.put("status", "DATA_UNAVAILABLE");
            out.put("model", modelName);
            out.put("reason", (iso3 == null)
                    ? "Destination not recognized for the ML model."
                    : (hs6 == null)
                        ? "HS code cannot be reduced to an HS6 product."
                        : "This product/destination is outside the ML model's trained scope "
                          + "(55 HS6 products x 10 destinations). No prediction is fabricated.");
            return out;
        }
        MlPrediction m = opt.get();
        out.put("available", true);
        out.put("status", "OK");
        out.put("model", m.modelName);
        out.put("hs6", m.hs6);
        out.put("destinationIso3", m.iso3);
        out.put("opportunityScore", Math.round(m.opportunityScore));   // 0..100
        out.put("modelRank", m.modelRank);                              // 1 = best destination for this product
        out.put("predictedExportValueUsd", m.predictedExportUsd);      // calibrated (regressor)
        out.put("rankerScore", m.rankerScore);
        out.put("note", "Ranking from XGBRanker (Spearman rho 0.896 on 2023->2024 holdout); "
                + "predicted value from XGBRegressor. Leakage-audited feature set (v4).");
        return out;
    }
}
