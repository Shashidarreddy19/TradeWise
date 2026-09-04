package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(name = "warehouse_type", length = 50)
    private String warehouseType; // GENERAL, COLD_STORAGE, HAZARDOUS, BONDED, ICD_CONTAINER_YARD

    @Column(name = "total_sqft")
    private Long totalSqft;

    @Column(name = "available_sqft")
    private Long availableSqft;

    @Column(name = "daily_rate_per_sqft", precision = 10, scale = 2)
    private BigDecimal dailyRatePerSqft;

    @Column(name = "cold_storage_available")
    private Boolean coldStorageAvailable;

    @Column(name = "hazardous_storage_available")
    private Boolean hazardousStorageAvailable;

    @Column(name = "nearest_port", length = 100)
    private String nearestPort;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
