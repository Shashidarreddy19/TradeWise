package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_data_quality_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDataQualityAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "hs6", length = 10)
    private String hs6;

    @Column(name = "trade_year")
    private Integer year;

    @Column(name = "check_type", nullable = false, length = 100)
    private String checkType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "source", length = 150)
    private String source;

    @Column(name = "audit_timestamp", updatable = false)
    private LocalDateTime auditTimestamp;

    @PrePersist
    protected void onCreate() {
        if (auditTimestamp == null) {
            auditTimestamp = LocalDateTime.now();
        }
    }
}
