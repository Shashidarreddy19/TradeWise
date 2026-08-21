package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "country_economic_indicators", uniqueConstraints = {
        @UniqueConstraint(name = "uq_economic_ind_key", columnNames = {"country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryEconomicIndicatorsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    @Column(name = "gdp_usd")
    private Double gdpUsd;

    @Column(name = "gdp_per_capita_usd")
    private Double gdpPerCapitaUsd;

    @Column(name = "gdp_growth_percent")
    private Double gdpGrowthPercent;

    @Column(name = "population")
    private Long population;

    @Column(name = "inflation_percent")
    private Double inflationPercent;

    @Column(name = "exchange_rate")
    private Double exchangeRate;

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
