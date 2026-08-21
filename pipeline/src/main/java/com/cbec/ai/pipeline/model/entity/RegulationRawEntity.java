package com.cbec.ai.pipeline.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regulation_raw")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationRawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "authority", nullable = false, length = 255)
    private String authority;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "section", columnDefinition = "TEXT")
    private String section;

    @Column(name = "subsection", columnDefinition = "TEXT")
    private String subsection;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "download_id")
    private Long downloadId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
