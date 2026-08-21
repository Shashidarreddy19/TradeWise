package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "source_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customs_territory", nullable = false, length = 50)
    private String customsTerritory;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "authority", nullable = false, length = 150)
    private String authority;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "download_url", length = 500)
    private String downloadUrl;

    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "data_format", nullable = false, length = 20)
    private String dataFormat;

    @Column(name = "nomenclature_type", nullable = false, length = 50)
    private String nomenclatureType;

    @Column(name = "minimum_code_length", nullable = false)
    private Integer minimumCodeLength;

    @Column(name = "maximum_code_length", nullable = false)
    private Integer maximumCodeLength;

    @Column(name = "preferred_parser", length = 50)
    private String preferredParser;

    @Column(name = "authentication_type", length = 30)
    private String authenticationType;

    @Column(name = "checksum_algorithm", length = 20)
    private String checksumAlgorithm;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "supports_versioning")
    private Boolean supportsVersioning;

    @Column(name = "supports_incremental_updates")
    private Boolean supportsIncrementalUpdates;

    @Column(name = "sync_frequency", nullable = false, length = 20)
    private String syncFrequency;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "last_successful_sync")
    private LocalDateTime lastSuccessfulSync;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.minimumCodeLength == null) this.minimumCodeLength = 6;
        if (this.maximumCodeLength == null) this.maximumCodeLength = 12;
        if (this.status == null) this.status = "ACTIVE";
        if (this.checksumAlgorithm == null) this.checksumAlgorithm = "SHA-256";
    }
}
