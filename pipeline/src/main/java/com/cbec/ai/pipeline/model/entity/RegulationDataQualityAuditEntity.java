package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regulation_data_quality_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationDataQualityAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus;

    @Column(name = "validation_reason", columnDefinition = "TEXT")
    private String validationReason;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
    }
}
