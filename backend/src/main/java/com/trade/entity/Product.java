package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a product listed by an exporter.
 * Linked to a product category and the owning exporter's user record.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The exporter who owns this product listing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exporter_id", nullable = false)
    private User exporter;

    // Category this product belongs to (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    /**
     * Denormalized category name stored in the DB as a NOT NULL varchar(100).
     * Kept in sync with the category FK relationship.
     */
    @Column(name = "category", nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false)
    private String name;

    /**
     * Harmonized System (HS) Code for customs classification.
     * e.g. "0910.30" for Turmeric Powder
     */
    @Column(name = "hs_code", nullable = true, length = 20)
    private String hsCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Product material (e.g., Cotton, Steel, Glass) */
    @Column(length = 255)
    private String material;

    /** Product composition (e.g., 100% cotton) */
    @Column(length = 255)
    private String composition;

    /** Intended use / function */
    @Column(name = "product_function", length = 255)
    private String function;

    /** Manufacturing process (e.g., Knitted, Woven) */
    @Column(name = "manufacturing_process", length = 255)
    private String manufacturingProcess;

    /** Physical form (e.g., Finished garment, Powder) */
    @Column(name = "physical_form", length = 255)
    private String physicalForm;

    /** Technical specifications */
    @Column(columnDefinition = "TEXT")
    private String specifications;

    /**
     * Manufacturing cost per unit.
     * NOT NULL in the DB — defaults to the price if not provided.
     */
    @Column(name = "manufacturing_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal manufacturingCost;

    /**
     * Unit price in INR.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Available quantity in standard unit (kg, units, etc.)
     */
    @Column
    private Integer quantity;

    /**
     * Weight per unit in kilograms.
     */
    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
