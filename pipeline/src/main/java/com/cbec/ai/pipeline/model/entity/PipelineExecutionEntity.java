package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_name", nullable = false, length = 150)
    private String pipelineName;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "records_found")
    private Long recordsFound;

    @Column(name = "records_inserted")
    private Long recordsInserted;

    @Column(name = "records_updated")
    private Long recordsUpdated;

    @Column(name = "duplicates")
    private Long duplicates;

    @Column(name = "invalid_records")
    private Long invalidRecords;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
}
