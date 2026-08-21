package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.hs_master — the authoritative HS code reference table.
 * Contains 63,746 real HS codes across 7 customs territories.
 * READ-ONLY from the main backend.
 */
@Entity
@Table(name = "hs_master")
public class HsMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customs_territory", nullable = false)
    private String customsTerritory;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false, length = 2)
    private String chapter;

    @Column(nullable = false, length = 4)
    private String heading;

    @Column(nullable = false, length = 6)
    private String hs6;

    @Column(name = "national_code", nullable = false, length = 50)
    private String nationalCode;

    @Column(name = "code_length", nullable = false)
    private Integer codeLength;

    @Column(name = "nomenclature_type", nullable = false)
    private String nomenclatureType;

    @Column(name = "official_description", nullable = false, columnDefinition = "TEXT")
    private String officialDescription;

    @Column(length = 50)
    private String unit;

    @Column(name = "dataset_version", nullable = false)
    private String datasetVersion;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "record_hash", length = 64)
    private String recordHash;

    @Column(length = 255)
    private String remarks;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "last_verified", nullable = false)
    private LocalDateTime lastVerified;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 100)
    private String category;

    // ── Getters ─────────────────────────────────────────────────────────────
    public Long getId() { return id; }
    public String getCustomsTerritory() { return customsTerritory; }
    public String getCountry() { return country; }
    public String getChapter() { return chapter; }
    public String getHeading() { return heading; }
    public String getHs6() { return hs6; }
    public String getNationalCode() { return nationalCode; }
    public Integer getCodeLength() { return codeLength; }
    public String getNomenclatureType() { return nomenclatureType; }
    public String getOfficialDescription() { return officialDescription; }
    public String getUnit() { return unit; }
    public String getDatasetVersion() { return datasetVersion; }
    public Boolean getIsCurrent() { return isCurrent; }
    public Boolean getIsActive() { return isActive; }
    public String getCategory() { return category; }
    public LocalDateTime getLastVerified() { return lastVerified; }
}
