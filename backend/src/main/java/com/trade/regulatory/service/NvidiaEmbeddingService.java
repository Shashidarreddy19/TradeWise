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
 * NVIDIA embedding service.
 *
 * Model: nvidia/llama-nemotron-embed-1b-v2 (OpenAI-compatible embeddings API).
 * Supports up to 8192 tokens and REQUIRES an input_type:
 *   - "query"   when embedding a user question (retrieval time)
 *   - "passage" when embedding a document/passage (indexing time)
 *
 * On any failure this service returns an empty vector so callers can fall back
 * to structured SQL retrieval (see migration spec section 16).
 */
@Service
public class NvidiaEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaEmbeddingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.api.key:}")
    private String apiKey;

    @Value("${nvidia.base.url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${nvidia.embedding.model:nvidia/llama-nemotron-embed-1b-v2}")
    private String embeddingModel;

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }

    public String getModel() {
        return embeddingModel;
    }

    /** Embed a user query (input_type=query). */
    public float[] embedQuery(String query) {
        List<float[]> out = embed(List.of(safe(query)), "query");
        return out.isEmpty() ? new float[0] : out.get(0);
    }

    /** Embed a single passage (input_type=passage). */
    public float[] embedPassage(String passage) {
        List<float[]> out = embed(List.of(safe(passage)), "passage");
        return out.isEmpty() ? new float[0] : out.get(0);
    }

    /** Embed a batch of passages (input_type=passage). Order is preserved. */
    public List<float[]> embedPassages(List<String> passages) {
        if (passages == null || passages.isEmpty()) return List.of();
        List<String> cleaned = new ArrayList<>(passages.size());
        for (String p : passages) cleaned.add(safe(p));
        return embed(cleaned, "passage");
    }

    /**
     * Core embeddings call following the NVIDIA / OpenAI-compatible pattern:
     *   input, model, encoding_format=float, extra_body{input_type, truncate:NONE}
     */
    private List<float[]> embed(List<String> inputs, String inputType) {
        if (!isAvailable() || inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", embeddingModel);
            request.put("input", inputs);
            request.put("encoding_format", "float");
            // NVIDIA embedding NIMs accept these at top level on the OpenAI-compatible route.
            request.put("input_type", inputType);
            request.put("truncate", "NONE");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/embeddings", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode data = objectMapper.readTree(response.getBody()).path("data");
                // Preserve request order using the "index" field when present.
                float[][] ordered = new float[inputs.size()][];
                int i = 0;
                for (JsonNode item : data) {
                    int idx = item.path("index").asInt(i);
                    JsonNode emb = item.path("embedding");
                    float[] vec = new float[emb.size()];
                    for (int j = 0; j < emb.size(); j++) vec[j] = (float) emb.get(j).asDouble();
                    if (idx >= 0 && idx < ordered.length) ordered[idx] = vec;
                    i++;
                }
                List<float[]> result = new ArrayList<>(inputs.size());
                for (float[] v : ordered) result.add(v != null ? v : new float[0]);
                log.debug("Generated {} {} embedding(s), dim={}", result.size(), inputType,
                        result.isEmpty() ? 0 : result.get(0).length);
                return result;
            }
            log.warn("Embedding API returned non-2xx: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            log.error("NVIDIA embedding call failed ({}): {}", inputType, safeErr(e));
            return List.of();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeErr(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return msg.replaceAll("nvapi-[A-Za-z0-9_\\-]+", "nvapi-***");
    }
}
