package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.government_incentives — real government incentive schemes.
 * Populated only from official government sources.
 */
@Entity
@Table(name = "government_incentives")
public class GovernmentIncentiveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "scheme_name", nullable = false, length = 255)
    private String schemeName;

    @Column(nullable = false, length = 255)
    private String authority;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    @Column(columnDefinition = "TEXT")
    private String benefit;

    @Column(name = "chapter_applicable", length = 255)
    private String chapterApplicable;

    @Column(name = "hs_code_applicable", length = 255)
    private String hsCodeApplicable;

    @Column(length = 100)
    private String sector;

    @Column(name = "application_process", columnDefinition = "TEXT")
    private String applicationProcess;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_document", length = 255)
    private String sourceDocument;

    @Column(length = 30)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCountry() { return country; }
    public String getSchemeName() { return schemeName; }
    public String getAuthority() { return authority; }
    public String getDescription() { return description; }
    public String getEligibility() { return eligibility; }
    public String getBenefit() { return benefit; }
    public String getChapterApplicable() { return chapterApplicable; }
    public String getHsCodeApplicable() { return hsCodeApplicable; }
    public String getSector() { return sector; }
    public String getApplicationProcess() { return applicationProcess; }
    public LocalDateTime getEffectiveDate() { return effectiveDate; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public String getSourceUrl() { return sourceUrl; }
    public String getStatus() { return status; }
}
