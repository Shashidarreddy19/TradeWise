package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tariff_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tariff_stat_key", columnNames = {"hs6", "destination_country", "trade_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffStatisticsEntity {

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

    @Column(name = "mfn_tariff_percent")
    private Double mfnTariffPercent;

    @Column(name = "preferential_tariff_percent")
    private Double preferentialTariffPercent;

    @Column(name = "applied_tariff_percent")
    private Double appliedTariffPercent;

    @Column(name = "bound_tariff_percent")
    private Double boundTariffPercent;

    @Column(name = "tariff_quota", length = 100)
    private String tariffQuota;

    @Column(name = "duty_free")
    private Boolean dutyFree;

    @Column(name = "tariff_type", length = 100)
    private String tariffType;

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
