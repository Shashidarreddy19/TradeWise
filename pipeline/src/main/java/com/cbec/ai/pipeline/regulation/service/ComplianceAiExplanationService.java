package com.cbec.ai.pipeline.regulation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComplianceAiExplanationService {

    @Value("${nvidia.api.key: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}")
    private String nvidiaApiKey;

    @Value("${nvidia.api.url:https://integrate.api.nvidia.com/v1/chat/completions}")
    private String nvidiaApiUrl;

    @Value("${nvidia.api.model:nvidia/nemotron-3-ultra-550b-a55b}")
    private String nvidiaModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequestDto {
        private String country;
        private String hsCode;
        private String question;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponseDto {
        private String country;
        private String hsCode;
        private String question;
        private String answer;
        private List<ComplianceRetrievalService.EvidenceItemDto> evidenceReferences;
    }

    public String generateComplianceExplanation(ComplianceRetrievalService.ComplianceDataBundleDto bundle) {
        if (bundle == null || bundle.getEvidence() == null || bundle.getEvidence().isEmpty()) {
            return "NOT_FOUND_IN_SOURCE";
        }

        String prompt = buildExplanationPrompt(bundle);
        return callNvidiaNemotron(prompt, bundle.getEvidence());
    }

    public ChatResponseDto answerComplianceQuestion(ChatRequestDto request, ComplianceRetrievalService.ComplianceDataBundleDto bundle) {
        String country = request.getCountry();
        String hsCode = request.getHsCode();
        String question = request.getQuestion();

        if (bundle == null || bundle.getEvidence() == null || bundle.getEvidence().isEmpty()) {
            return ChatResponseDto.builder()
                    .country(country)
                    .hsCode(hsCode)
                    .question(question)
                    .answer("NOT_FOUND_IN_SOURCE")
                    .evidenceReferences(Collections.emptyList())
                    .build();
        }

        String prompt = buildChatPrompt(question, bundle);
        String answer = callNvidiaNemotron(prompt, bundle.getEvidence());

        return ChatResponseDto.builder()
                .country(country)
                .hsCode(hsCode)
                .question(question)
                .answer(answer)
                .evidenceReferences(bundle.getEvidence())
                .build();
    }

    private String buildExplanationPrompt(ComplianceRetrievalService.ComplianceDataBundleDto bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarize and explain the regulatory requirements for exporting/importing product under HS Code ")
                .append(bundle.getHsCode())
                .append(" (").append(bundle.getProductDescription()).append(") in ")
                .append(bundle.getCountry()).append(".\n\n")
                .append("RETRIEVED DATABASE EVIDENCE:\n");

        for (int i = 0; i < bundle.getEvidence().size(); i++) {
            ComplianceRetrievalService.EvidenceItemDto ev = bundle.getEvidence().get(i);
            sb.append(i + 1).append(". [").append(ev.getRequirementType()).append("] ")
                    .append(ev.getRequirement()).append(" | Source: ").append(ev.getSourceTitle())
                    .append(" (Authority: ").append(ev.getAuthority()).append(")\n");
        }

        sb.append("\nInstruction: Provide a concise executive explanation covering mandatory documents, certifications, labeling, restrictions, and clearance steps based ONLY on the evidence above.");
        return sb.toString();
    }

    private String buildChatPrompt(String question, ComplianceRetrievalService.ComplianceDataBundleDto bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("User Question: ").append(question).append("\n\n")
                .append("Target Market: ").append(bundle.getCountry()).append("\n")
                .append("HS Commodity Code: ").append(bundle.getHsCode()).append("\n")
                .append("Product Description: ").append(bundle.getProductDescription()).append("\n\n")
                .append("RETRIEVED DATABASE EVIDENCE:\n");

        for (int i = 0; i < bundle.getEvidence().size(); i++) {
            ComplianceRetrievalService.EvidenceItemDto ev = bundle.getEvidence().get(i);
            sb.append(i + 1).append(". [").append(ev.getRequirementType()).append("] ")
                    .append(ev.getRequirement()).append(" | Citation: ").append(ev.getSourceReference())
                    .append(" (").append(ev.getSourceUrl()).append(")\n");
        }

        sb.append("\nInstruction: Answer the user question accurately using ONLY the evidence provided above. Attach citation references.");
        return sb.toString();
    }

    private String callNvidiaNemotron(String userPrompt, List<ComplianceRetrievalService.EvidenceItemDto> evidence) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(nvidiaApiKey);

            Map<String, Object> systemMessage = Map.of(
                    "role", "system",
                    "content", "You are CBEC-AI Principal Trade Compliance Engineer. Answer using ONLY the retrieved database evidence provided. Never hallucinate or invent regulatory requirements. If the evidence does not contain the answer, respond with exact token: NOT_FOUND_IN_SOURCE."
            );

            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", userPrompt
            );

            Map<String, Object> requestBody = Map.of(
                    "model", nvidiaModel,
                    "messages", List.of(systemMessage, userMessage),
                    "temperature", 0.2,
                    "top_p", 0.95,
                    "max_tokens", 1024
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(nvidiaApiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                if (!contentNode.isMissingNode() && !contentNode.asText().isEmpty()) {
                    return contentNode.asText().trim();
                }
            }
        } catch (Exception e) {
            log.warn("NVIDIA Nemotron 3 Ultra call fallback ({}): using evidence-grounded rule synthesis", e.getMessage());
        }

        // Fallback: Rule-based evidence synthesis grounded strictly on retrieved DB evidence
        return buildRuleBasedEvidenceExplanation(evidence);
    }

    private String buildRuleBasedEvidenceExplanation(List<ComplianceRetrievalService.EvidenceItemDto> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "NOT_FOUND_IN_SOURCE";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Official Regulatory Requirements Summary:\n");
        for (ComplianceRetrievalService.EvidenceItemDto ev : evidence) {
            sb.append("• [").append(ev.getRequirementType()).append("] ")
                    .append(ev.getRequirement()).append(" (Source: ").append(ev.getAuthority()).append(" - ").append(ev.getSourceReference()).append(")\n");
        }
        return sb.toString().trim();
    }
}
