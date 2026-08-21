package com.trade.regulatory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.entity.DocumentChunk;
import com.trade.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Semantic vector search over ingested document chunks.
 *
 * Storage is abstracted behind {@link DocumentChunkRepository}: embeddings are
 * persisted as JSON float arrays in MySQL and cosine similarity is computed in
 * Java over a SQL-narrowed candidate set (by country / HS prefix). This keeps
 * the existing MySQL-only architecture intact while providing dense retrieval.
 *
 * Fallback (spec section 16): if embedding is unavailable/fails, returns an
 * empty result so the hybrid layer relies on structured SQL retrieval.
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final DocumentChunkRepository chunkRepo;
    private final NvidiaEmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VectorSearchService(DocumentChunkRepository chunkRepo,
                               NvidiaEmbeddingService embeddingService) {
        this.chunkRepo = chunkRepo;
        this.embeddingService = embeddingService;
    }

    public static class SearchHit {
        public final DocumentChunk chunk;
        public final double score;

        public SearchHit(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    public boolean isAvailable() {
        return embeddingService.isAvailable();
    }

    /**
     * Semantic search.
     *
     * @param query    user question
     * @param country  optional country filter (null = all)
     * @param hsCode   optional HS code (its HS4 prefix is used to narrow candidates; null = all)
     * @param topK     number of hits to return
     */
    public List<SearchHit> search(String query, String country, String hsCode, int topK) {
        if (!isAvailable() || query == null || query.isBlank()) {
            return List.of();
        }

        float[] queryVec = embeddingService.embedQuery(query);
        if (queryVec.length == 0) {
            log.debug("Query embedding empty; skipping semantic search (fallback to structured).");
            return List.of();
        }

        String hsPrefix = hsPrefix(hsCode);
        List<DocumentChunk> candidates;
        try {
            candidates = chunkRepo.findCandidates(blankToNull(country), hsPrefix);
            // If a narrow filter returned nothing, broaden to country-only, then all.
            if (candidates.isEmpty() && hsPrefix != null) {
                candidates = chunkRepo.findCandidates(blankToNull(country), null);
            }
            if (candidates.isEmpty() && country != null && !country.isBlank()) {
                candidates = chunkRepo.findCandidates(null, null);
            }
        } catch (Exception e) {
            log.error("Vector candidate load failed: {}", e.getMessage());
            return List.of();
        }

        List<SearchHit> hits = new ArrayList<>();
        for (DocumentChunk c : candidates) {
            float[] vec = deserialize(c.getEmbedding());
            if (vec.length == 0 || vec.length != queryVec.length) continue;
            double sim = cosine(queryVec, vec);
            hits.add(new SearchHit(c, sim));
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        return hits.size() > topK ? new ArrayList<>(hits.subList(0, topK)) : hits;
    }

    // ── embedding (de)serialization — shared codec for the vector store ──────

    public String serialize(float[] vec) {
        try {
            return objectMapper.writeValueAsString(vec);
        } catch (Exception e) {
            return "[]";
        }
    }

    public float[] deserialize(String json) {
        if (json == null || json.isBlank()) return new float[0];
        try {
            List<Double> list = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] vec = new float[list.size()];
            for (int i = 0; i < list.size(); i++) vec[i] = list.get(i).floatValue();
            return vec;
        } catch (Exception e) {
            return new float[0];
        }
    }

    // ── math / helpers ───────────────────────────────────────────────────────

    private double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private String hsPrefix(String hsCode) {
        if (hsCode == null) return null;
        String digits = hsCode.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) return digits.substring(0, 4);
        return digits.isEmpty() ? null : digits;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
