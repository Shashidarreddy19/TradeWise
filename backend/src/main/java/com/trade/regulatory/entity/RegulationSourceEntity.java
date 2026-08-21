package com.trade.regulatory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to tradedata.regulation_source — official regulatory sources.
 */
@Entity
@Table(name = "regulation_source")
public class RegulationSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 255)
    private String authority;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(length = 30)
    private String status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public Long getId() { return id; }
    public String getCountry() { return country; }
    public String getAuthority() { return authority; }
    public String getTitle() { return title; }
    public String getDocumentType() { return documentType; }
    public String getSourceUrl() { return sourceUrl; }
    public String getStatus() { return status; }
}
