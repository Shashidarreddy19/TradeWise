package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rejected_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectedRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "raw_record", columnDefinition = "TEXT")
    private String rawRecord;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "pipeline_stage", nullable = false, length = 50)
    private String pipelineStage;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
