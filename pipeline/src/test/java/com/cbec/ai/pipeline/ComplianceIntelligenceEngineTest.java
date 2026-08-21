package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import com.cbec.ai.pipeline.regulation.controller.RegulationController;
import com.cbec.ai.pipeline.regulation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ComplianceIntelligenceEngineTest {

    @Autowired
    private HsMasterRepository hsMasterRepository;

    @Autowired
    private RegulationMasterRepository masterRepository;

    @Autowired
    private RegulationHsMappingRepository hsMappingRepository;

    @Autowired
    private RegulationDocumentRepository documentRepository;

    @Autowired
    private RegulationCertificationRepository certificationRepository;

    @Autowired
    private RegulationLabelingRepository labelingRepository;

    @Autowired
    private RegulationRestrictionRepository restrictionRepository;

    @Autowired
    private RegulationProcedureRepository procedureRepository;

    @Autowired
    private RegulationSourceRepository sourceRepository;

    @Autowired
    private RegulationDownloadHistoryRepository downloadHistoryRepository;

    @Autowired
    private ComplianceRetrievalService retrievalService;

    @Autowired
    private ComplianceScoringService scoringService;

    @Autowired
    private ComplianceAiExplanationService aiExplanationService;

    @Autowired
    private ComplianceGuidanceService guidanceService;

    @Autowired
    private ComplianceComparisonService comparisonService;

    @Autowired
    private ComplianceFreshnessService freshnessService;

    @Autowired
    private ComplianceDataQualityService dataQualityService;

    @Autowired
    private RegulationController regulationController;

    @BeforeEach
    public void seedTestData() {
        // Lightweight in-memory dataset seeding across target markets (India, US, UK, Germany, Japan, South Korea)
        if (masterRepository.count() == 0) {
            List.of("United States", "United Kingdom", "India", "Germany", "Japan", "South Korea").forEach(country -> {
                RegulationMasterEntity master = masterRepository.save(RegulationMasterEntity.builder()
                        .country(country)
                        .title(country + " Official Trade Regulation")
                        .authority(country + " Customs & Trade Authority")
                        .regulationType("IMPORT_CONTROL")
                        .summary("Official regulatory import control requirements for " + country)
                        .sourceUrl("https://www.gov." + country.toLowerCase().replace(" ", "") + ".org/trade")
                        .confidenceScore(0.96)
                        .build());

                hsMasterRepository.save(HsMasterEntity.builder()
                        .country(country)
                        .customsTerritory(country)
                        .chapter("33")
                        .heading("3304")
                        .hs6("330499")
                        .nationalCode("33049900")
                        .codeLength(8)
                        .nomenclatureType("NATIONAL")
                        .category("COSMETICS")
                        .datasetVersion("v1")
                        .officialDescription("Beauty or make-up preparations & skin care products")
                        .build());

                hsMappingRepository.save(RegulationHsMappingEntity.builder()
                        .regulationId(master.getId())
                        .chapter("33")
                        .heading("3304")
                        .hs6("330499")
                        .nationalCode("33049900")
                        .sourceReference(country + " Reg Section 3304")
                        .build());

                documentRepository.save(RegulationDocumentEntity.builder()
                        .regulationId(master.getId())
                        .documentName("Import Declaration Form 1")
                        .mandatory(true)
                        .remarks("Required prior to entry release")
                        .build());

                certificationRepository.save(RegulationCertificationEntity.builder()
                        .regulationId(master.getId())
                        .certificationName("Certificate of Analysis")
                        .mandatory(true)
                        .remarks("Chemical safety verification")
                        .build());

                labelingRepository.save(RegulationLabelingEntity.builder()
                        .regulationId(master.getId())
                        .requirement("Official Language Labeling & Expiry Date")
                        .remarks("Mandatory on outer packaging")
                        .build());

                restrictionRepository.save(RegulationRestrictionEntity.builder()
                        .regulationId(master.getId())
                        .restrictionType("CONTROLLED_INGREDIENTS")
                        .description("Restricted heavy metals & prohibited chemicals")
                        .remarks("Maximum limit thresholds enforced")
                        .build());

                procedureRepository.save(RegulationProcedureEntity.builder()
                        .regulationId(master.getId())
                        .procedureName("Customs Port Inspection")
                        .description("Physical sampling at port of entry")
                        .stepOrder(1)
                        .build());

                RegulationSourceEntity src = sourceRepository.save(RegulationSourceEntity.builder()
                        .country(country)
                        .authority(country + " Customs & Border Protection")
                        .title(country + " Official Trade Portal")
                        .documentType("OFFICIAL_REGULATION")
                        .sourceUrl("https://www.gov." + country.toLowerCase().replace(" ", "") + ".org/trade")
                        .format("HTML")
                        .status("ACTIVE")
                        .lastUpdated(LocalDateTime.now())
                        .build());

                downloadHistoryRepository.save(RegulationDownloadHistoryEntity.builder()
                        .country(country)
                        .sourceId(src.getId())
                        .fileName("reg.html")
                        .filePath("/downloads/" + country + "/reg.html")
                        .fileSize(1024L)
                        .sha256("abc123sha256hash_" + country.toLowerCase().replace(" ", ""))
                        .downloadTime(LocalDateTime.now())
                        .lastVerifiedAt(LocalDateTime.now())
                        .version("v1")
                        .status("UNCHANGED")
                        .build());
            });
        }
    }

    @Test
    @DisplayName("Test 1: Exact National Code Match Resolution")
    public void testExactNationalCodeMatch() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("United States", "33049900");
        assertNotNull(bundle);
        assertEquals("United States", bundle.getCountry());
        assertNotNull(bundle.getHsMatch());
        assertNotNull(bundle.getEvidence());
        assertFalse(bundle.getEvidence().isEmpty(), "Evidence must never be empty for valid HS query");

        // Assert all requirements have valid source references
        bundle.getEvidence().forEach(ev -> {
            assertNotNull(ev.getAuthority());
            assertNotNull(ev.getSourceTitle());
            assertNotNull(ev.getSourceReference());
            assertFalse(ev.getSourceReference().isBlank(), "Every requirement MUST have a source reference");
        });
    }

    @Test
    @DisplayName("Test 2: HS6 Subheading Match Resolution")
    public void testHs6SubheadingMatch() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("United Kingdom", "330499");
        assertNotNull(bundle);
        assertEquals("United Kingdom", bundle.getCountry());
        assertNotNull(bundle.getEvidence());
        assertFalse(bundle.getEvidence().isEmpty());
    }

    @Test
    @DisplayName("Test 3: 4-Digit Heading Match Resolution")
    public void testHeading4DigitMatch() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("Germany", "3304");
        assertNotNull(bundle);
        assertEquals("Germany", bundle.getCountry());
        assertNotNull(bundle.getEvidence());
        assertFalse(bundle.getEvidence().isEmpty());
    }

    @Test
    @DisplayName("Test 4: 2-Digit Chapter Match Resolution")
    public void testChapter2DigitMatch() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("India", "33");
        assertNotNull(bundle);
        assertEquals("India", bundle.getCountry());
        assertNotNull(bundle.getEvidence());
        assertFalse(bundle.getEvidence().isEmpty());
    }

    @Test
    @DisplayName("Test 5: Compliance Complexity Scoring Engine")
    public void testComplianceScoring() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("United States", "33049900");
        ComplianceScoringService.ScoringResultDto scoring = scoringService.calculateComplianceScore(bundle);

        assertNotNull(scoring);
        assertTrue(scoring.getScore() >= 0 && scoring.getScore() <= 100, "Score must be bounded between 0 and 100");
        assertNotNull(scoring.getLevel());
        assertNotNull(scoring.getFactors());
        assertFalse(scoring.getFactors().isEmpty(), "Scoring factor breakdown must be present");
    }

    @Test
    @DisplayName("Test 6: AI Explanation & Grounded Chat Assistant")
    public void testAiExplanationAndChat() {
        ComplianceRetrievalService.ComplianceDataBundleDto bundle = retrievalService.retrieveComplianceData("Japan", "33049900");
        String explanation = aiExplanationService.generateComplianceExplanation(bundle);
        assertNotNull(explanation);
        assertFalse(explanation.isBlank());

        ComplianceAiExplanationService.ChatRequestDto chatReq = ComplianceAiExplanationService.ChatRequestDto.builder()
                .country("Japan")
                .hsCode("33049900")
                .question("What import documentation is required for skin care products?")
                .build();

        ComplianceAiExplanationService.ChatResponseDto chatResp = aiExplanationService.answerComplianceQuestion(chatReq, bundle);
        assertNotNull(chatResp);
        assertNotNull(chatResp.getAnswer());
        assertFalse(chatResp.getAnswer().isBlank());
        assertNotNull(chatResp.getEvidenceReferences());
    }

    @Test
    @DisplayName("Test 7: 10-Step Export Guidance Workflow Service")
    public void testExportGuidance() {
        ComplianceGuidanceService.ExportGuidanceResponseDto guidance = guidanceService.generateExportGuidance("South Korea", "33049900");
        assertNotNull(guidance);
        assertEquals("South Korea", guidance.getCountry());
        assertTrue(guidance.getTotalSteps() >= 8, "Workflow must contain complete export clearance steps");
        assertFalse(guidance.getWorkflowSteps().isEmpty());

        guidance.getWorkflowSteps().forEach(step -> {
            assertNotNull(step.getStepNumber());
            assertNotNull(step.getStepName());
            assertNotNull(step.getDescription());
            assertNotNull(step.getEvidenceCitations());
            assertFalse(step.getEvidenceCitations().isEmpty(), "Every guidance step must be supported by DB evidence citations");
        });
    }

    @Test
    @DisplayName("Test 8: Objective Multi-Country Compliance Comparison")
    public void testMultiCountryComparison() {
        ComplianceComparisonService.CompareRequestDto request = ComplianceComparisonService.CompareRequestDto.builder()
                .hsCode("33049900")
                .countries(List.of("United States", "United Kingdom", "Germany", "Japan", "India"))
                .build();

        ComplianceComparisonService.CompareResponseDto response = comparisonService.compareCountries(request);
        assertNotNull(response);
        assertEquals(5, response.getCountriesCompared());
        assertNotNull(response.getRankings());
        assertEquals(5, response.getRankings().size());

        for (int i = 0; i < response.getRankings().size() - 1; i++) {
            assertTrue(response.getRankings().get(i).getComplianceScore() <= response.getRankings().get(i + 1).getComplianceScore(),
                    "Rankings must be sorted objectively by compliance complexity score");
        }
    }

    @Test
    @DisplayName("Test 9: Regulation Freshness & SHA-256 Versioning Audit")
    public void testRegulationFreshnessReport() {
        ComplianceFreshnessService.FreshnessReportResponseDto report = freshnessService.getRegulationFreshnessReport(null);
        assertNotNull(report);
        assertTrue(report.getTotalSourcesMonitored() > 0, "Monitored sources count must be greater than zero");
        assertNotNull(report.getSources());
        assertFalse(report.getSources().isEmpty());

        report.getSources().forEach(item -> {
            assertNotNull(item.getCountry());
            assertNotNull(item.getAuthority());
            assertNotNull(item.getCurrentVersion());
            assertNotNull(item.getSha256());
            assertNotNull(item.getChangedStatus());
        });
    }

    @Test
    @DisplayName("Test 10: Automated Database Quality Audit Service")
    public void testDataQualityAudit() {
        ComplianceDataQualityService.DataQualityReportResponseDto quality = dataQualityService.runDataQualityAudit();
        assertNotNull(quality);
        assertTrue(quality.getTotalRecords() > 0, "Total database records checked must be > 0");
        assertNotNull(quality.getQualityScore());
        assertTrue(quality.getQualityScore() >= 80.0, "Database Quality Score must exceed 80.0%");
    }

    @Test
    @DisplayName("Test 11: Production REST Controller Check Endpoint")
    public void testControllerCheckEndpoint() {
        RegulationController.ComplianceCheckRequest req = RegulationController.ComplianceCheckRequest.builder()
                .country("United States")
                .hsCode("33049900")
                .build();

        var responseEntity = regulationController.checkCompliance(req);
        assertNotNull(responseEntity);
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        assertNotNull(responseEntity.getBody());
        assertEquals("United States", responseEntity.getBody().getCountry());
        assertNotNull(responseEntity.getBody().getComplianceScore());
        assertNotNull(responseEntity.getBody().getComplexityLevel());
        assertNotNull(responseEntity.getBody().getEvidence());
        assertFalse(responseEntity.getBody().getEvidence().isEmpty());
    }
}
