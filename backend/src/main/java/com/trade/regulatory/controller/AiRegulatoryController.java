package com.trade.regulatory.controller;

import com.trade.regulatory.service.*;
import com.trade.regulatory.service.HybridRetrievalService.HybridContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * AI-powered regulatory intelligence endpoints.
 *
 * Uses the NVIDIA Nemotron-3-Ultra LLM for explanations/chat but NEVER as the
 * source of truth. Context is assembled by the HYBRID retrieval layer:
 * structured regulatory data (authoritative) + semantic document RAG (reranked).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiRegulatoryController {

    private final NvidiaAiService aiService;
    private final HybridRetrievalService hybridRetrievalService;
    private final DocumentIngestionService ingestionService;
    private final NvidiaEmbeddingService embeddingService;
    private final NvidiaRerankService rerankService;
    private final RegulatoryKnowledgeService knowledgeService;

    public AiRegulatoryController(NvidiaAiService aiService,
                                  HybridRetrievalService hybridRetrievalService,
                                  DocumentIngestionService ingestionService,
                                  NvidiaEmbeddingService embeddingService,
                                  NvidiaRerankService rerankService,
                                  RegulatoryKnowledgeService knowledgeService) {
        this.aiService = aiService;
        this.hybridRetrievalService = hybridRetrievalService;
        this.ingestionService = ingestionService;
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.knowledgeService = knowledgeService;
    }

    /**
     * POST /api/v1/ai/regulatory-chat
     * Grounded regulatory Q&A — hybrid retrieval first, then LLM explanation.
     */
    @PostMapping("/regulatory-chat")
    public ResponseEntity<?> regulatoryChat(@RequestBody Map<String, String> request) {
        String country = request.getOrDefault("country", "");
        String hsCode = request.getOrDefault("hsCode", "");
        String question = request.getOrDefault("question", "");

        if (country.isBlank() || hsCode.isBlank() || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "country, hsCode, and question are required"));
        }

        // 1 + 2. Hybrid retrieval (structured authoritative + semantic reranked).
        HybridContext hybrid = hybridRetrievalService.retrieve(country, hsCode, question);
        var regData = hybrid.regData;

        // 3. Grounded LLM answer. If the LLM is unavailable, chat() returns a safe
        //    message and the structured data + sources below are still returned.
        String answer = aiService.chat(
                "You are an expert trade compliance advisor for Indian SME exporters. " +
                "Answer the user's question using ONLY the provided context. " +
                "Prioritize the STRUCTURED REGULATORY DATA (it is authoritative and tied to the " +
                "country and HS code). Use document excerpts as supporting detail. " +
                "Be specific, actionable, and cite official sources and page numbers where available.",
                question,
                hybrid.contextText
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", answer);
        response.put("country", regData.country);
        response.put("hsCode", regData.hsCode);
        response.put("matchType", regData.matchType);
        response.put("confidence", regData.confidence);
        response.put("regulationFound", regData.regulationFound);
        response.put("sources", hybrid.sources);
        response.put("usedSemanticRag", hybrid.usedSemantic);
        response.put("usedRerank", hybrid.usedRerank);
        response.put("semanticHits", hybrid.semanticHits);
        response.put("aiAvailable", aiService.isAvailable());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/explain-recommendation
     *
     * Product-wise AI explanation of WHY a destination country is recommended for
     * a specific product (HS code). Grounded on real regulatory data:
     *   1. Hybrid retrieval — structured DB regulations + semantic RAG (authoritative).
     *   2. Knowledge-base fallback — official minimum requirements when DB is sparse.
     *   3. NVIDIA LLM narrative grounded ONLY on the assembled data. If the LLM is
     *      unavailable, a knowledge-based narrative is produced from the same data
     *      (never fabricated, never a mock).
     *
     * Body: { country, hsCode, productName?, category?,
     *         complianceScore?, dutyRate?, mlScore?, mlAvailable? }
     */
    @PostMapping("/explain-recommendation")
    public ResponseEntity<?> explainRecommendation(@RequestBody Map<String, Object> request) {
        String country = str(request.get("country"));
        String hsCode = str(request.get("hsCode"));
        String productName = str(request.get("productName"));
        String category = str(request.get("category"));

        if (country.isBlank() || hsCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "country and hsCode are required"));
        }

        String cleanHs = hsCode.replaceAll("[^0-9]", "");
        String productLabel = productName.isBlank() ? ("HS " + cleanHs) : productName;

        // 1. Structured + semantic retrieval (authoritative regulatory data).
        String question = "Why is " + country + " a good export market for " + productLabel
                + " (HS " + cleanHs + ") from India, and what compliance is required?";
        HybridContext hybrid = hybridRetrievalService.retrieve(country, cleanHs, question);
        var regData = hybrid.regData;

        // 2. Extract structured lists from DB; fall back to knowledge base when sparse.
        List<String> documents = new ArrayList<>();
        List<String> certificates = new ArrayList<>();
        List<String> restrictions = new ArrayList<>();
        List<String> labeling = new ArrayList<>();

        if (regData != null) {
            regData.documents.forEach(d -> documents.add(nonBlank(d.getDocumentName(), d.getRemarks())));
            regData.certifications.forEach(c -> certificates.add(nonBlank(c.getCertificationName(), c.getRemarks())));
            regData.restrictions.forEach(r -> restrictions.add(nonBlank(r.getDescription(), r.getRestrictionType())));
            regData.labeling.forEach(l -> labeling.add(nonBlank(l.getRequirement(), l.getRemarks())));
        }

        boolean sparse = documents.size() < 5 || certificates.size() < 3
                || (regData != null && "CHAPTER".equalsIgnoreCase(String.valueOf(regData.matchType)));

        if (sparse) {
            Map<String, Object> kb = knowledgeService.getKnowledgeBasedRegulations(
                    "India", country, cleanHs, productName, category);
            mergeStrings(documents, asStringList(kb.get("required_documents")));
            mergeStrings(certificates, asStringList(kb.get("certifications")));
            mergeStrings(restrictions, asStringList(kb.get("restricted_products")));
            mergeStrings(labeling, asStringList(kb.get("labeling_requirements")));
        }

        List<String> reqDocuments = dedupeLimit(documents, 12);
        List<String> reqCertificates = dedupeLimit(certificates, 10);
        List<String> reqRestrictions = dedupeLimit(restrictions, 8);
        List<String> reqLabeling = dedupeLimit(labeling, 8);

        // 3. Recommendation drivers (real metrics passed from the ranking view).
        Integer complianceScore = toInt(request.get("complianceScore"));
        Object dutyRate = request.get("dutyRate");
        Object mlScore = request.get("mlScore");
        boolean mlAvailable = Boolean.parseBoolean(str(request.get("mlAvailable")));

        StringBuilder drivers = new StringBuilder();
        if (complianceScore != null) drivers.append("Compliance readiness score: ").append(complianceScore).append("/100. ");
        if (dutyRate != null && !str(dutyRate).isBlank()) drivers.append("Import duty rate: ").append(dutyRate).append("%. ");
        if (mlAvailable && mlScore != null) drivers.append("ML market-opportunity score available (").append(mlScore).append("). ");
        drivers.append("Required documents identified: ").append(reqDocuments.size())
               .append("; certifications: ").append(reqCertificates.size())
               .append("; restrictions: ").append(reqRestrictions.size()).append(".");

        // Assemble grounding context for the LLM (structured, authoritative).
        String context = hybrid.contextText + "\n\nRECOMMENDATION DRIVERS:\n" + drivers +
                "\n\nDOCUMENTS: " + String.join("; ", reqDocuments) +
                "\n\nCERTIFICATIONS: " + String.join("; ", reqCertificates) +
                "\n\nRESTRICTIONS: " + String.join("; ", reqRestrictions) +
                "\n\nLABELING: " + String.join("; ", reqLabeling);

        // 4. Generate the product-specific narrative.
        String explanation;
        String complianceSummary;
        boolean aiUsed = aiService.isAvailable();

        if (aiUsed) {
            String aiExplanation = aiService.chat(
                    "You are a trade compliance advisor for Indian SME exporters. Write a concise, " +
                    "product-specific explanation (3-5 sentences) of WHY " + country + " is a recommended " +
                    "export market for " + productLabel + " (HS " + cleanHs + ") from India. Reference the " +
                    "recommendation drivers and regulatory readiness. Use ONLY the provided context — no assumptions.",
                    "Explain why we recommend exporting " + productLabel + " to " + country + ".",
                    context);
            String aiSummary = aiService.chat(
                    "You are a trade compliance advisor. In 2-3 sentences, summarise the compliance situation " +
                    "for exporting " + productLabel + " (HS " + cleanHs + ") to " + country + " from India, " +
                    "based ONLY on the provided documents, certifications, and restrictions. Be practical.",
                    "Summarise the compliance requirements for " + productLabel + " to " + country + ".",
                    context);

            // Per-field fallback: if a specific LLM call failed/returned a sentinel,
            // substitute the deterministic knowledge-based narrative for THAT field only.
            explanation = isAiFailure(aiExplanation)
                    ? buildFallbackExplanation(country, productLabel, cleanHs,
                            complianceScore, dutyRate, mlAvailable, reqDocuments, reqCertificates, reqRestrictions)
                    : aiExplanation;
            complianceSummary = isAiFailure(aiSummary)
                    ? buildFallbackComplianceSummary(country, productLabel,
                            reqDocuments, reqCertificates, reqRestrictions, reqLabeling)
                    : aiSummary;

            // If BOTH failed, the response is entirely knowledge-based.
            if (isAiFailure(aiExplanation) && isAiFailure(aiSummary)) aiUsed = false;
        } else {
            explanation = buildFallbackExplanation(country, productLabel, cleanHs,
                    complianceScore, dutyRate, mlAvailable, reqDocuments, reqCertificates, reqRestrictions);
            complianceSummary = buildFallbackComplianceSummary(country, productLabel,
                    reqDocuments, reqCertificates, reqRestrictions, reqLabeling);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("country", country);
        response.put("hsCode", cleanHs);
        response.put("productName", productLabel);
        response.put("category", category);
        response.put("explanation", explanation);
        response.put("compliance_summary", complianceSummary);
        response.put("required_documents", reqDocuments);
        response.put("required_certificates", reqCertificates);
        response.put("import_restrictions", reqRestrictions);
        response.put("labeling_rules", reqLabeling);
        response.put("recommendationDrivers", drivers.toString());
        response.put("matchType", regData != null ? regData.matchType : null);
        response.put("confidence", regData != null ? regData.confidence : null);
        response.put("sources", hybrid.sources);
        response.put("aiGenerated", aiUsed);
        response.put("dataSource", sparse ? "DB + KNOWLEDGE_BASE" : "DATABASE");

        return ResponseEntity.ok(response);
    }

    // ── Explanation helpers ──────────────────────────────────────────────────

    private String buildFallbackExplanation(String country, String product, String hs,
                                            Integer complianceScore, Object dutyRate, boolean mlAvailable,
                                            List<String> docs, List<String> certs, List<String> restrictions) {
        StringBuilder sb = new StringBuilder();
        sb.append(country).append(" is recommended for exporting ").append(product)
          .append(" (HS ").append(hs).append(") from India");
        if (complianceScore != null) {
            String tier = complianceScore >= 75 ? "strong" : complianceScore >= 50 ? "moderate" : "developing";
            sb.append(" based on a ").append(tier).append(" compliance readiness score of ")
              .append(complianceScore).append("/100");
        }
        sb.append(". ");
        if (dutyRate != null && !str(dutyRate).isBlank()) {
            sb.append("The applicable import duty of ").append(dutyRate)
              .append("% keeps landed cost competitive. ");
        }
        if (mlAvailable) {
            sb.append("The ML market-ranking model also ranks this destination favourably for this product. ");
        }
        if (restrictions.isEmpty()) {
            sb.append("No product-specific import restrictions were identified for this route, ");
        } else {
            sb.append("There are ").append(restrictions.size())
              .append(" restriction(s) to review before shipping, ");
        }
        sb.append("with ").append(docs.size()).append(" required documents and ")
          .append(certs.size()).append(" certifications mapped for clearance.");
        return sb.toString();
    }

    private String buildFallbackComplianceSummary(String country, String product,
                                                  List<String> docs, List<String> certs,
                                                  List<String> restrictions, List<String> labeling) {
        StringBuilder sb = new StringBuilder();
        sb.append("To export ").append(product).append(" to ").append(country)
          .append(", prepare ").append(docs.size()).append(" mandatory document(s)");
        if (!certs.isEmpty()) sb.append(" and obtain ").append(certs.size()).append(" certification(s)");
        sb.append(". ");
        if (!labeling.isEmpty()) {
            sb.append(labeling.size()).append(" labeling rule(s) apply to the consignment. ");
        }
        if (restrictions.isEmpty()) {
            sb.append("No blocking restrictions were found; the route is clear for standard export procedures.");
        } else {
            sb.append("Address ").append(restrictions.size())
              .append(" restriction(s) with a customs broker before dispatch.");
        }
        return sb.toString();
    }

    /** Detect LLM failure/unavailable sentinel strings so we can fall back per field. */
    private boolean isAiFailure(String s) {
        if (s == null || s.isBlank()) return true;
        String low = s.toLowerCase();
        return low.contains("ai service temporarily unavailable")
                || low.contains("ai service not configured")
                || low.contains("please set the nvidia_api_key");
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private String nonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary.trim();
        return fallback != null ? fallback.trim() : "";
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        try { return (int) Math.round(Double.parseDouble(String.valueOf(o).replaceAll("[^0-9.\\-]", ""))); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) continue;
                if (item instanceof String s) out.add(s);
                else if (item instanceof Map<?, ?> m) {
                    Object v = m.get("description");
                    if (v == null) v = m.get("document_name");
                    if (v == null) v = m.get("certification_name");
                    if (v == null) v = m.get("requirement");
                    if (v != null) out.add(String.valueOf(v));
                } else out.add(String.valueOf(item));
            }
        }
        return out;
    }

    private void mergeStrings(List<String> target, List<String> extra) {
        for (String s : extra) if (s != null && !s.isBlank()) target.add(s.trim());
    }

    private List<String> dedupeLimit(List<String> in, int limit) {
        List<String> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String s : in) {
            if (s == null || s.isBlank()) continue;
            String key = s.trim().toLowerCase();
            if (seen.add(key)) out.add(s.trim());
            if (out.size() >= limit) break;
        }
        return out;
    }

    /**
     * POST /api/v1/ai/summarize-document
     * Structured document summarisation using the NVIDIA LLM.
     */
    @PostMapping("/summarize-document")
    public ResponseEntity<?> summarizeDocument(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        String title = request.getOrDefault("title", "");

        if (text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Document text is required"));
        }

        Map<String, Object> result = aiService.summarizeDocument(text, title);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/ai/ingest-document
     * Ingest a plain-text regulatory document into the semantic vector store.
     * Body: { documentId, country, hsCode, regulationId?, sourceUrl?, text }
     */
    @PostMapping("/ingest-document")
    public ResponseEntity<?> ingestDocument(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        String documentId = request.getOrDefault("documentId", "");
        if (text.isBlank() || documentId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "documentId and text are required"));
        }
        Long regulationId = parseLong(request.get("regulationId"));
        var result = ingestionService.ingestText(
                documentId,
                request.get("country"),
                request.get("hsCode"),
                regulationId,
                request.get("sourceUrl"),
                text);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/ai/ingest-pdf  (multipart/form-data)
     * Ingest a PDF regulatory document (per-page text; optional page images).
     */
    @PostMapping("/ingest-pdf")
    public ResponseEntity<?> ingestPdf(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "documentId", required = false) String documentId,
                                       @RequestParam(value = "country", required = false) String country,
                                       @RequestParam(value = "hsCode", required = false) String hsCode,
                                       @RequestParam(value = "regulationId", required = false) String regulationId,
                                       @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
                                       @RequestParam(value = "renderImages", required = false, defaultValue = "false") boolean renderImages) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        }
        String docId = (documentId != null && !documentId.isBlank())
                ? documentId : file.getOriginalFilename();
        try {
            var result = ingestionService.ingestPdf(
                    docId, country, hsCode, parseLong(regulationId), sourceUrl,
                    file.getBytes(), renderImages);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "PDF ingestion failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/ai/status
     * Report the configured NVIDIA AI/RAG pipeline (LLM + embedding + reranker).
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAiStatus() {
        Map<String, Object> status = new LinkedHashMap<>(aiService.getFullStatus());
        status.put("embeddingModel", embeddingService.getModel());
        status.put("embeddingAvailable", embeddingService.isAvailable());
        status.put("rerankModel", rerankService.getModel());
        status.put("rerankAvailable", rerankService.isAvailable());
        return ResponseEntity.ok(status);
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
