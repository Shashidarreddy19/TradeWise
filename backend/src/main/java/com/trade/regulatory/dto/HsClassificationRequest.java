package com.trade.regulatory.dto;

import lombok.*;

/**
 * Request DTO for AI-assisted HS Code Classification.
 * All fields describe the product to be exported from India.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsClassificationRequest {
    private String productName;
    private String category;
    private String description;
    private String material;
    private String composition;
    /** Alias: function / usage / intended use */
    private String function;
    /** Alias field for 'function' — maps to same classification signal */
    private String usage;
    private String manufacturingProcess;
    private String physicalForm;
    private String specifications;
    /** Alias for specifications */
    private String additionalSpecifications;
    private String productType;
    private String brand;
    private String model;
    @Builder.Default
    private String originCountry = "India";

    /** Get effective function/usage (merges both fields) */
    public String getEffectiveFunction() {
        if (function != null && !function.isBlank()) return function;
        if (usage != null && !usage.isBlank()) return usage;
        return "";
    }

    /** Get effective specifications (merges both fields) */
    public String getEffectiveSpecifications() {
        StringBuilder sb = new StringBuilder();
        if (specifications != null && !specifications.isBlank()) sb.append(specifications);
        if (additionalSpecifications != null && !additionalSpecifications.isBlank()) {
            if (sb.length() > 0) sb.append(". ");
            sb.append(additionalSpecifications);
        }
        return sb.toString();
    }
}
