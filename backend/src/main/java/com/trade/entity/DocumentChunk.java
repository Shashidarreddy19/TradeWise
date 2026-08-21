package com.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A semantic chunk of an ingested regulatory/government document, with its
 * embedding vector and provenance metadata. Lives in the PRIMARY
 * (InternationalTrade) datasource so it can be created/updated without touching
 * the read-only TradeData schema.
 *
 * The embedding is stored as a JSON array of floats (MySQL 8 has no native
 * vector type); similarity search is performed in Java via cosine similarity.
 * This is intentionally an abstraction over storage so it can later be swapped
 * for pgvector/a dedicated vector DB without changing the retrieval logic.
 */
@Entity
@Table(name = "document_chunk", indexes = {
        @Index(name = "idx_docchunk_country", columnList = "country"),
        @Index(name = "idx_docchunk_hs", columnList = "hs_code"),
        @Index(name = "idx_docchunk_document", columnList = "document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    /** Logical document identifier (e.g. filename or ingestion batch id). */
    @Column(name = "document_id", length = 255)
    private String documentId;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "hs_code", length = 20)
    private String hsCode;

    /** Optional link to a TradeData regulation id (not a JPA FK — cross-database). */
    @Column(name = "regulation_id")
    private Long regulationId;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Lob
    @Column(name = "chunk_text", columnDefinition = "LONGTEXT")
    private String chunkText;

    /** Embedding vector serialized as a JSON float array. */
    @Lob
    @Column(name = "embedding", columnDefinition = "LONGTEXT")
    private String embedding;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /** Optional base64 data URL or path reference to an extracted page image. */
    @Lob
    @Column(name = "image_reference", columnDefinition = "LONGTEXT")
    private String imageReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
