package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.regulation_master — regulations linked to countries.
 * READ-ONLY from the main backend.
 */
@Entity
@Table(name = "regulation_master")
public class RegulationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 255)
    private String authority;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "regulation_type", length = 100)
    private String regulationType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getCountry() { return country; }
    public String getAuthority() { return authority; }
    public String getTitle() { return title; }
    public String getRegulationType() { return regulationType; }
    public String getSummary() { return summary; }
    public LocalDateTime getEffectiveDate() { return effectiveDate; }
    public String getSourceUrl() { return sourceUrl; }
    public Double getConfidenceScore() { return confidenceScore; }
}
