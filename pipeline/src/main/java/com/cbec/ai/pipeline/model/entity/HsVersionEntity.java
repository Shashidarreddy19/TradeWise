package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hs_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hs_master_id", nullable = false)
    private Long hsMasterId;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "national_code", nullable = false, length = 50)
    private String nationalCode;

    @Column(name = "official_description", nullable = false, columnDefinition = "TEXT")
    private String officialDescription;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "last_verified", nullable = false)
    private LocalDateTime lastVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.effectiveFrom == null) this.effectiveFrom = LocalDateTime.now();
        if (this.lastVerified == null) this.lastVerified = LocalDateTime.now();
    }
}
