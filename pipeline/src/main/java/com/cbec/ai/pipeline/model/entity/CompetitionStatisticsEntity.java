package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "competition_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uq_competition_stat_key", columnNames = {"hs6", "destination_country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionStatisticsEntity {

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

    @Column(name = "supplier_country_count")
    private Integer supplierCountryCount;

    @Column(name = "top_supplier_country", length = 100)
    private String topSupplierCountry;

    @Column(name = "top_supplier_share_percent")
    private Double topSupplierSharePercent;

    @Column(name = "top5_supplier_share_percent")
    private Double top5SupplierSharePercent;

    @Column(name = "india_market_share_percent")
    private Double indiaMarketSharePercent;

    @Column(name = "india_supplier_rank")
    private Integer indiaSupplierRank;

    @Column(name = "supplier_hhi")
    private Double supplierHhi;

    @Column(name = "source", nullable = false, length = 150)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
