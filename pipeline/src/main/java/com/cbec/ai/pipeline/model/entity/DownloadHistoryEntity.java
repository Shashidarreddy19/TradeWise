package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "download_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "etag", length = 255)
    private String etag;

    @Column(name = "last_modified", length = 255)
    private String lastModified;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "checksum_algorithm", length = 20)
    private String checksumAlgorithm;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "download_url", length = 500)
    private String downloadUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "download_duration_ms")
    private Long downloadDurationMs;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "downloaded_at", nullable = false, updatable = false)
    private LocalDateTime downloadedAt;

    @PrePersist
    protected void onCreate() {
        this.downloadedAt = LocalDateTime.now();
        if (this.checksumAlgorithm == null) this.checksumAlgorithm = "SHA-256";
    }
}
