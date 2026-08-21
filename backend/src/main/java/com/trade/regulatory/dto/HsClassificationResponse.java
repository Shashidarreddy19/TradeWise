package com.trade.regulatory.dto;

import lombok.*;
import java.util.List;

/**
 * Response DTO for AI-assisted HS Code Classification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsClassificationResponse {
    private String productName;
    private String originCountry;
    private String classificationStatus; // MATCHED, LOW_CONFIDENCE, NO_CONFIDENT_MATCH, ERROR
    private String classificationMode;   // AI_RANKED, DATABASE_ONLY
    private String recommendedHsCode;
    private double confidenceScore;
    private String confidenceLevel; // HIGH_CONFIDENCE, GOOD_MATCH, POSSIBLE_MATCH, LOW_CONFIDENCE
    private String classificationLevel; // INDIAN_NATIONAL_CODE, HS6, HS4_HEADING, HS2_CHAPTER
    private List<HsCandidate> topCandidates;
    private String classificationExplanation;
    private List<String> warnings;
    /** True if confidence is too low to recommend */
    @Builder.Default
    private boolean needsReview = false;
    /** Source database used */
    @Builder.Default
    private String dataSource = "TradeData.hs_master";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HsCandidate {
        private String hsCode;
        private String officialDescription;
        private double matchScore;
        private int rank;
        private String reason;
        private List<String> matchedAttributes;
        private String chapter;
        private String heading;
        private String hs6;
        private String source;
        private boolean evidenceAvailable;
    }
}
