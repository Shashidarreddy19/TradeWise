package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_feature_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationFeatureAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "hs6", nullable = false, length = 10)
    private String hs6;

    @Column(name = "trade_year", nullable = false)
    private Integer year;

    @Column(name = "audit_status", nullable = false, length = 50)
    private String auditStatus; // DATA_AVAILABLE, DATA_UNAVAILABLE, DATA_QUALITY_WARNING

    @Column(name = "null_feature_count")
    private Integer nullFeatureCount;

    @Column(name = "quality_issue_details", columnDefinition = "TEXT")
    private String qualityIssueDetails;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) auditTimestamp = LocalDateTime.now();
    }
}
