package com.trade.repository;

import com.trade.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for document chunks (vector store) in the primary datasource.
 * Candidate filtering is done in SQL (by country / HS prefix); cosine similarity
 * ranking is done in Java over the returned candidate set.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByCountryIgnoreCase(String country);

    List<DocumentChunk> findByDocumentId(String documentId);

    long countByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);

    /**
     * Candidate chunks for a country, optionally narrowed by HS code prefix.
     * Passing null for either parameter disables that filter.
     */
    @Query("SELECT c FROM DocumentChunk c WHERE "
            + "(:country IS NULL OR LOWER(c.country) = LOWER(:country)) AND "
            + "(:hsPrefix IS NULL OR c.hsCode LIKE CONCAT(:hsPrefix, '%'))")
    List<DocumentChunk> findCandidates(@Param("country") String country,
                                       @Param("hsPrefix") String hsPrefix);
}
