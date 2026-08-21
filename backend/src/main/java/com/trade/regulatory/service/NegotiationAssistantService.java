package com.trade.regulatory.service;

import com.trade.regulatory.service.HybridRetrievalService.HybridContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NVIDIA-powered negotiation assistant.
 *
 * Flow (spec section 11): structured regulatory retrieval + semantic document
 * retrieval + rerank (via HybridRetrievalService) -> Nemotron-3-Ultra 550B.
 * NEVER invents legal obligations — regulatory facts come from retrieved context.
 */
@Service
public class NegotiationAssistantService {

    private final NvidiaAiService aiService;
    private final HybridRetrievalService hybridRetrievalService;

    public NegotiationAssistantService(NvidiaAiService aiService,
                                       HybridRetrievalService hybridRetrievalService) {
        this.aiService = aiService;
        this.hybridRetrievalService = hybridRetrievalService;
    }

    public Map<String, Object> generateNegotiationAdvice(Map<String, String> input) {
        String product = input.getOrDefault("product", "");
        String hsCode = input.getOrDefault("hsCode", "");
        String country = input.getOrDefault("country", "");
        String buyerMessage = input.getOrDefault("buyerMessage", "");
        String objective = input.getOrDefault("objective", "");
        String quantity = input.getOrDefault("quantity", "");
        String targetPrice = input.getOrDefault("targetPrice", "");

        if (!aiService.isAvailable()) {
            return Map.of(
                    "error", "AI service not configured. Set NVIDIA_API_KEY.",
                    "available", false
            );
        }

        // 1. Hybrid retrieval for grounding (structured + semantic, reranked).
        String context = "";
        boolean usedSemantic = false;
        if (!country.isBlank() && !hsCode.isBlank()) {
            String retrievalQuery = (objective.isBlank() ? "export compliance" : objective)
                    + " " + product + " to " + country;
            HybridContext hybrid = hybridRetrievalService.retrieve(country, hsCode, retrievalQuery);
            context = hybrid.contextText;
            usedSemantic = hybrid.usedSemantic;
        }

        // 2. Build negotiation prompt.
        String prompt = String.format("""
            Help an Indian SME exporter negotiate with an international buyer.

            PRODUCT: %s (HS: %s)
            DESTINATION: %s
            QUANTITY: %s
            TARGET PRICE: %s
            EXPORTER'S OBJECTIVE: %s

            BUYER'S MESSAGE:
            %s

            Provide:
            1. Analysis of buyer's position
            2. Suggested response (professional, concise)
            3. Key negotiation points
            4. Compliance considerations from the regulatory context
            5. Risks to be aware of
            6. Questions to ask the buyer
            7. Recommended fallback position

            Do NOT invent legal or regulatory requirements. Only reference compliance
            points that appear in the provided context.
            """, product, hsCode, country, quantity, targetPrice, objective, buyerMessage);

        // 3. Grounded generation via the main LLM.
        String response = aiService.generateNegotiationAdvice(
                "You are an expert international trade negotiation advisor for Indian SME exporters. " +
                "Provide practical, actionable negotiation advice. Be professional and culturally sensitive.",
                prompt, context
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("advice", response);
        result.put("product", product);
        result.put("country", country);
        result.put("hsCode", hsCode);
        result.put("usedSemanticRag", usedSemantic);
        result.put("aiAvailable", true);
        return result;
    }
}
