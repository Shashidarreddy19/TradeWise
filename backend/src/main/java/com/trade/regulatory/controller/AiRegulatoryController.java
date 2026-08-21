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

    public AiRegulatoryController(NvidiaAiService aiService,
                                  HybridRetrievalService hybridRetrievalService,
                                  DocumentIngestionService ingestionService,
                                  NvidiaEmbeddingService embeddingService,
                                  NvidiaRerankService rerankService) {
        this.aiService = aiService;
        this.hybridRetrievalService = hybridRetrievalService;
        this.ingestionService = ingestionService;
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
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
