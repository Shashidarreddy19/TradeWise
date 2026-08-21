package com.trade.dto.category;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a product category record.
 */
@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String categoryName;
}
