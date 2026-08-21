package com.trade.regulatory.service;

import com.trade.regulatory.service.RegulatoryRetrievalService.RegulatoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Hybrid retrieval: combines the existing STRUCTURED regulatory retrieval
 * (country + HS hierarchical matching from TradeData) with SEMANTIC document
 * retrieval (dense vector search + multimodal rerank), then assembles a single
 * grounded context for the LLM.
 *
 * Structured matches are authoritative (directly tied to country + HS code) and
 * are ALWAYS included with high priority. Semantic document excerpts supplement
 * them and are ordered by the reranker.
 *
 * Degradation (spec section 16):
 *   - embedding/vector unavailable -> structured-only context
 *   - rerank unavailable           -> semantic hits used in vector-similarity order
 *   - LLM unavailable              -> caller still returns structured data + sources
 */
@Service
public class HybridRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);

    private final RegulatoryRetrievalService retrievalService;
    private final VectorSearchService vectorSearchService;
    private final NvidiaRerankService rerankService;

    @Value("${rag.vector.top-k:12}")
    private int vectorTopK;

    @Value("${rag.rerank.top-n:6}")
    private int rerankTopN;

    public HybridRetrievalService(RegulatoryRetrievalService retrievalService,
                                  VectorSearchService vectorSearchService,
                                  NvidiaRerankService rerankService) {
        this.retrievalService = retrievalService;
        this.vectorSearchService = vectorSearchService;
        this.rerankService = rerankService;
    }

    public static class HybridContext {
        public RegulatoryResult regData;
        public String contextText = "";
        public List<Map<String, String>> sources = new ArrayList<>();
        public boolean usedSemantic = false;
        public boolean usedRerank = false;
        public int semanticHits = 0;
    }

    /**
     * Retrieve combined context for a grounded answer.
     *
     * @param country  destination country
     * @param hsCode   product HS code
     * @param question user question (drives semantic retrieval + reranking)
     */
    public HybridContext retrieve(String country, String hsCode, String question) {
        HybridContext ctx = new HybridContext();

        // 1. STRUCTURED retrieval (authoritative, always included).
        RegulatoryResult regData = retrievalService.getRegulations(country, hsCode);
        ctx.regData = regData;

        StringBuilder context = new StringBuilder();
        context.append("=== STRUCTURED REGULATORY DATA (authoritative, from official database) ===\n");
        context.append(buildStructuredContext(regData));

        // Structured sources.
        if (regData.sources != null) {
            for (var s : regData.sources) {
                ctx.sources.add(Map.of(
                        "type", "structured",
                        "authority", nz(s.getAuthority()),
                        "title", nz(s.getTitle()),
                        "url", nz(s.getSourceUrl())
                ));
            }
        }

        // 2. SEMANTIC retrieval (supplementary).
        List<VectorSearchService.SearchHit> hits = Collections.emptyList();
        try {
            if (vectorSearchService.isAvailable() && question != null && !question.isBlank()) {
                hits = vectorSearchService.search(question, country, hsCode, vectorTopK);
            }
        } catch (Exception e) {
            log.warn("Semantic search failed, continuing structured-only: {}", e.getMessage());
        }

        if (!hits.isEmpty()) {
            ctx.usedSemantic = true;
            ctx.semanticHits = hits.size();

            // 3. Rerank the semantic candidates (multimodal: text + optional page image).
            List<NvidiaRerankService.Passage> passages = new ArrayList<>();
            for (var h : hits) {
                passages.add(new NvidiaRerankService.Passage(
                        h.chunk.getChunkText(), h.chunk.getImageReference()));
            }
            List<NvidiaRerankService.Ranked> ranked = rerankService.rerank(question, passages);
            ctx.usedRerank = rerankService.isAvailable();

            int limit = Math.min(rerankTopN, ranked.size());
            if (limit > 0) {
                context.append("\n=== SUPPLEMENTARY DOCUMENT EXCERPTS (semantic retrieval, reranked) ===\n");
                for (int i = 0; i < limit; i++) {
                    var r = ranked.get(i);
                    var chunk = hits.get(r.index).chunk;
                    String cite = citation(chunk);
                    context.append("\n[Excerpt ").append(i + 1).append(cite).append("]\n");
                    String text = chunk.getChunkText();
                    if (text != null && !text.isBlank()) {
                        context.append(text.length() > 1500 ? text.substring(0, 1500) : text).append("\n");
                    } else if (chunk.getImageReference() != null) {
                        context.append("(document page image)\n");
                    }
                    // Track semantic sources for citation.
                    Map<String, String> src = new LinkedHashMap<>();
                    src.put("type", "document");
                    src.put("documentId", nz(chunk.getDocumentId()));
                    src.put("page", chunk.getPageNumber() != null ? String.valueOf(chunk.getPageNumber()) : "");
                    src.put("url", nz(chunk.getSourceUrl()));
                    ctx.sources.add(src);
                }
            }
        }

        ctx.contextText = context.toString();
        return ctx;
    }

    /**
     * Build the structured-data context block from a regulatory result.
     * (Extracted from the previous controller logic so it is reusable.)
     */
    public String buildStructuredContext(RegulatoryResult regData) {
        StringBuilder context = new StringBuilder();
        context.append("Country: ").append(nz(regData.country)).append("\n");
        context.append("HS Code: ").append(nz(regData.hsCode)).append("\n");
        context.append("Product: ").append(regData.productDescription != null ? regData.productDescription : "N/A").append("\n");
        context.append("Match Level: ").append(nz(regData.matchType)).append("\n");
        context.append("Confidence: ").append(regData.confidence != null ? regData.confidence : "N/A").append("\n");
        context.append("Regulation Found: ").append(regData.regulationFound).append("\n");

        if (regData.regulations != null && !regData.regulations.isEmpty()) {
            context.append("\nREGULATIONS:\n");
            regData.regulations.forEach(r ->
                    context.append("- ").append(r.getTitle())
                           .append(" [").append(r.getAuthority()).append("]\n"));
        }
        if (regData.documents != null && !regData.documents.isEmpty()) {
            context.append("\nREQUIRED DOCUMENTS:\n");
            regData.documents.forEach(d ->
                    context.append("- ").append(d.getDocumentName())
                           .append(Boolean.TRUE.equals(d.getMandatory()) ? " (MANDATORY)" : " (Optional)").append("\n"));
        }
        if (regData.certifications != null && !regData.certifications.isEmpty()) {
            context.append("\nREQUIRED CERTIFICATIONS:\n");
            regData.certifications.forEach(c ->
                    context.append("- ").append(c.getCertificationName())
                           .append(Boolean.TRUE.equals(c.getMandatory()) ? " (MANDATORY)" : " (Optional)").append("\n"));
        }
        if (regData.labeling != null && !regData.labeling.isEmpty()) {
            context.append("\nLABELING REQUIREMENTS:\n");
            regData.labeling.forEach(l -> context.append("- ").append(l.getRequirement()).append("\n"));
        }
        if (regData.restrictions != null && !regData.restrictions.isEmpty()) {
            context.append("\nRESTRICTIONS:\n");
            regData.restrictions.forEach(r ->
                    context.append("- [").append(r.getRestrictionType()).append("] ").append(r.getDescription()).append("\n"));
        }
        if (regData.procedures != null && !regData.procedures.isEmpty()) {
            context.append("\nPROCEDURES:\n");
            regData.procedures.forEach(p ->
                    context.append("- Step ").append(p.getStepOrder() != null ? p.getStepOrder() : "")
                           .append(": ").append(p.getProcedureName()).append(" - ").append(p.getDescription()).append("\n"));
        }
        if (regData.sources != null && !regData.sources.isEmpty()) {
            context.append("\nOFFICIAL SOURCES:\n");
            regData.sources.forEach(s ->
                    context.append("- ").append(s.getTitle()).append(" [").append(s.getAuthority()).append("]")
                           .append(s.getSourceUrl() != null ? " URL: " + s.getSourceUrl() : "").append("\n"));
        }
        return context.toString();
    }

    private String citation(com.trade.entity.DocumentChunk chunk) {
        StringBuilder sb = new StringBuilder();
        if (chunk.getDocumentId() != null) sb.append(" — ").append(chunk.getDocumentId());
        if (chunk.getPageNumber() != null) sb.append(", p.").append(chunk.getPageNumber());
        return sb.toString();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
