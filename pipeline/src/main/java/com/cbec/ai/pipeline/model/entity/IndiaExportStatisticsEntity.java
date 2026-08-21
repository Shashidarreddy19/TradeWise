package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "india_export_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uq_india_export_key", columnNames = {"hs6", "destination_country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndiaExportStatisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hs_code", nullable = false, length = 50)
    private String hsCode;

    @Column(name = "hs6", nullable = false, length = 6)
    private String hs6;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    @Column(name = "india_export_value_usd")
    private Double indiaExportValueUsd;

    @Column(name = "india_export_quantity")
    private Double indiaExportQuantity;

    @Column(name = "quantity_unit", length = 50)
    private String quantityUnit;

    @Column(name = "previous_year_export_value_usd")
    private Double previousYearExportValueUsd;

    @Column(name = "export_growth_percent")
    private Double exportGrowthPercent;

    @Column(name = "three_year_cagr")
    private Double threeYearCagr;

    @Column(name = "india_market_share_percent")
    private Double indiaMarketSharePercent;

    @Column(name = "india_supplier_rank")
    private Integer indiaSupplierRank;

    @Column(name = "source", nullable = false, length = 150)
    private String source;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "data_timestamp")
    private LocalDateTime dataTimestamp;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (dataTimestamp == null) dataTimestamp = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
