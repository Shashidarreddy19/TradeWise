package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cbec_country_recommendation_dataset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cbec_dataset_key", columnNames = {"hs6", "destination_country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CbecCountryRecommendationDatasetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hs6", nullable = false, length = 6)
    private String hs6;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    // Trade Demand Features
    @Column(name = "import_value_usd")
    private Double importValueUsd;

    @Column(name = "import_quantity")
    private Double importQuantity;

    @Column(name = "import_growth_rate")
    private Double importGrowthRate;

    @Column(name = "import_cagr")
    private Double importCagr;

    @Column(name = "market_size_score")
    private Integer marketSizeScore;

    // India Performance Features
    @Column(name = "india_export_value_usd")
    private Double indiaExportValueUsd;

    @Column(name = "india_export_quantity")
    private Double indiaExportQuantity;

    @Column(name = "india_market_share_percent")
    private Double indiaMarketSharePercent;

    @Column(name = "india_export_growth_rate")
    private Double indiaExportGrowthRate;

    @Column(name = "india_export_cagr")
    private Double indiaExportCagr;

    @Column(name = "india_historical_export_trend", length = 50)
    private String indiaHistoricalExportTrend;

    // Tariff Features
    @Column(name = "mfn_tariff_percent")
    private Double mfnTariffPercent;

    @Column(name = "preferential_tariff_percent")
    private Double preferentialTariffPercent;

    @Column(name = "effective_tariff_percent")
    private Double effectiveTariffPercent;

    @Column(name = "tariff_advantage_score")
    private Integer tariffAdvantageScore;

    // Competition Features
    @Column(name = "supplier_hhi")
    private Double supplierHhi;

    @Column(name = "top_supplier_share")
    private Double topSupplierShare;

    @Column(name = "number_of_supplier_countries")
    private Integer numberOfSupplierCountries;

    @Column(name = "competition_score")
    private Integer competitionScore;

    // Compliance & Regulatory Complexity Features (from existing DB)
    @Column(name = "compliance_score")
    private Integer complianceScore;

    @Column(name = "document_count")
    private Integer documentCount;

    @Column(name = "certification_count")
    private Integer certificationCount;

    @Column(name = "restriction_count")
    private Integer restrictionCount;

    @Column(name = "labeling_requirement_count")
    private Integer labelingRequirementCount;

    @Column(name = "procedure_count")
    private Integer procedureCount;

    @Column(name = "regulatory_burden_score")
    private Integer regulatoryBurdenScore;

    // Economic Features
    @Column(name = "gdp_usd")
    private Double gdpUsd;

    @Column(name = "gdp_per_capita_usd")
    private Double gdpPerCapitaUsd;

    @Column(name = "population")
    private Long population;

    @Column(name = "gdp_growth")
    private Double gdpGrowth;

    // Risk Features
    @Column(name = "country_risk_score")
    private Integer countryRiskScore;

    @Column(name = "political_economic_risk_indicator", length = 50)
    private String politicalEconomicRiskIndicator;

    @Column(name = "trade_risk_indicator", length = 50)
    private String tradeRiskIndicator;

    // Accessibility Features
    @Column(name = "market_access_score")
    private Integer marketAccessScore;

    @Column(name = "preferential_trade_agreement_indicator")
    private Boolean preferentialTradeAgreementIndicator;

    @Column(name = "india_trade_agreement_indicator")
    private Boolean indiaTradeAgreementIndicator;

    // Currency Features
    @Column(name = "exchange_rate")
    private Double exchangeRate;

    @Column(name = "exchange_rate_volatility")
    private Double exchangeRateVolatility;

    // Final Derived Normalized Sub-Scores (0-100)
    @Column(name = "demand_score")
    private Integer demandScore;

    @Column(name = "growth_score")
    private Integer growthScore;

    @Column(name = "tariff_score")
    private Integer tariffScore;

    @Column(name = "compliance_score_normalized")
    private Integer complianceScoreNormalized;

    @Column(name = "economic_score")
    private Integer economicScore;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "accessibility_score")
    private Integer accessibilityScore;

    @Column(name = "india_potential_score")
    private Integer indiaPotentialScore;

    // Source Metadata
    @Column(name = "trade_source", length = 150)
    private String tradeSource;

    @Column(name = "tariff_source", length = 150)
    private String tariffSource;

    @Column(name = "economic_source", length = 150)
    private String economicSource;

    @Column(name = "risk_source", length = 150)
    private String riskSource;

    @Column(name = "dataset_status", length = 50)
    private String datasetStatus;

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
