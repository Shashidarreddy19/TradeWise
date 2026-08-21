package com.trade.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request payload for creating or updating a product.
 */
@Data
public class ProductRequest {

    /**
     * Category ID. Required — every product must belong to a category.
     */
    @NotNull(message = "Please select a product category.")
    private Long categoryId;

    @NotBlank(message = "Product name is required")
    private String name;

    /**
     * HS Code is optional at creation time. Users can create a product first,
     * then use the AI-powered HS classification to assign the code later.
     * When provided, it must match the 4-8 digit format.
     */
    @Pattern(regexp = "^(\\d{4}(\\.?\\d{2}){0,3})?$", message = "HS Code format must be 4-10 digits (e.g., 0910, 091030, 0910300000, or 0910.30.00)")
    private String hsCode;

    private String description;

    /** Product material (e.g., Cotton, Steel, Glass) */
    private String material;

    /** Product composition (e.g., 100% cotton, 70% polyester 30% cotton) */
    private String composition;

    /** Intended use / function (e.g., Casual clothing, Food seasoning) */
    private String function;

    /** Manufacturing process (e.g., Knitted, Woven, Injection molded) */
    private String manufacturingProcess;

    /** Physical form (e.g., Finished garment, Powder, Liquid, Tablet) */
    private String physicalForm;

    /** Technical specifications */
    private String specifications;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    private BigDecimal weight;
}
