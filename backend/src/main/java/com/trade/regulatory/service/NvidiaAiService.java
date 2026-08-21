package com.trade.regulatory.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NVIDIA LLM integration service (main answer generation layer).
 *
 * Model: nvidia/nemotron-3-ultra-550b-a55b (reasoning-capable, text-only)
 * via the OpenAI-compatible endpoint at https://integrate.api.nvidia.com/v1.
 *
 * This service is NEVER the source of truth. It only explains / summarises /
 * advises on the basis of pre-retrieved regulatory context supplied by the
 * hybrid retrieval layer. Grounding rules are enforced in the system prompt.
 *
 * Responsibilities (see migration spec section 13):
 *   - chat()                     -> grounded regulatory Q&A / explanations
 *   - summarizeDocument()        -> structured document summarization
 *   - generateNegotiationAdvice()-> grounded negotiation assistant
 */
@Service
public class NvidiaAiService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaAiService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${nvidia.api.key:}")
    private String apiKey;

    @Value("${nvidia.base.url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${nvidia.model:nvidia/nemotron-3-ultra-550b-a55b}")
    private String model;

    @Value("${nvidia.llm.max-tokens:4096}")
    private int maxTokens;

    @Value("${nvidia.llm.temperature:0.2}")
    private double temperature;

    @Value("${nvidia.llm.top-p:0.95}")
    private double topP;

    @Value("${nvidia.llm.enable-thinking:false}")
    private boolean enableThinking;

    @Value("${nvidia.llm.reasoning-budget:4096}")
    private int reasoningBudget;

    /**
     * Shared grounding preamble — enforced on every LLM call so the model can
     * never invent regulatory facts.
     */
    private static final String GROUNDING_RULES =
            "\n\nGROUNDING RULES (STRICT):\n" +
            "- The retrieved database records and official documents are the ONLY source of truth.\n" +
            "- Answer ONLY using the supplied regulatory/document context.\n" +
            "- NEVER invent certificates, regulations, authorities, tariff rates, government schemes,\n" +
            "  URLs, deadlines, customs procedures, restrictions, or compliance requirements.\n" +
            "- If the context is insufficient, clearly state that the available regulatory data is\n" +
            "  insufficient to answer confidently.\n" +
            "- Distinguish (1) facts directly supported by the sources from (2) your reasoning/explanation.\n" +
            "- Cite the source document / authority / page number when possible.\n" +
            "- Be precise and conservative: exporters rely on this for legal compliance.";

    /** Retry attempts for transient upstream errors (5xx / 404 / timeouts). */
    private static final int MAX_ATTEMPTS = 3;

    public NvidiaAiService() {
        // Explicit timeouts: frontier models can be slow, but we don't want to hang forever.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);   // 15s to connect
        factory.setReadTimeout(180_000);      // 180s to read (550B generation can be slow)
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHAT — grounded answer generation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generate a grounded regulatory answer using retrieved context.
     * The context MUST come from retrieval — this method adds explanation only.
     */
    public String chat(String systemPrompt, String userMessage, String regulatoryContext) {
        if (!isAvailable()) {
            return "AI service not configured. Please set the NVIDIA_API_KEY environment variable. " +
                   "Structured regulatory data is still available through the non-AI APIs.";
        }

        String fullSystemPrompt = systemPrompt +
                (regulatoryContext != null && !regulatoryContext.isBlank()
                        ? "\n\nRETRIEVED CONTEXT FROM OFFICIAL SOURCES:\n" + regulatoryContext
                        : "") +
                GROUNDING_RULES;

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("messages", List.of(
                Map.of("role", "system", "content", fullSystemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        request.put("temperature", temperature);
        request.put("top_p", topP);
        request.put("max_tokens", maxTokens);
        request.put("stream", false);

        // Reasoning ("thinking") controls for Nemotron-3-Ultra.
        Map<String, Object> chatTemplateKwargs = new LinkedHashMap<>();
        chatTemplateKwargs.put("enable_thinking", enableThinking);
        request.put("chat_template_kwargs", chatTemplateKwargs);
        if (enableThinking) {
            request.put("reasoning_budget", reasoningBudget);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // Retry on transient upstream errors (5xx / 404 / timeouts) with backoff.
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(
                        baseUrl + "/chat/completions", entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode message = root.path("choices").path(0).path("message");
                    // Reasoning models may put the answer in content and traces in reasoning_content.
                    String content = message.path("content").asText("");
                    return stripReasoning(content);
                }
                log.warn("NVIDIA chat API returned non-2xx: {}", response.getStatusCode());
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 5xx or timeout/connection issue — transient, retry.
                log.warn("NVIDIA chat transient error (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, safeErr(e));
            } catch (HttpClientErrorException e) {
                // 404 from the gateway is often transient for busy frontier models; retry it.
                // Other 4xx (400/401/403/422) are not retryable — fail fast.
                if (e.getStatusCode().value() == 404) {
                    log.warn("NVIDIA chat 404 (attempt {}/{}) — retrying", attempt, MAX_ATTEMPTS);
                } else {
                    log.error("NVIDIA chat non-retryable client error: {}", safeErr(e));
                    break;
                }
            } catch (Exception e) {
                log.error("NVIDIA chat call failed: {}", safeErr(e));
                break;
            }

            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(1500L * attempt); // linear backoff: 1.5s, 3s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return "AI service temporarily unavailable after retries. Regulatory data is still accessible through the structured APIs.";
    }

    // ══════════════════════════════════════════════════════════════════════
    // DOCUMENT SUMMARIZATION — structured extraction
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Summarize a trade/regulatory document into a structured summary.
     * Extracts regulation metadata when present; uses "Not specified in the document"
     * for missing fields and never invents information.
     *
     * @param text  full (or already-retrieved most-relevant) document text
     * @param title optional document title
     */
    public Map<String, Object> summarizeDocument(String text, String title) {
        if (!isAvailable()) {
            return Map.of(
                    "error", "AI service not configured. Set NVIDIA_API_KEY.",
                    "available", false);
        }
        if (text == null || text.isBlank()) {
            return Map.of("error", "Document text is required", "available", true);
        }

        // Nemotron-3-Ultra handles long context; allow a generous window.
        String docText = text.length() > 60000 ? text.substring(0, 60000) : text;

        String systemPrompt =
                "You are a trade compliance document analyst. Produce a precise, structured summary " +
                "of the supplied regulatory/trade document. Extract ONLY what is present in the document. " +
                "For any field not present, write exactly \"Not specified in the document\". Never invent.";

        String userPrompt =
                "Document title: " + (title != null && !title.isBlank() ? title : "Untitled") + "\n\n" +
                "Summarize the document below. Return clearly labelled sections:\n" +
                "1. Overview (2-3 sentences)\n" +
                "2. Regulation name\n" +
                "3. Issuing authority\n" +
                "4. Effective date\n" +
                "5. Applicable countries\n" +
                "6. Applicable HS codes\n" +
                "7. Required documents\n" +
                "8. Required certifications\n" +
                "9. Labeling requirements\n" +
                "10. Restrictions\n" +
                "11. Customs procedures\n" +
                "12. Deadlines\n" +
                "13. Other important compliance requirements\n" +
                "14. Official source / references (incl. page numbers if present)\n\n" +
                "DOCUMENT:\n" + docText;

        String summary = chat(systemPrompt, userPrompt, "");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("title", title != null && !title.isBlank() ? title : "Untitled");
        result.put("model", model);
        result.put("available", true);
        result.put("note", "AI-generated structured summary — verify against the original official document.");
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // NEGOTIATION ADVICE — grounded negotiation assistant
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Generate grounded negotiation advice. The caller supplies the negotiation
     * prompt and the retrieved regulatory context; regulatory facts must come
     * from that context, never from the model.
     */
    public String generateNegotiationAdvice(String systemPrompt, String negotiationPrompt, String regulatoryContext) {
        return chat(systemPrompt, negotiationPrompt, regulatoryContext);
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATUS
    // ══════════════════════════════════════════════════════════════════════

    public Map<String, Object> getFullStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", "NVIDIA");
        status.put("chatModel", model);
        status.put("chatAvailable", isAvailable());
        status.put("baseUrl", baseUrl);
        status.put("reasoningEnabled", enableThinking);
        return status;
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /**
     * Remove any reasoning/thinking traces that a reasoning model may emit
     * inline (e.g. &lt;think&gt;...&lt;/think&gt;) so the user sees only the answer.
     */
    private String stripReasoning(String content) {
        if (content == null) return "";
        String cleaned = content.replaceAll("(?s)<think>.*?</think>", "").trim();
        // Some models emit an unterminated leading think block.
        if (cleaned.contains("</think>")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf("</think>") + "</think>".length()).trim();
        }
        return cleaned.isBlank() ? content.trim() : cleaned;
    }

    private String safeErr(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        // Defensive: never echo a bearer token if it somehow appears in an error.
        return msg.replaceAll("nvapi-[A-Za-z0-9_\\-]+", "nvapi-***");
    }
}
