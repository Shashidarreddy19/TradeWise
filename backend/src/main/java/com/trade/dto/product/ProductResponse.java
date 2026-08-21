package com.trade.dto.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for a product record.
 * Exposes category and exporter as named fields — no entity leakage.
 */
@Data
@Builder
public class ProductResponse {

    private Long id;
    private Long exporterId;
    private String exporterName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String hsCode;
    private String description;
    private String material;
    private String composition;
    private String function;
    private String manufacturingProcess;
    private String physicalForm;
    private String specifications;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal weight;
}
