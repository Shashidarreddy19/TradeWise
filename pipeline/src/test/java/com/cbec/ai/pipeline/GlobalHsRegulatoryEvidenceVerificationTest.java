package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.regulation.service.HsRegulatoryCoverageService;
import com.cbec.ai.pipeline.regulation.service.RegulationHsEvidenceVerificationService;
import com.cbec.ai.pipeline.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Required Substantive Evidence Verification Integration Test.
 * Validates that regulation_hs_evidence_verification records strictly match source evidence.
 */
@Slf4j
@SpringBootTest
public class GlobalHsRegulatoryEvidenceVerificationTest {

    @Autowired private HsMasterRepository hsMasterRepository;
    @Autowired private RegulationMasterRepository regulationMasterRepository;
    @Autowired private RegulationHsMappingRepository hsMappingRepository;
    @Autowired private HsRegulatoryCoverageAuditRepository coverageAuditRepository;
    @Autowired private RegulationHsEvidenceVerificationRepository verificationRepository;
    @Autowired private RegulationDocumentRepository documentRepository;
    @Autowired private RegulationCertificationRepository certificationRepository;
    @Autowired private HsRegulatoryCoverageService coverageService;
    @Autowired private RegulationHsEvidenceVerificationService verificationService;

    @BeforeEach
    void seedTestData() {
        if (hsMasterRepository.countByCountry("United States") >= 5) return;

        // Seed representative test fixtures
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("61").heading("6109").hs6("610910").nationalCode("61091000")
                .codeLength(8).nomenclatureType("HTS").category("Apparel")
                .officialDescription("Cotton T-shirts").unit("DOZ")
                .datasetVersion("2026").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("33").heading("3304").hs6("330499").nationalCode("33049900")
                .codeLength(8).nomenclatureType("HTS").category("Cosmetics")
                .officialDescription("Beauty preparations").unit("KG")
                .datasetVersion("2026").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("30").heading("3004").hs6("300490").nationalCode("30049010")
                .codeLength(8).nomenclatureType("HTS").category("Pharmaceuticals")
                .officialDescription("Medicaments").unit("KG")
                .datasetVersion("2026").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("09").heading("0904").hs6("090411").nationalCode("09041100")
                .codeLength(8).nomenclatureType("HTS").category("Spices")
                .officialDescription("Pepper dried").unit("KG")
                .datasetVersion("2026").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("71").heading("7113").hs6("711319").nationalCode("71131900")
                .codeLength(8).nomenclatureType("HTS").category("Jewellery")
                .officialDescription("Jewellery").unit("GR")
                .datasetVersion("2026").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // Master Regulations
        RegulationMasterEntity reg1 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States").authority("U.S. CBP / FTC")
                .title("US Textile and Apparel Import Requirements — HTS 61091000")
                .regulationType("IMPORT_DOCUMENTATION,LABELING")
                .summary("Customs entry requirements for cotton T-shirts under HTS 61091000")
                .sourceUrl("https://www.cbp.gov/trade/priority-issues/textiles")
                .confidenceScore(0.95).build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg1.getId())
                .nationalCode("61091000").hs6("610910").heading("6109").chapter("61")
                .mappingMethod("EXPLICIT_SOURCE_MAPPING").confidence(0.95).confidenceScore(0.95)
                .sourceReference("CBP Textile Enforcement — HTS 61091000 — https://www.cbp.gov/trade/priority-issues/textiles").build());

        documentRepository.save(RegulationDocumentEntity.builder().regulationId(reg1.getId()).documentName("Commercial Invoice").mandatory(true).build());
        documentRepository.save(RegulationDocumentEntity.builder().regulationId(reg1.getId()).documentName("Packing List").mandatory(true).build());
        documentRepository.save(RegulationDocumentEntity.builder().regulationId(reg1.getId()).documentName("Country of Origin Declaration (CBP Form 3461)").mandatory(true).build());
        certificationRepository.save(RegulationCertificationEntity.builder().regulationId(reg1.getId()).certificationName("FTC Textile Fiber Products Identification Act Compliance").mandatory(true).build());

        RegulationMasterEntity reg2 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States").authority("Food and Drug Administration (FDA)")
                .title("FDA Cosmetic Safety Modernization — HS6 330499")
                .regulationType("PRODUCT_SAFETY,LABELING")
                .summary("FDA MoCRA cosmetic safety reporting")
                .sourceUrl("https://www.fda.gov/cosmetics/cosmetics-laws-regulations")
                .confidenceScore(0.90).build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg2.getId())
                .hs6("330499").heading("3304").chapter("33")
                .mappingMethod("HS6_SUBHEADING").confidence(0.90).confidenceScore(0.90)
                .sourceReference("FDA MoCRA Cosmetic Regulations — HS 3304.99 — https://www.fda.gov/cosmetics").build());

        RegulationMasterEntity reg3 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States").authority("Food and Drug Administration (FDA)")
                .title("FDA Drug Import Controls — Heading 3004")
                .regulationType("PHARMA,PERMITS")
                .summary("FDA prior notice and NDC registration")
                .sourceUrl("https://www.fda.gov/drugs/international-drug-programs")
                .confidenceScore(0.85).build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg3.getId())
                .heading("3004").chapter("30")
                .mappingMethod("HEADING_4DIGIT").confidence(0.85).confidenceScore(0.85)
                .sourceReference("FDA Drug Regulations — Heading 3004 — https://www.fda.gov/drugs").build());

        RegulationMasterEntity reg4 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States").authority("USDA APHIS")
                .title("USDA Phytosanitary Rules — Chapter 09 Spices")
                .regulationType("SPS,AGRI")
                .summary("USDA APHIS spice import requirements")
                .sourceUrl("https://www.aphis.usda.gov/import-export")
                .confidenceScore(0.70).build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg4.getId())
                .chapter("09")
                .mappingMethod("CHAPTER_2DIGIT").confidence(0.70).confidenceScore(0.70)
                .sourceReference("USDA APHIS Plant Protection — Chapter 09 — https://www.aphis.usda.gov/import-export").build());

        coverageService.buildCoverageForCountry("United States");
    }

    @Test
    @DisplayName("Test 1: Substantive Evidence Audit Execution & Zero Duplicate Check")
    public void testEvidenceVerificationAuditExecution() {
        var report = verificationService.auditCountryEvidence("United States");
        assertNotNull(report);
        assertEquals("United States", report.getCountry());
        assertEquals(5, report.getTotalHsCodes());

        List<RegulationHsEvidenceVerificationEntity> list = verificationRepository.findByCountry("United States");
        assertEquals(5, list.size());

        Set<String> uniqueKeys = list.stream().map(v -> v.getCountry() + "|" + v.getHsCode()).collect(Collectors.toSet());
        assertEquals(5, uniqueKeys.size(), "Zero duplicate verification records allowed");
    }

    @Test
    @DisplayName("Test 2: Evidence Level and Status Alignment — Verified records contain actual evidence")
    public void testEvidenceLevelAndStatusAlignment() {
        verificationService.auditCountryEvidence("United States");

        List<RegulationHsEvidenceVerificationEntity> list = verificationRepository.findByCountry("United States");

        for (RegulationHsEvidenceVerificationEntity entity : list) {
            assertNotNull(entity.getHsCode());
            assertNotNull(entity.getVerificationStatus());
            assertNotNull(entity.getEvidenceLevel());

            if (RegulationHsEvidenceVerificationService.STATUS_EXACTLY_SUPPORTED.equals(entity.getVerificationStatus())) {
                assertEquals(RegulationHsEvidenceVerificationService.LEVEL_EXACT, entity.getEvidenceLevel());
                assertNotNull(entity.getEvidenceText(), "VERIFIED EXACT record must contain actual evidence text");
                assertNotNull(entity.getSourceUrl(), "VERIFIED record must contain source URL");
                assertNotNull(entity.getRegulationId(), "VERIFIED record must contain valid regulation_id");
            }

            if (RegulationHsEvidenceVerificationService.STATUS_HS6_SUPPORTED.equals(entity.getVerificationStatus())) {
                assertEquals(RegulationHsEvidenceVerificationService.LEVEL_HS6, entity.getEvidenceLevel());
                assertNotNull(entity.getEvidenceText(), "VERIFIED HS6 record must contain actual evidence text");
            }

            if (RegulationHsEvidenceVerificationService.STATUS_NOT_SUPPORTED.equals(entity.getVerificationStatus())) {
                assertEquals(RegulationHsEvidenceVerificationService.LEVEL_NO_EVIDENCE, entity.getEvidenceLevel());
                assertNull(entity.getEvidenceText(), "NOT_SUPPORTED record must NOT contain fabricated evidence text");
            }
        }
    }

    @Test
    @DisplayName("Test 3: Verification Metric Calculation — NOT_SUPPORTED & INDIRECT_INFERENCE excluded from exact product coverage")
    public void testVerificationMetricCalculations() {
        var report = verificationService.auditCountryEvidence("United States");

        assertEquals(1, report.getExactlySupported(), "Exactly 1 EXACTLY_SUPPORTED record expected (61091000)");
        assertEquals(1, report.getHs6Supported(), "Exactly 1 HS6_SUPPORTED record expected (33049900)");
        assertEquals(1, report.getHs4Supported(), "Exactly 1 HS4_SUPPORTED record expected (30049010)");
        assertEquals(1, report.getHs2Supported(), "Exactly 1 HS2_SUPPORTED record expected (09041100)");
        assertEquals(1, report.getNotSupported(), "Exactly 1 NOT_SUPPORTED record expected (71131900)");

        assertEquals(20.0, report.getExactProductCoveragePct(), 0.1, "Exact Product Coverage should be 20.0% (1/5)");
        assertEquals(40.0, report.getDirectHs6CoveragePct(), 0.1, "Direct HS6 Coverage should be 40.0% (2/5)");
        assertEquals(80.0, report.getHierarchicalCoveragePct(), 0.1, "Hierarchical Coverage should be 80.0% (4/5)");
        assertEquals(20.0, report.getUnsupportedPct(), 0.1, "Unsupported Pct should be 20.0% (1/5)");

        log.info("Metric verification passed: ExactProductCoverage={}%, DirectHs6Coverage={}%, HierarchicalCoverage={}%",
                report.getExactProductCoveragePct(), report.getDirectHs6CoveragePct(), report.getHierarchicalCoveragePct());
    }
}
