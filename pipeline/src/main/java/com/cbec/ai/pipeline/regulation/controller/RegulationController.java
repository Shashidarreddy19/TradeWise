package com.cbec.ai.pipeline.regulation.controller;

import com.cbec.ai.pipeline.regulation.service.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class RegulationController {

    private final RegulationPipelineService pipelineService;
    private final RegulationAuditService auditService;
    private final ComplianceRetrievalService retrievalService;
    private final ComplianceScoringService scoringService;
    private final ComplianceAiExplanationService aiExplanationService;
    private final ComplianceGuidanceService guidanceService;
    private final ComplianceComparisonService comparisonService;
    private final ComplianceFreshnessService freshnessService;
    private final ComplianceDataQualityService dataQualityService;
    private final GlobalCoverageAuditService globalCoverageAuditService;
    private final GlobalDatabaseSeedService databaseSeedService;
    private final HsMasterRepository hsMasterRepository;
    private final RegulationMasterRepository masterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;

    public RegulationController(
            RegulationPipelineService pipelineService,
            RegulationAuditService auditService,
            ComplianceRetrievalService retrievalService,
            ComplianceScoringService scoringService,
            ComplianceAiExplanationService aiExplanationService,
            ComplianceGuidanceService guidanceService,
            ComplianceComparisonService comparisonService,
            ComplianceFreshnessService freshnessService,
            ComplianceDataQualityService dataQualityService,
            GlobalCoverageAuditService globalCoverageAuditService,
            GlobalDatabaseSeedService databaseSeedService,
            HsMasterRepository hsMasterRepository,
            RegulationMasterRepository masterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository) {
        this.pipelineService = pipelineService;
        this.auditService = auditService;
        this.retrievalService = retrievalService;
        this.scoringService = scoringService;
        this.aiExplanationService = aiExplanationService;
        this.guidanceService = guidanceService;
        this.comparisonService = comparisonService;
        this.freshnessService = freshnessService;
        this.dataQualityService = dataQualityService;
        this.globalCoverageAuditService = globalCoverageAuditService;
        this.databaseSeedService = databaseSeedService;
        this.hsMasterRepository = hsMasterRepository;
        this.masterRepository = masterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryPipelineRequest {
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheckRequest {
        private String country;
        private String hsCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HsMatchInfo {
        private String type;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceDto {
        private String requirement;
        private String requirementType;
        private String authority;
        private String sourceTitle;
        private String sourceUrl;
        private String sourceReference;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullComplianceResponseDto {
        private String country;
        private String hsCode;
        private String productDescription;
        private Integer complianceScore;
        private String complexityLevel;
        private List<ComplianceRetrievalService.EvidenceItemDto> evidence;
        private List<Object> documents;
        private List<Object> certifications;
        private List<Object> labelingRequirements;
        private List<Object> restrictions;
        private List<Object> procedures;
        private HsMatchInfo hsMatch;
        private String aiExplanation;
    }

    /**
     * Reusable Generic Regulation Pipeline Endpoint for any country.
     */
    @PostMapping("/pipeline/process-regulations")
    public ResponseEntity<RegulationAiProcessorService.GenericAiProcessingMetrics> processRegulations(
            @RequestBody(required = false) CountryPipelineRequest request,
            @RequestParam(name = "country", required = false) String countryParam) {

        String targetCountry = "United Kingdom";
        if (request != null && request.getCountry() != null && !request.getCountry().isBlank()) {
            targetCountry = request.getCountry();
        } else if (countryParam != null && !countryParam.isBlank()) {
            targetCountry = countryParam;
        }

        return ResponseEntity.ok(pipelineService.processRegulationsForCountry(targetCountry));
    }

    /**
     * Global Database Audit Service & Report
     * GET /api/v1/pipeline/audit-regulations
     */
    @GetMapping("/pipeline/audit-regulations")
    public ResponseEntity<RegulationAuditService.GlobalRegulationAuditReportDto> auditRegulations() {
        return ResponseEntity.ok(auditService.generateAuditReport());
    }

    /**
     * Phase 4: Production Evidence-Backed Compliance Query Engine
     * POST /api/v1/compliance/check
     */
    @PostMapping("/compliance/check")
    public ResponseEntity<FullComplianceResponseDto> checkCompliance(
            @RequestBody ComplianceCheckRequest request) {

        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(request.getCountry(), request.getHsCode());
        ComplianceScoringService.ScoringResultDto scoring = scoringService.calculateComplianceScore(bundle);
        String aiExplanation = aiExplanationService.generateComplianceExplanation(bundle);

        HsMatchInfo hsMatchInfo = HsMatchInfo.builder()
                .type(bundle.getHsMatch() != null ? bundle.getHsMatch().getMatchType() : "EXACT_NATIONAL_CODE")
                .confidence(bundle.getHsMatch() != null ? bundle.getHsMatch().getConfidence() : 0.95)
                .build();

        FullComplianceResponseDto response = FullComplianceResponseDto.builder()
                .country(bundle.getCountry())
                .hsCode(bundle.getHsCode())
                .productDescription(bundle.getProductDescription())
                .complianceScore(scoring.getScore())
                .complexityLevel(scoring.getLevel())
                .documents((List) bundle.getDocuments())
                .certifications((List) bundle.getCertifications())
                .labelingRequirements((List) bundle.getLabelingRequirements())
                .restrictions((List) bundle.getRestrictions())
                .procedures((List) bundle.getProcedures())
                .hsMatch(hsMatchInfo)
                .evidence(bundle.getEvidence())
                .aiExplanation(aiExplanation)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Phase 6: AI Chat Assistant
     * POST /api/v1/compliance/chat
     */
    @PostMapping("/compliance/chat")
    public ResponseEntity<ComplianceAiExplanationService.ChatResponseDto> chatCompliance(
            @RequestBody ComplianceAiExplanationService.ChatRequestDto request) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(request.getCountry(), request.getHsCode());
        return ResponseEntity.ok(aiExplanationService.answerComplianceQuestion(request, bundle));
    }

    /**
     * Phase 7: Step-by-Step Export Guidance Workflow
     * GET /api/v1/compliance/guidance
     */
    @GetMapping("/compliance/guidance")
    public ResponseEntity<ComplianceGuidanceService.ExportGuidanceResponseDto> getGuidance(
            @RequestParam("country") String country,
            @RequestParam("hsCode") String hsCode) {
        return ResponseEntity.ok(guidanceService.generateExportGuidance(country, hsCode));
    }

    /**
     * Phase 8: Multi-Country Compliance Complexity Comparison
     * POST /api/v1/compliance/compare
     */
    @PostMapping("/compliance/compare")
    public ResponseEntity<ComplianceComparisonService.CompareResponseDto> compareCountries(
            @RequestBody ComplianceComparisonService.CompareRequestDto request) {
        return ResponseEntity.ok(comparisonService.compareCountries(request));
    }

    /**
     * Phase 9: Regulation Freshness & SHA-256 Version Audit
     * GET /api/v1/compliance/freshness
     */
    @GetMapping("/compliance/freshness")
    public ResponseEntity<ComplianceFreshnessService.FreshnessReportResponseDto> getFreshness(
            @RequestParam(name = "country", required = false) String country) {
        return ResponseEntity.ok(freshnessService.getRegulationFreshnessReport(country));
    }

    /**
     * Phase 10: Automated Database Quality Audit
     * GET /api/v1/compliance/data-quality
     */
    /**
     * Phase 10 & Multi-Country Pipeline Execution: 11-Market Regulatory Ingestion, Evidence HS Mapping & 14-Category Audit
     * POST /api/v1/pipeline/ingest-and-audit-global
     */
    @PostMapping("/pipeline/ingest-and-audit-global")
    public ResponseEntity<GlobalCoverageAuditService.GlobalCoverageReportDto> ingestAndAuditGlobal() {
        return ResponseEntity.ok(globalCoverageAuditService.executeGlobalIngestionAndAudit());
    }

    /**
     * Production Real Ingestion Endpoint for a Single Country
     * POST /api/v1/pipeline/ingest-country?country=India
     */
    @PostMapping("/pipeline/ingest-country")
    public ResponseEntity<String> ingestCountry(@RequestParam String country) {
        globalCoverageAuditService.ingestSingleCountry(country);
        return ResponseEntity.ok("Successfully executed production regulatory ingestion for country: " + country);
    }

    /**
     * Production Real Ingestion Endpoint for All 11 Supported Destination Countries
     * POST /api/v1/pipeline/ingest-all
     */
    @PostMapping("/pipeline/ingest-all")
    public ResponseEntity<String> ingestAll() {
        databaseSeedService.seedAllCountriesInDatabase();
        return ResponseEntity.ok("Successfully executed production regulatory ingestion across all 11 destination countries into MySQL TradeData.");
    }

    /**
     * Pipeline Status & Metrics Endpoint
     * GET /api/v1/pipeline/status
     */
    @GetMapping("/pipeline/status")
    public ResponseEntity<Map<String, Object>> getPipelineStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ACTIVE");
        status.put("supportedCountries", GlobalCoverageAuditService.ALL_11_COUNTRIES);
        status.put("hsMasterCount", hsMasterRepository.count());
        status.put("masterRegulationsCount", masterRepository.count());
        status.put("documentsCount", documentRepository.count());
        status.put("certificationsCount", certificationRepository.count());
        return ResponseEntity.ok(status);
    }

    // -------------------------------------------------------------------------
    // Step 18 & 19 Structured Regulatory Intelligence APIs
    // -------------------------------------------------------------------------

    @GetMapping("/regulations/{country}/{hsCode}")
    public ResponseEntity<List<com.cbec.ai.pipeline.model.entity.RegulationMasterEntity>> getRegulations(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        return ResponseEntity.ok(bundle.getMasterRegulations());
    }

    @GetMapping("/compliance/{country}/{hsCode}")
    public ResponseEntity<FullComplianceResponseDto> getCompliance(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceCheckRequest req = ComplianceCheckRequest.builder().country(country).hsCode(hsCode).build();
        return checkCompliance(req);
    }

    @GetMapping("/documents/{country}/{hsCode}")
    public ResponseEntity<List<com.cbec.ai.pipeline.model.entity.RegulationDocumentEntity>> getDocuments(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        return ResponseEntity.ok(bundle.getDocuments());
    }

    @GetMapping("/certificates/{country}/{hsCode}")
    public ResponseEntity<List<com.cbec.ai.pipeline.model.entity.RegulationCertificationEntity>> getCertificates(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        return ResponseEntity.ok(bundle.getCertifications());
    }

    @GetMapping("/requirements/{country}/{hsCode}")
    public ResponseEntity<Map<String, Object>> getRequirements(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("country", bundle.getCountry());
        map.put("hsCode", bundle.getHsCode());
        map.put("productDescription", bundle.getProductDescription());
        map.put("documents", bundle.getDocuments());
        map.put("certifications", bundle.getCertifications());
        map.put("labelingRequirements", bundle.getLabelingRequirements());
        map.put("restrictions", bundle.getRestrictions());
        map.put("procedures", bundle.getProcedures());
        return ResponseEntity.ok(map);
    }

    @GetMapping("/evidence/{country}/{hsCode}")
    public ResponseEntity<List<ComplianceRetrievalService.EvidenceItemDto>> getEvidence(
            @PathVariable String country, @PathVariable String hsCode) {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData(country, hsCode);
        return ResponseEntity.ok(bundle.getEvidence());
    }
}
