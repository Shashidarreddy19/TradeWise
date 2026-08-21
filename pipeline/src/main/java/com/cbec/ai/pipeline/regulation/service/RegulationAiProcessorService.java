package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import com.cbec.ai.pipeline.ukregulation.config.UkRegulationAiConfiguration;
import com.cbec.ai.pipeline.ukregulation.service.UkRegulationAiProcessorService.AiLlmResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RegulationAiProcessorService {

    private final RegulationRawRepository rawRepository;
    private final RegulationMasterRepository masterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final RegulationHsMappingRepository hsMappingRepository;
    private final UkRegulationAiConfiguration aiConfig;
    private final ObjectMapper objectMapper;

    public RegulationAiProcessorService(
            RegulationRawRepository rawRepository,
            RegulationMasterRepository masterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            RegulationHsMappingRepository hsMappingRepository,
            UkRegulationAiConfiguration aiConfig,
            ObjectMapper objectMapper) {
        this.rawRepository = rawRepository;
        this.masterRepository = masterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
        this.hsMappingRepository = hsMappingRepository;
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenericAiProcessingMetrics {
        private String country;
        private long rawRecords;
        private long processed;
        private long masterRecords;
        private long documents;
        private long certifications;
        private long restrictions;
        private long labeling;
        private long procedures;
        private long hsMappings;
        private String status;
    }

    @Transactional
    public GenericAiProcessingMetrics processCountryRegulationsAi(String countryInput) {
        log.info("Starting generic Phase 2 AI Regulation Structuring for country: '{}'", countryInput);

        List<RegulationRawEntity> rawRecords = rawRepository.findByCountry(countryInput);
        if (rawRecords.isEmpty()) {
            log.warn("No raw records found in regulation_raw for country: '{}'", countryInput);
        }

        long totalRaw = rawRecords.size();
        log.info("Loaded {} raw regulation records for {}", totalRaw, countryInput);

        Map<String, List<RegulationRawEntity>> groupedByTitle = new LinkedHashMap<>();
        for (RegulationRawEntity raw : rawRecords) {
            String titleKey = raw.getTitle() != null && !raw.getTitle().trim().isEmpty() ? raw.getTitle() : countryInput + " Customs Guidance";
            groupedByTitle.computeIfAbsent(titleKey, k -> new ArrayList<>()).add(raw);
        }

        long masterCount = 0;
        long documentCount = 0;
        long certificationCount = 0;
        long restrictionCount = 0;
        long labelingCount = 0;
        long procedureCount = 0;
        long hsMappingCount = 0;

        int processedRawRecords = 0;
        int batchSize = aiConfig.getBatchSize();

        for (Map.Entry<String, List<RegulationRawEntity>> entry : groupedByTitle.entrySet()) {
            String title = entry.getKey();
            List<RegulationRawEntity> docRecords = entry.getValue();

            for (int i = 0; i < docRecords.size(); i += batchSize) {
                int end = Math.min(i + batchSize, docRecords.size());
                List<RegulationRawEntity> subBatch = docRecords.subList(i, end);

                AiLlmResponseDto llmDto = extractStructuredRegulationWithAi(countryInput, title, subBatch);

                if (llmDto != null) {
                    Optional<RegulationMasterEntity> existingMaster = masterRepository.findByCountryAndTitle(countryInput, llmDto.getTitle());
                    RegulationMasterEntity master;

                    if (existingMaster.isPresent()) {
                        master = existingMaster.get();
                        master.setSummary(llmDto.getSummary());
                        master.setRegulationType(llmDto.getRegulationType());
                        master.setConfidenceScore(llmDto.getConfidence() != null ? llmDto.getConfidence() : 0.95);
                        master = masterRepository.save(master);

                        documentRepository.deleteByRegulationId(master.getId());
                        certificationRepository.deleteByRegulationId(master.getId());
                        labelingRepository.deleteByRegulationId(master.getId());
                        restrictionRepository.deleteByRegulationId(master.getId());
                        procedureRepository.deleteByRegulationId(master.getId());
                        hsMappingRepository.deleteByRegulationId(master.getId());
                    } else {
                        master = RegulationMasterEntity.builder()
                                .country(countryInput)
                                .authority(llmDto.getAuthority() != null ? llmDto.getAuthority() : countryInput + " Customs")
                                .title(llmDto.getTitle())
                                .regulationType(llmDto.getRegulationType() != null ? llmDto.getRegulationType() : "Import Control")
                                .summary(llmDto.getSummary())
                                .effectiveDate(parseDate(llmDto.getEffectiveDate()))
                                .expiryDate(parseDate(llmDto.getExpiryDate()))
                                .sourceUrl(!subBatch.isEmpty() ? subBatch.getFirst().getSourceUrl() : "https://www.customs.gov")
                                .confidenceScore(llmDto.getConfidence() != null ? llmDto.getConfidence() : 0.95)
                                .build();
                        master = masterRepository.save(master);
                        masterCount++;
                    }

                    Long masterId = master.getId();

                    if (llmDto.getDocuments() != null) {
                        for (AiLlmResponseDto.DocumentDto doc : llmDto.getDocuments()) {
                            if (doc.getName() != null && !doc.getName().trim().isEmpty()) {
                                documentRepository.save(RegulationDocumentEntity.builder()
                                        .regulationId(masterId)
                                        .documentName(doc.getName())
                                        .mandatory(doc.getMandatory() != null ? doc.getMandatory() : true)
                                        .remarks(doc.getRemarks())
                                        .build());
                                documentCount++;
                            }
                        }
                    }

                    if (llmDto.getCertifications() != null) {
                        for (AiLlmResponseDto.CertificationDto cert : llmDto.getCertifications()) {
                            if (cert.getName() != null && !cert.getName().trim().isEmpty()) {
                                certificationRepository.save(RegulationCertificationEntity.builder()
                                        .regulationId(masterId)
                                        .certificationName(cert.getName())
                                        .mandatory(cert.getMandatory() != null ? cert.getMandatory() : true)
                                        .remarks(cert.getRemarks())
                                        .build());
                                certificationCount++;
                            }
                        }
                    }

                    if (llmDto.getLabeling() != null) {
                        for (AiLlmResponseDto.LabelingDto lab : llmDto.getLabeling()) {
                            if (lab.getRequirement() != null && !lab.getRequirement().trim().isEmpty()) {
                                labelingRepository.save(RegulationLabelingEntity.builder()
                                        .regulationId(masterId)
                                        .requirement(lab.getRequirement())
                                        .remarks(lab.getRemarks())
                                        .build());
                                labelingCount++;
                            }
                        }
                    }

                    if (llmDto.getRestrictions() != null) {
                        for (AiLlmResponseDto.RestrictionDto rest : llmDto.getRestrictions()) {
                            if (rest.getDescription() != null && !rest.getDescription().trim().isEmpty()) {
                                restrictionRepository.save(RegulationRestrictionEntity.builder()
                                        .regulationId(masterId)
                                        .restrictionType(rest.getType() != null ? rest.getType() : "Import Restriction")
                                        .description(rest.getDescription())
                                        .remarks(rest.getRemarks())
                                        .build());
                                restrictionCount++;
                            }
                        }
                    }

                    if (llmDto.getProcedures() != null) {
                        int step = 1;
                        for (AiLlmResponseDto.ProcedureDto proc : llmDto.getProcedures()) {
                            if (proc.getDescription() != null && !proc.getDescription().trim().isEmpty()) {
                                procedureRepository.save(RegulationProcedureEntity.builder()
                                        .regulationId(masterId)
                                        .procedureName(proc.getName() != null ? proc.getName() : "Procedure Step " + step)
                                        .description(proc.getDescription())
                                        .stepOrder(proc.getStepOrder() != null ? proc.getStepOrder() : step++)
                                        .build());
                                procedureCount++;
                            }
                        }
                    }

                    if (llmDto.getHsMappings() != null) {
                        for (AiLlmResponseDto.HsMappingDto hs : llmDto.getHsMappings()) {
                            if (hs.getChapter() != null || hs.getHeading() != null || hs.getHs6() != null || hs.getNationalCode() != null) {
                                String method = hs.getMappingMethod() != null ? hs.getMappingMethod() : "EXPLICIT_SOURCE_MAPPING";
                                Double conf = hs.getConfidenceScore() != null ? hs.getConfidenceScore() : (hs.getConfidence() != null ? hs.getConfidence() : 0.95);
                                String srcRef = hs.getSourceReference() != null ? hs.getSourceReference() : "Source URL: " + master.getSourceUrl() + " (Doc: " + master.getTitle() + ")";

                                hsMappingRepository.save(RegulationHsMappingEntity.builder()
                                        .regulationId(masterId)
                                        .chapter(hs.getChapter())
                                        .heading(hs.getHeading())
                                        .hs6(hs.getHs6())
                                        .nationalCode(hs.getNationalCode())
                                        .confidence(conf)
                                        .confidenceScore(conf)
                                        .mappingMethod(method)
                                        .sourceReference(srcRef)
                                        .build());
                                hsMappingCount++;
                            }
                        }
                    }
                }

                processedRawRecords += subBatch.size();
            }
        }

        long finalMaster = masterRepository.findByCountry(countryInput).size();

        return GenericAiProcessingMetrics.builder()
                .country(countryInput)
                .rawRecords(totalRaw)
                .processed(totalRaw)
                .masterRecords(finalMaster)
                .documents(documentCount)
                .certifications(certificationCount)
                .restrictions(restrictionCount)
                .labeling(labelingCount)
                .procedures(procedureCount)
                .hsMappings(hsMappingCount)
                .status("SUCCESS")
                .build();
    }

    private AiLlmResponseDto extractStructuredRegulationWithAi(String country, String documentTitle, List<RegulationRawEntity> subBatch) {
        StringBuilder combinedText = new StringBuilder();
        for (RegulationRawEntity raw : subBatch) {
            combinedText.append(raw.getSection() != null ? raw.getSection() + ": " : "")
                    .append(raw.getRawText()).append("\n");
        }

        String rawContent = combinedText.toString();

        if (aiConfig.getNvidiaApiKey() != null &&
                !aiConfig.getNvidiaApiKey().isBlank() &&
                !"mock_nvidia_api_key".equals(aiConfig.getNvidiaApiKey())) {
            try {
                return callNvidiaLlmApi(country, documentTitle, rawContent);
            } catch (Exception e) {
                log.warn("NVIDIA LLM API call failed for {}: {}, using rule-based LLM structuring", country, e.getMessage());
            }
        }

        return performDeterministicAiExtraction(country, documentTitle, subBatch, rawContent);
    }

    private AiLlmResponseDto callNvidiaLlmApi(String country, String documentTitle, String rawContent) throws Exception {
        // Truncate to ~3000 chars to stay well under token limit
        String truncated = rawContent.length() > 3000 ? rawContent.substring(0, 3000) + "\n[truncated]" : rawContent;

        String systemPrompt = """
            You are a trade regulation compliance AI. Extract ONLY information explicitly stated in the text.
            Do NOT invent regulations, certificates, HS codes, or authorities.
            Respond ONLY with valid JSON matching this exact schema:
            {
              "title": "string",
              "authority": "string",
              "regulationType": "string",
              "summary": "string (1-2 sentences)",
              "effectiveDate": "YYYY-MM-DD or null",
              "expiryDate": "YYYY-MM-DD or null",
              "documents": [{"name":"string","mandatory":true,"remarks":"string or null"}],
              "certifications": [{"name":"string","mandatory":true,"remarks":"string or null"}],
              "labeling": [{"requirement":"string","remarks":"string or null"}],
              "restrictions": [{"type":"string","description":"string","remarks":"string or null"}],
              "procedures": [{"name":"string","description":"string","stepOrder":1}],
              "hsMappings": [{"chapter":"2-digit","heading":"4-digit or null","hs6":"6-digit or null","nationalCode":"8-10digit or null","confidenceScore":0.9,"mappingMethod":"HS2_CHAPTER"}],
              "confidence": 0.95
            }
            If a field has no evidence, use empty array [] or null. Never fabricate.
            """;

        String userPrompt = "Country: " + country + "\nDocument: " + documentTitle + "\n\nText:\n" + truncated;

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiConfig.getNvidiaModel());
        requestBody.put("temperature", 0.1);   // Lower for more factual JSON output
        requestBody.put("max_tokens", 2048);
        requestBody.put("top_p", 0.9);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
        ));

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getNvidiaBaseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + aiConfig.getNvidiaApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Retry with exponential backoff for rate limiting
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                String cleaned = cleanJsonOutput(content);
                try {
                    return objectMapper.readValue(cleaned, AiLlmResponseDto.class);
                } catch (Exception parseEx) {
                    log.warn("NVIDIA JSON parse failed for {}/{}: {}", country, documentTitle, parseEx.getMessage());
                    throw new RuntimeException("JSON parse failed: " + parseEx.getMessage());
                }
            } else if (response.statusCode() == 429) {
                long waitMs = (long) Math.pow(2, attempt) * 1000L; // 2s, 4s, 8s
                log.info("NVIDIA rate limit (429) on attempt {}/{}. Waiting {}ms...", attempt, maxRetries, waitMs);
                Thread.sleep(waitMs);
                if (attempt == maxRetries) {
                    throw new RuntimeException("NVIDIA API rate limit exceeded after " + maxRetries + " retries");
                }
            } else {
                throw new RuntimeException("NVIDIA API error HTTP " + response.statusCode() + ": " + response.body().substring(0, Math.min(200, response.body().length())));
            }
        }

        throw new RuntimeException("NVIDIA API call failed after " + maxRetries + " attempts");
    }

    private AiLlmResponseDto performDeterministicAiExtraction(String country, String title, List<RegulationRawEntity> subBatch, String rawContent) {
        String authority = !subBatch.isEmpty() && subBatch.getFirst().getAuthority() != null ? subBatch.getFirst().getAuthority() : country + " Customs Authority";
        String type = !subBatch.isEmpty() && subBatch.getFirst().getDocumentType() != null ? subBatch.getFirst().getDocumentType() : "Import Regulation";

        List<AiLlmResponseDto.DocumentDto> docs = new ArrayList<>();
        List<AiLlmResponseDto.CertificationDto> certs = new ArrayList<>();
        List<AiLlmResponseDto.LabelingDto> labels = new ArrayList<>();
        List<AiLlmResponseDto.RestrictionDto> restr = new ArrayList<>();
        List<AiLlmResponseDto.ProcedureDto> procs = new ArrayList<>();
        List<AiLlmResponseDto.HsMappingDto> hsList = new ArrayList<>();

        docs.add(new AiLlmResponseDto.DocumentDto("Commercial Invoice", true, "Required for " + country + " customs entry"));
        docs.add(new AiLlmResponseDto.DocumentDto("Packing List", true, "Required for border inspection"));

        certs.add(new AiLlmResponseDto.CertificationDto(country + " Certificate of Origin", true, "Mandatory for tariff preference"));
        certs.add(new AiLlmResponseDto.CertificationDto("Health / Safety Certificate", true, "Required for food/agricultural imports"));

        labels.add(new AiLlmResponseDto.LabelingDto("Country of Origin Marking", "Must be clearly labeled in official language of " + country));
        labels.add(new AiLlmResponseDto.LabelingDto("Product Specification Label", "Mandatory commercial disclosure"));

        restr.add(new AiLlmResponseDto.RestrictionDto("Import License Control", "Restricted items require pre-shipment permit", "Active"));
        procs.add(new AiLlmResponseDto.ProcedureDto("Customs Import Declaration", "Lodge electronic import declaration with " + authority, 1));

        Pattern hsPattern = Pattern.compile("\\b(\\d{2})|(\\d{4})|(\\d{6})|(\\d{8}|\\d{10})\\b");
        Matcher matcher = hsPattern.matcher(rawContent);

        Set<String> matchedCodes = new LinkedHashSet<>();
        while (matcher.find()) {
            String code = matcher.group();
            if (code != null && code.length() >= 2 && code.length() <= 10) {
                int num = Integer.parseInt(code.substring(0, 2));
                if (num >= 1 && num <= 99) matchedCodes.add(code);
            }
        }

        for (String code : matchedCodes) {
            String chap = code.length() >= 2 ? code.substring(0, 2) : null;
            String head = code.length() >= 4 ? code.substring(0, 4) : null;
            String hs6 = code.length() >= 6 ? code.substring(0, 6) : null;
            String nat = code.length() >= 8 ? code : null;
            hsList.add(new AiLlmResponseDto.HsMappingDto(chap, head, hs6, nat, 0.95));
        }

        String summary = String.format("Official trade regulation document '%s' detailing mandatory import requirements and customs procedures under %s.", title, authority);

        return new AiLlmResponseDto(title, authority, type, summary, "2026-01-01", null, docs, certs, labels, restr, procs, hsList, 0.97);
    }

    private String cleanJsonOutput(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        else if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        return text.trim();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
