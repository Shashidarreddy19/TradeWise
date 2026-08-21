package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hs_master", uniqueConstraints = {
    @UniqueConstraint(name = "uq_customs_code_version", columnNames = {"country", "customs_territory", "national_code", "dataset_version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HsMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "chapter", nullable = false, length = 2)
    private String chapter;

    @Column(name = "heading", nullable = false, length = 4)
    private String heading;

    @Column(name = "hs6", nullable = false, length = 6)
    private String hs6;

    @Column(name = "national_code", nullable = false, length = 50)
    private String nationalCode;

    @Column(name = "code_length", nullable = false)
    private Integer codeLength;

    @Column(name = "nomenclature_type", nullable = false, length = 50)
    private String nomenclatureType;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "official_description", nullable = false, columnDefinition = "TEXT")
    private String officialDescription;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "dataset_version", nullable = false, length = 50)
    private String datasetVersion;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "record_hash", length = 64)
    private String recordHash;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "last_verified", nullable = false)
    private LocalDateTime lastVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isCurrent == null) this.isCurrent = true;
        if (this.isActive == null) this.isActive = true;
        if (this.effectiveFrom == null) this.effectiveFrom = LocalDateTime.now();
        if (this.lastVerified == null) this.lastVerified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
