package com.trade.dto.market;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for the market analysis endpoint.
 * All data is sourced from the Compliance table — no AI involved.
 *
 * This response structure is intentionally compatible with a future RAG
 * enrichment step; AI/LLM layers can augment the fields without changing this DTO.
 */
@Data
@Builder
public class MarketAnalysisResponse {

    // Destination country info
    private Long countryId;
    private String countryName;
    private String currency;

    // Product info
    private Long productId;
    private String productName;
    private String hsCode;

    // Product category
    private Long categoryId;
    private String categoryName;

    // Compliance data (from Compliance table)
    private BigDecimal customsDuty;
    private String requiredCertificates;
    private String requiredDocuments;
    private String restrictedItems;
    private String transitTime;
    private String recommendedPort;

    // Compliance record availability flag
    private boolean complianceDataAvailable;
}
