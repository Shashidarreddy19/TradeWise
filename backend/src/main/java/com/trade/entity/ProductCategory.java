package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a product category supported by the Trade platform.
 * Seeded at application startup via DataSeeder.
 */
@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;
}
