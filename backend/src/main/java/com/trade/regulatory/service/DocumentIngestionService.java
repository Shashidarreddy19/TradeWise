package com.trade.regulatory.service;

import com.trade.entity.DocumentChunk;
import com.trade.repository.DocumentChunkRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Ingests regulatory/government documents into the vector store:
 *
 *   text/PDF -> extract (per page) -> chunk -> embed (passage) -> persist
 *
 * Provenance (document id, country, HS code, regulation id, page number,
 * source URL, optional page image) is preserved on each chunk so the final
 * answer can cite the original source.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final int EMBED_BATCH = 16;

    private final DocumentChunkRepository chunkRepo;
    private final NvidiaEmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;

    @Value("${rag.chunk.size:1200}")
    private int chunkSize;

    @Value("${rag.chunk.overlap:200}")
    private int chunkOverlap;

    public DocumentIngestionService(DocumentChunkRepository chunkRepo,
                                    NvidiaEmbeddingService embeddingService,
                                    VectorSearchService vectorSearchService) {
        this.chunkRepo = chunkRepo;
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
    }

    public static class IngestResult {
        public String documentId;
        public int chunksCreated;
        public int pagesProcessed;
        public boolean embedded;
        public String note;
    }

    public boolean isAvailable() {
        return embeddingService.isAvailable();
    }

    /**
     * Ingest a plain-text document. Re-ingesting the same documentId replaces
     * previous chunks.
     */
    @Transactional
    public IngestResult ingestText(String documentId, String country, String hsCode,
                                   Long regulationId, String sourceUrl, String text) {
        IngestResult result = new IngestResult();
        result.documentId = documentId;
        if (text == null || text.isBlank()) {
            result.note = "No text supplied";
            return result;
        }
        deleteExisting(documentId);

        List<String> chunks = chunkText(text);
        List<DocumentChunk> saved = embedAndBuild(documentId, country, hsCode, regulationId,
                sourceUrl, null, chunks, null);
        chunkRepo.saveAll(saved);

        result.chunksCreated = saved.size();
        result.pagesProcessed = 1;
        result.embedded = embeddingService.isAvailable();
        result.note = result.embedded ? "Ingested and embedded" :
                "Stored without embeddings (embedding service unavailable)";
        return result;
    }

    /**
     * Ingest a PDF: extracts text per page, chunks each page, and (optionally)
     * renders a page image (base64 PNG) for multimodal reranking.
     */
    @Transactional
    public IngestResult ingestPdf(String documentId, String country, String hsCode,
                                  Long regulationId, String sourceUrl,
                                  byte[] pdfBytes, boolean renderImages) {
        IngestResult result = new IngestResult();
        result.documentId = documentId;
        deleteExisting(documentId);

        List<DocumentChunk> allChunks = new ArrayList<>();
        int pages = 0;
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = renderImages ? new PDFRenderer(doc) : null;

            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);

                String pageImage = null;
                if (renderer != null) {
                    pageImage = renderPageImage(renderer, p - 1);
                }

                if ((pageText == null || pageText.isBlank()) && pageImage == null) {
                    continue;
                }

                List<String> pageChunks = chunkText(pageText != null ? pageText : "");
                if (pageChunks.isEmpty() && pageImage != null) {
                    pageChunks = List.of(""); // image-only page
                }
                List<DocumentChunk> built = embedAndBuild(documentId, country, hsCode,
                        regulationId, sourceUrl, p, pageChunks, pageImage);
                allChunks.addAll(built);
            }
        } catch (Exception e) {
            log.error("PDF ingestion failed for {}: {}", documentId, e.getMessage());
            result.note = "PDF parsing failed: " + e.getMessage();
            return result;
        }

        chunkRepo.saveAll(allChunks);
        result.chunksCreated = allChunks.size();
        result.pagesProcessed = pages;
        result.embedded = embeddingService.isAvailable();
        result.note = result.embedded ? "PDF ingested and embedded"
                : "PDF stored without embeddings (embedding service unavailable)";
        return result;
    }

    // ── internals ────────────────────────────────────────────────────────────

    private List<DocumentChunk> embedAndBuild(String documentId, String country, String hsCode,
                                              Long regulationId, String sourceUrl, Integer pageNumber,
                                              List<String> chunks, String pageImage) {
        List<DocumentChunk> built = new ArrayList<>();
        if (chunks == null || chunks.isEmpty()) return built;

        // Embed in batches (only non-empty texts).
        Map<Integer, float[]> vectors = new HashMap<>();
        if (embeddingService.isAvailable()) {
            for (int start = 0; start < chunks.size(); start += EMBED_BATCH) {
                int end = Math.min(start + EMBED_BATCH, chunks.size());
                List<String> batch = chunks.subList(start, end);
                List<float[]> embedded = embeddingService.embedPassages(batch);
                for (int i = 0; i < embedded.size(); i++) {
                    vectors.put(start + i, embedded.get(i));
                }
            }
        }

        for (int i = 0; i < chunks.size(); i++) {
            float[] vec = vectors.get(i);
            DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .country(country)
                    .hsCode(hsCode)
                    .regulationId(regulationId)
                    .pageNumber(pageNumber)
                    .chunkText(chunks.get(i))
                    .embedding(vec != null && vec.length > 0 ? vectorSearchService.serialize(vec) : null)
                    .sourceUrl(sourceUrl)
                    .imageReference(pageImage)
                    .build();
            built.add(chunk);
        }
        return built;
    }

    private void deleteExisting(String documentId) {
        try {
            if (chunkRepo.countByDocumentId(documentId) > 0) {
                chunkRepo.deleteByDocumentId(documentId);
            }
        } catch (Exception e) {
            log.warn("Could not clear existing chunks for {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Chunk text into overlapping windows on sentence/whitespace boundaries.
     */
    List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null) return chunks;
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return chunks;
        if (normalized.length() <= chunkSize) {
            chunks.add(normalized);
            return chunks;
        }
        int step = Math.max(1, chunkSize - chunkOverlap);
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) break;
        }
        return chunks;
    }

    private String renderPageImage(PDFRenderer renderer, int pageIndex) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 100, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            log.warn("Page image render failed for page {}: {}", pageIndex + 1, e.getMessage());
            return null;
        }
    }
}
