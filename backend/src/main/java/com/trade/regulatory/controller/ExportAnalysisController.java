package com.trade.regulatory.controller;

import com.trade.regulatory.service.RegulatoryRetrievalService;
import com.trade.regulatory.service.NvidiaAiService;
import com.trade.regulatory.service.MlExportRankingService;
import com.trade.regulatory.repository.HsMasterRepository;
import com.trade.regulatory.entity.HsMasterEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Export Analysis Controller.
 * Combines: confirmed Indian HS code + destination country → full regulatory analysis.
 * 
 * Flow:
 * 1. Validate Indian HS code exists in hs_master
 * 2. Retrieve destination-country regulatory data (hierarchical matching)
 * 3. Calculate compliance score
 * 4. Generate NVIDIA RAG explanation (if available)
 * 5. Return structured analysis result
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportAnalysisController {

    private final RegulatoryRetrievalService regulatoryService;
    private final NvidiaAiService aiService;
    private final HsMasterRepository hsMasterRepo;
    private final MlExportRankingService mlRankingService;

    /**
     * POST /api/v1/export/analyze
     * Full export analysis: product + origin HS + destination country → regulations + compliance + AI
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeExport(@RequestBody Map<String, Object> request) {
        String originCountry = (String) request.getOrDefault("originCountry", "India");
        String destinationCountry = (String) request.get("destinationCountry");
        String hsCode = (String) request.get("hsCode");
        String productName = (String) request.getOrDefault("productName", "");
        String category = (String) request.getOrDefault("category", "");
        String description = (String) request.getOrDefault("description", "");

        // Validation
        if (destinationCountry == null || destinationCountry.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "error", "Destination country is required."));
        }
        if (hsCode == null || hsCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "error", "Confirmed HS code is required."));
        }
        if (originCountry.equalsIgnoreCase(destinationCountry)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "error", "Destination country must be different from origin."));
        }

        // Step 1: Validate HS code exists in Indian hs_master
        Optional<HsMasterEntity> hsEntity = hsMasterRepo
                .findByCountryAndNationalCodeAndIsCurrentTrue("India", hsCode);
        String hsDescription = hsEntity.map(HsMasterEntity::getOfficialDescription)
                .orElse("HS code not found in Indian tariff schedule");
        boolean hsVerified = hsEntity.isPresent();

        // Step 2: Retrieve destination regulations using hierarchical matching
        RegulatoryRetrievalService.RegulatoryResult regResult =
                regulatoryService.getRegulations(destinationCountry, hsCode);

        // Step 3: Calculate compliance score
        RegulatoryRetrievalService.ComplianceScore compliance =
                regulatoryService.calculateCompliance(regResult);

        // Step 4: Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);

        // Product info
        response.put("product", Map.of(
                "name", productName,
                "category", category,
                "description", description
        ));

        // Origin
        response.put("origin", Map.of("country", originCountry));

        // Destination
        response.put("destination", Map.of("country", destinationCountry));

        // HS Classification
        Map<String, Object> hsInfo = new LinkedHashMap<>();
        hsInfo.put("hsCode", hsCode);
        hsInfo.put("description", hsDescription);
        hsInfo.put("verified", hsVerified);
        hsInfo.put("status", "USER_CONFIRMED");
        hsInfo.put("source", "TradeData.hs_master");
        response.put("hsClassification", hsInfo);

        // Compliance
        response.put("compliance", Map.of(
                "score", compliance.numericScore,
                "complexityLevel", compliance.complexity,
                "documentsCount", compliance.documentsCount,
                "certificationsCount", compliance.certificationsCount,
                "restrictionsCount", compliance.restrictionsCount,
                "regulationFound", compliance.regulationFound
        ));

        // ML export-opportunity prediction from the WINNING v4 model
        // (XGBRanker for ranking + XGBRegressor for calibrated value).
        // Replaces the previous heuristic opportunity score. DATA_UNAVAILABLE when
        // the product/destination is outside the model's trained scope (never fabricated).
        Map<String, Object> mlPrediction = mlRankingService.predictBlock(hsCode, destinationCountry);
        response.put("mlPrediction", mlPrediction);

        // Regulatory data
        response.put("matchType", regResult.matchType);
        response.put("confidence", regResult.confidence);
        response.put("regulationFound", regResult.regulationFound);

        // Regulations
        List<Map<String, Object>> regulations = new ArrayList<>();
        regResult.regulations.forEach(r -> regulations.add(Map.of(
                "title", r.getTitle() != null ? r.getTitle() : "",
                "authority", r.getAuthority() != null ? r.getAuthority() : "",
                "type", r.getRegulationType() != null ? r.getRegulationType() : ""
        )));
        response.put("regulations", regulations);

        // Documents
        List<Map<String, Object>> documents = new ArrayList<>();
        regResult.documents.forEach(d -> documents.add(Map.of(
                "name", d.getDocumentName(),
                "mandatory", d.getMandatory() != null ? d.getMandatory() : true
        )));
        response.put("documents", documents);

        // Certifications
        List<Map<String, Object>> certifications = new ArrayList<>();
        regResult.certifications.forEach(c -> certifications.add(Map.of(
                "name", c.getCertificationName(),
                "mandatory", c.getMandatory() != null ? c.getMandatory() : true
        )));
        response.put("certifications", certifications);

        // Labeling
        List<Map<String, Object>> labeling = new ArrayList<>();
        regResult.labeling.forEach(l -> labeling.add(Map.of(
                "requirement", l.getRequirement() != null ? l.getRequirement() : ""
        )));
        response.put("labelingRequirements", labeling);

        // Restrictions
        List<Map<String, Object>> restrictions = new ArrayList<>();
        regResult.restrictions.forEach(r -> restrictions.add(Map.of(
                "type", r.getRestrictionType() != null ? r.getRestrictionType() : "",
                "description", r.getDescription() != null ? r.getDescription() : ""
        )));
        response.put("restrictions", restrictions);

        // Procedures
        List<Map<String, Object>> procedures = new ArrayList<>();
        regResult.procedures.forEach(p -> procedures.add(Map.of(
                "name", p.getProcedureName() != null ? p.getProcedureName() : "",
                "description", p.getDescription() != null ? p.getDescription() : "",
                "stepOrder", p.getStepOrder() != null ? p.getStepOrder() : 0
        )));
        response.put("procedures", procedures);

        // Sources
        List<String> sources = new ArrayList<>();
        regResult.sources.forEach(s -> {
            if (s.getSourceUrl() != null) sources.add(s.getSourceUrl());
        });
        response.put("sources", sources);

        // Data availability
        response.put("dataAvailability", Map.of(
                "regulationsAvailable", !regResult.regulations.isEmpty(),
                "documentsAvailable", !regResult.documents.isEmpty(),
                "certificationsAvailable", !regResult.certifications.isEmpty(),
                "labelingAvailable", !regResult.labeling.isEmpty(),
                "restrictionsAvailable", !regResult.restrictions.isEmpty(),
                "proceduresAvailable", !regResult.procedures.isEmpty(),
                "sourcesAvailable", !regResult.sources.isEmpty()
        ));

        // Step 5: NVIDIA RAG explanation (if available)
        // If the DB has NO regulatory data for this product/destination, use AI to
        // generate the full compliance requirements (certificates, documents, etc.)
        Map<String, Object> aiExplanation = new LinkedHashMap<>();
        boolean dbHasData = regResult.regulationFound &&
                (!regResult.regulations.isEmpty() || !regResult.documents.isEmpty() || !regResult.certifications.isEmpty());

        if (aiService.isAvailable() && !dbHasData) {
            // AI COMPLIANCE GENERATION — no DB data available, so AI provides everything.
            try {
                log.info("No DB regulatory data for {} -> {} (HS {}); invoking AI compliance generation",
                        originCountry, destinationCountry, hsCode);

                String aiComplianceResponse = aiService.chat(
                    "You are an expert trade compliance advisor for Indian SME exporters. " +
                    "Generate the complete export compliance requirements for the given product and destination. " +
                    "IMPORTANT: Keep each section concise (max 5-6 items per category). " +
                    "You MUST respond in EXACTLY this JSON format with no other text:\n" +
                    "{\n" +
                    "  \"regulations\": [{\"title\": \"...\", \"authority\": \"...\", \"type\": \"IMPORT_REGULATION\", \"description\": \"brief\"}],\n" +
                    "  \"documents\": [{\"name\": \"...\", \"mandatory\": true, \"reason\": \"brief\", \"issuingAuthority\": \"...\"}],\n" +
                    "  \"certifications\": [{\"name\": \"...\", \"mandatory\": true, \"reason\": \"brief\", \"issuingAuthority\": \"...\"}],\n" +
                    "  \"labelingRequirements\": [{\"requirement\": \"...\", \"reason\": \"brief\"}],\n" +
                    "  \"restrictions\": [{\"type\": \"...\", \"description\": \"brief\"}],\n" +
                    "  \"procedures\": [{\"name\": \"...\", \"description\": \"brief\", \"stepOrder\": 1}],\n" +
                    "  \"exportGuidance\": \"brief 2-3 sentence summary\",\n" +
                    "  \"complianceScore\": 50,\n" +
                    "  \"complexityLevel\": \"MEDIUM\"\n" +
                    "}\n\n" +
                    "Rules:\n" +
                    "- Max 5 items per category. Be concise in descriptions (under 50 words each).\n" +
                    "- Only include items RELEVANT to this specific product type and destination.\n" +
                    "- complianceScore: 0-100 (100=easy, 0=very complex). complexityLevel: LOW/MEDIUM/HIGH/VERY_HIGH.\n" +
                    "- Do NOT invent non-existent regulations. Use standard international trade requirements.\n" +
                    "- Keep total response under 2000 tokens.",
                    String.format("Generate export compliance for:\nProduct: %s\nCategory: %s\nDescription: %s\nHS Code: %s\nOrigin: %s\nDestination: %s",
                        productName, category, description, hsCode, originCountry, destinationCountry),
                    ""
                );

                // Parse AI compliance response
                Map<String, Object> aiCompliance = parseAiComplianceResponse(aiComplianceResponse);

                if (aiCompliance != null && !aiCompliance.isEmpty()) {
                    // Override the empty DB results with AI-generated compliance data
                    if (aiCompliance.containsKey("regulations")) {
                        regulations.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiRegs = (List<Map<String, Object>>) aiCompliance.get("regulations");
                        if (aiRegs != null) regulations.addAll(aiRegs);
                    }
                    if (aiCompliance.containsKey("documents")) {
                        documents.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiDocs = (List<Map<String, Object>>) aiCompliance.get("documents");
                        if (aiDocs != null) documents.addAll(aiDocs);
                    }
                    if (aiCompliance.containsKey("certifications")) {
                        certifications.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiCerts = (List<Map<String, Object>>) aiCompliance.get("certifications");
                        if (aiCerts != null) certifications.addAll(aiCerts);
                    }
                    if (aiCompliance.containsKey("labelingRequirements")) {
                        labeling.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiLabeling = (List<Map<String, Object>>) aiCompliance.get("labelingRequirements");
                        if (aiLabeling != null) labeling.addAll(aiLabeling);
                    }
                    if (aiCompliance.containsKey("restrictions")) {
                        restrictions.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiRestrictions = (List<Map<String, Object>>) aiCompliance.get("restrictions");
                        if (aiRestrictions != null) restrictions.addAll(aiRestrictions);
                    }
                    if (aiCompliance.containsKey("procedures")) {
                        procedures.clear();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> aiProcedures = (List<Map<String, Object>>) aiCompliance.get("procedures");
                        if (aiProcedures != null) procedures.addAll(aiProcedures);
                    }

                    // Update the response with AI-generated data
                    response.put("regulations", regulations);
                    response.put("documents", documents);
                    response.put("certifications", certifications);
                    response.put("labelingRequirements", labeling);
                    response.put("restrictions", restrictions);
                    response.put("procedures", procedures);

                    // Update compliance score from AI
                    int aiCompScore = aiCompliance.containsKey("complianceScore")
                            ? ((Number) aiCompliance.get("complianceScore")).intValue() : 50;
                    String aiComplexity = aiCompliance.containsKey("complexityLevel")
                            ? String.valueOf(aiCompliance.get("complexityLevel")) : "MEDIUM";
                    response.put("compliance", Map.of(
                            "score", aiCompScore,
                            "complexityLevel", aiComplexity,
                            "documentsCount", documents.size(),
                            "certificationsCount", certifications.size(),
                            "restrictionsCount", restrictions.size(),
                            "regulationFound", true
                    ));

                    // Update data availability
                    response.put("dataAvailability", Map.of(
                            "regulationsAvailable", !regulations.isEmpty(),
                            "documentsAvailable", !documents.isEmpty(),
                            "certificationsAvailable", !certifications.isEmpty(),
                            "labelingAvailable", !labeling.isEmpty(),
                            "restrictionsAvailable", !restrictions.isEmpty(),
                            "proceduresAvailable", !procedures.isEmpty(),
                            "sourcesAvailable", false
                    ));
                    response.put("regulationFound", true);
                    response.put("dataSource", "AI_GENERATED");

                    aiExplanation.put("available", true);
                    aiExplanation.put("source", "AI_GENERATED");
                    aiExplanation.put("text", aiCompliance.getOrDefault("exportGuidance",
                            "AI-generated compliance requirements based on product attributes and destination country."));
                    aiExplanation.put("note", "These requirements are AI-generated because no structured regulatory data " +
                            "exists in the database for this HS code/destination combination. Verify against official sources before use.");
                } else {
                    aiExplanation.put("available", false);
                    aiExplanation.put("text", "AI compliance generation returned no usable data.");
                }
            } catch (Exception e) {
                log.warn("AI compliance generation failed: {}", e.getMessage());
                aiExplanation.put("available", false);
                aiExplanation.put("text", "AI compliance generation failed. No regulatory data available.");
            }
        } else if (aiService.isAvailable() && dbHasData) {
            // DB HAS data — use AI to explain/summarize existing regulatory data (existing flow)
            try {
                StringBuilder context = new StringBuilder();
                context.append("Product: ").append(productName).append("\n");
                context.append("HS Code: ").append(hsCode).append("\n");
                context.append("Origin: ").append(originCountry).append("\n");
                context.append("Destination: ").append(destinationCountry).append("\n");
                context.append("Match Type: ").append(regResult.matchType).append("\n\n");
                context.append("Regulations found: ").append(regulations.size()).append("\n");
                context.append("Documents required: ").append(documents.size()).append("\n");
                context.append("Certifications required: ").append(certifications.size()).append("\n");
                context.append("Restrictions: ").append(restrictions.size()).append("\n");
                context.append("Compliance Score: ").append(compliance.numericScore).append("/100\n");
                context.append("Complexity: ").append(compliance.complexity).append("\n");

                if (!regulations.isEmpty()) {
                    context.append("\nRegulation details:\n");
                    regulations.forEach(r -> context.append("- ").append(r.get("title")).append(" (").append(r.get("authority")).append(")\n"));
                }
                if (!documents.isEmpty()) {
                    context.append("\nRequired documents:\n");
                    documents.forEach(d -> context.append("- ").append(d.get("name")).append(d.get("mandatory").equals(true) ? " [MANDATORY]" : " [Optional]").append("\n"));
                }

                String aiResponse = aiService.chat(
                        "You are a trade compliance advisor. Summarize the export requirements based on the provided regulatory evidence. " +
                        "Do NOT invent requirements not present in the evidence. If data is limited, say so clearly.",
                        "Summarize the export requirements for this product going from " + originCountry + " to " + destinationCountry + ".",
                        context.toString()
                );
                aiExplanation.put("available", true);
                aiExplanation.put("source", "DB_GROUNDED");
                aiExplanation.put("text", aiResponse);
            } catch (Exception e) {
                log.warn("AI explanation failed: {}", e.getMessage());
                aiExplanation.put("available", false);
                aiExplanation.put("text", "AI explanation temporarily unavailable.");
            }
        } else {
            aiExplanation.put("available", false);
            aiExplanation.put("text", aiService.isAvailable() ?
                    "No regulatory evidence available for AI analysis." :
                    "NVIDIA AI service not configured.");
        }
        response.put("aiExplanation", aiExplanation);

        log.info("Export analysis: {} -> {} (HS: {}) | compliance={} | regs={} | docs={} | certs={}",
                originCountry, destinationCountry, hsCode,
                compliance.numericScore, regulations.size(), documents.size(), certifications.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Parse the AI-generated compliance JSON response.
     * Tolerant of markdown wrapping, partial JSON, and missing fields.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiComplianceResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) return null;
        try {
            String json = aiResponse.trim();
            // Strip markdown code fences if present
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                if (json.contains("```")) json = json.substring(0, json.lastIndexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                if (json.contains("```")) json = json.substring(0, json.lastIndexOf("```"));
            }
            // Find the JSON object
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            } else if (start >= 0) {
                // Truncated JSON — try to close it
                json = json.substring(start);
                json = repairTruncatedJson(json);
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI compliance JSON: {}", e.getMessage());
            // Try a more aggressive repair
            try {
                String repaired = repairTruncatedJson(aiResponse);
                if (repaired != null) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
                    return mapper.readValue(repaired, Map.class);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Attempt to repair truncated JSON by closing open arrays and objects.
     */
    private String repairTruncatedJson(String json) {
        if (json == null) return null;
        // Find the first { 
        int start = json.indexOf('{');
        if (start < 0) return null;
        json = json.substring(start);

        // Remove any trailing incomplete value (truncated string, etc.)
        // Find the last complete key-value pair
        int lastComma = json.lastIndexOf(',');
        int lastBrace = Math.max(json.lastIndexOf('}'), json.lastIndexOf(']'));
        if (lastComma > lastBrace) {
            // Truncated after a comma — remove the incomplete part
            json = json.substring(0, lastComma);
        }

        // Count open brackets and close them
        int openBraces = 0, openBrackets = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == '{') openBraces++;
                else if (c == '}') openBraces--;
                else if (c == '[') openBrackets++;
                else if (c == ']') openBrackets--;
            }
        }
        StringBuilder sb = new StringBuilder(json);
        while (openBrackets > 0) { sb.append(']'); openBrackets--; }
        while (openBraces > 0) { sb.append('}'); openBraces--; }
        return sb.toString();
    }
}
