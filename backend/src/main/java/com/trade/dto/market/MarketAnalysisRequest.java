package com.trade.dto.market;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for the market analysis endpoint.
 * Accepts a country and a product ID; derives category from the product.
 */
@Data
public class MarketAnalysisRequest {

    @NotNull(message = "Country ID is required")
    private Long countryId;

    @NotNull(message = "Product ID is required")
    private Long productId;
}
