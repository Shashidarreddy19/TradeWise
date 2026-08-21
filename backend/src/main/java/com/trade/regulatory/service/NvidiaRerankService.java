package com.trade.regulatory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * NVIDIA multimodal reranking service.
 *
 * Model: nvidia/llama-nemotron-rerank-vl-1b-v2
 * Endpoint: https://ai.api.nvidia.com/v1/retrieval/.../reranking
 *
 * Given a query and a list of candidate passages (text and/or a base64 page
 * image), the reranker returns a relevance logit per passage. Higher = more
 * relevant. Callers use the returned order to select the best context for the
 * LLM.
 *
 * Fallback (migration spec section 16): if the reranker is unavailable or
 * fails, this service returns the passages in their original order so the
 * pipeline degrades gracefully rather than failing.
 */
@Service
public class NvidiaRerankService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaRerankService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.api.key:}")
    private String apiKey;

    @Value("${nvidia.rerank.model:nvidia/llama-nemotron-rerank-vl-1b-v2}")
    private String rerankModel;

    @Value("${nvidia.rerank.url:https://ai.api.nvidia.com/v1/retrieval/nvidia/llama-nemotron-rerank-vl-1b-v2/reranking}")
    private String rerankUrl;

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank()
                && rerankModel != null && !rerankModel.isBlank();
    }

    public String getModel() {
        return rerankModel;
    }

    /** A candidate passage. imageDataUrl is optional (base64 data URL of a page image). */
    public static class Passage {
        public final String text;
        public final String imageDataUrl;

        public Passage(String text) {
            this(text, null);
        }

        public Passage(String text, String imageDataUrl) {
            this.text = text != null ? text : "";
            this.imageDataUrl = imageDataUrl;
        }
    }

    /** A ranking result: index into the original passage list + relevance logit. */
    public static class Ranked {
        public final int index;
        public final double logit;

        public Ranked(int index, double logit) {
            this.index = index;
            this.logit = logit;
        }
    }

    /**
     * Rerank passages against the query.
     *
     * @return list of {@link Ranked} sorted by descending relevance. On failure
     *         or when unavailable, returns the identity order (index 0..n-1).
     */
    public List<Ranked> rerank(String query, List<Passage> passages) {
        List<Ranked> identity = identityOrder(passages);
        if (!isAvailable() || passages == null || passages.isEmpty() || query == null || query.isBlank()) {
            return identity;
        }

        try {
            List<Map<String, Object>> passagePayload = new ArrayList<>(passages.size());
            for (Passage p : passages) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("text", p.text);
                if (p.imageDataUrl != null && !p.imageDataUrl.isBlank()) {
                    pm.put("image", p.imageDataUrl);
                }
                passagePayload.add(pm);
            }

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", rerankModel);
            request.put("query", Map.of("text", query));
            request.put("passages", passagePayload);
            request.put("truncate", "END");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(rerankUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Ranked> parsed = parseRankings(response.getBody(), passages.size());
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }
            log.warn("Rerank API returned non-usable response ({}); using original order",
                    response.getStatusCode());
            return identity;
        } catch (Exception e) {
            log.error("NVIDIA rerank call failed: {}; using original order", safeErr(e));
            return identity;
        }
    }

    private List<Ranked> parseRankings(String body, int count) {
        try {
            JsonNode root = objectMapper.readTree(body);
            // Standard NVIDIA rerank shape: {"rankings":[{"index":i,"logit":x}, ...]}
            JsonNode rankings = root.path("rankings");
            if (rankings.isMissingNode() || !rankings.isArray()) {
                // Some variants use "results" with "relevance_score".
                rankings = root.path("results");
            }
            if (!rankings.isArray()) return List.of();

            List<Ranked> out = new ArrayList<>();
            for (JsonNode r : rankings) {
                int idx = r.has("index") ? r.path("index").asInt(-1) : -1;
                double score;
                if (r.has("logit")) score = r.path("logit").asDouble();
                else if (r.has("relevance_score")) score = r.path("relevance_score").asDouble();
                else if (r.has("score")) score = r.path("score").asDouble();
                else score = 0.0;
                if (idx >= 0 && idx < count) {
                    out.add(new Ranked(idx, score));
                }
            }
            // Ensure descending by logit (API usually returns pre-sorted).
            out.sort((a, b) -> Double.compare(b.logit, a.logit));
            return out;
        } catch (Exception e) {
            log.warn("Failed to parse rerank response: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Ranked> identityOrder(List<Passage> passages) {
        List<Ranked> identity = new ArrayList<>();
        if (passages != null) {
            for (int i = 0; i < passages.size(); i++) identity.add(new Ranked(i, 0.0));
        }
        return identity;
    }

    private String safeErr(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return msg.replaceAll("nvapi-[A-Za-z0-9_\\-]+", "nvapi-***");
    }
}
