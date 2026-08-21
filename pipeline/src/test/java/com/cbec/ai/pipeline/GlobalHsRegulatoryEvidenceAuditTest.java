package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.regulation.service.HsRegulatoryCoverageService;
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
 * Dedicated Evidence Verification Audit Test Class.
 * Performs read-only integrity auditing against hs_master, regulation_master,
 * regulation_hs_mapping, and hs_regulatory_coverage_audit.
 */
@Slf4j
@SpringBootTest
public class GlobalHsRegulatoryEvidenceAuditTest {

    @Autowired private HsMasterRepository hsMasterRepository;
    @Autowired private RegulationMasterRepository regulationMasterRepository;
    @Autowired private RegulationSourceRepository sourceRepository;
    @Autowired private RegulationHsMappingRepository hsMappingRepository;
    @Autowired private HsRegulatoryCoverageAuditRepository coverageAuditRepository;
    @Autowired private RegulationDocumentRepository documentRepository;
    @Autowired private RegulationCertificationRepository certificationRepository;
    @Autowired private RegulationLabelingRepository labelingRepository;
    @Autowired private RegulationRestrictionRepository restrictionRepository;
    @Autowired private RegulationProcedureRepository procedureRepository;
    @Autowired private HsRegulatoryCoverageService coverageService;

    @BeforeEach
    void seedMinimalAuditFixture() {
        if (hsMasterRepository.countByCountry("United States") >= 5) return;

        // Seed representative slice for United States
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("61").heading("6109").hs6("610910").nationalCode("61091000")
                .codeLength(8).nomenclatureType("HTS").category("Apparel")
                .officialDescription("T-shirts of cotton").unit("DOZ")
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
                .officialDescription("Jewellery of precious metals").unit("GR")
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
                .summary("FDA MoCRA cosmetic safety and ingredient reporting")
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

        // Build audit coverage
        coverageService.buildCoverageForCountry("United States");
    }

    @Test
    @DisplayName("Audit Step 1 & 2: HS Inventory & Coverage Table Sync — Zero missing, zero orphans")
    public void testHsInventoryAndAuditSync() {
        log.info("Executing Audit Step 1 & 2: HS Inventory & Coverage Table Sync...");

        long hsMasterTotal = hsMasterRepository.countByCountry("United States");
        long coverageAuditTotal = coverageAuditRepository.countByCountry("United States");

        assertEquals(hsMasterTotal, coverageAuditTotal, "hs_master and hs_regulatory_coverage_audit counts must be equal");

        List<HsMasterEntity> masterList = hsMasterRepository.findByCountry("United States");
        List<HsRegulatoryCoverageAuditEntity> auditList = coverageAuditRepository.findByCountry("United States");

        Set<String> masterKeys = masterList.stream().map(HsMasterEntity::getNationalCode).collect(Collectors.toSet());
        Set<String> auditKeys = auditList.stream().map(HsRegulatoryCoverageAuditEntity::getHsCode).collect(Collectors.toSet());

        Set<String> missingInAudit = new HashSet<>(masterKeys);
        missingInAudit.removeAll(auditKeys);
        assertEquals(0, missingInAudit.size(), "Zero HS codes must be missing from coverage audit");

        Set<String> orphanAudit = new HashSet<>(auditKeys);
        orphanAudit.removeAll(masterKeys);
        assertEquals(0, orphanAudit.size(), "Zero orphan audit records allowed");
    }

    @Test
    @DisplayName("Audit Step 3 & 4: Mapping Method Distribution & Coverage Breakdown")
    public void testMappingMethodDistribution() {
        log.info("Executing Audit Step 3 & 4: Mapping Method Distribution...");

        List<HsRegulatoryCoverageAuditEntity> auditList = coverageAuditRepository.findByCountry("United States");
        Map<String, Long> methodCounts = auditList.stream()
                .collect(Collectors.groupingBy(HsRegulatoryCoverageAuditEntity::getMappingMethod, Collectors.counting()));

        assertTrue(methodCounts.getOrDefault("EXACT_NATIONAL_CODE", 0L) >= 1, "EXACT_NATIONAL_CODE mapping present");
        assertTrue(methodCounts.getOrDefault("HS6", 0L) >= 1, "HS6 mapping present");
        assertTrue(methodCounts.getOrDefault("HS4_HEADING", 0L) >= 1, "HS4_HEADING mapping present");
        assertTrue(methodCounts.getOrDefault("HS2_CHAPTER", 0L) >= 1, "HS2_CHAPTER mapping present");
        assertTrue(methodCounts.getOrDefault("NOT_FOUND", 0L) >= 1, "NOT_FOUND mapping present");

        long total = auditList.size();
        long mapped = auditList.stream().filter(r -> Boolean.TRUE.equals(r.getRegulationFound())).count();
        double anyMappingCoverage = (mapped * 100.0) / total;

        log.info("Distribution: EXACT={}, HS6={}, HS4={}, HS2={}, NOT_FOUND={}",
                methodCounts.get("EXACT_NATIONAL_CODE"), methodCounts.get("HS6"),
                methodCounts.get("HS4_HEADING"), methodCounts.get("HS2_CHAPTER"),
                methodCounts.get("NOT_FOUND"));
        log.info("Any Mapping Coverage: {}%", anyMappingCoverage);
    }

    @Test
    @DisplayName("Audit Step 5, 6, 7: Regulation Evidence Lineage & Source URL Integrity")
    public void testRegulationEvidenceAndSourceIntegrity() {
        log.info("Executing Audit Step 5, 6, 7: Evidence Lineage & Source URL Integrity...");

        List<HsRegulatoryCoverageAuditEntity> mappedList = coverageAuditRepository.findByCountry("United States").stream()
                .filter(r -> Boolean.TRUE.equals(r.getRegulationFound()))
                .toList();

        for (HsRegulatoryCoverageAuditEntity audit : mappedList) {
            assertNotNull(audit.getRegulationId(), "regulation_id must not be null for mapped record");
            assertNotNull(audit.getRegulationTitle(), "regulation_title must not be null for mapped record");
            assertNotNull(audit.getAuthority(), "authority must not be null for mapped record");
            assertNotNull(audit.getSourceUrl(), "source_url must not be null for mapped record");
            assertNotNull(audit.getSourceReference(), "source_reference must not be null for mapped record");

            assertTrue(audit.getSourceUrl().startsWith("http://") || audit.getSourceUrl().startsWith("https://"),
                    "source_url must be valid HTTP/HTTPS URL");

            Optional<RegulationMasterEntity> regOpt = regulationMasterRepository.findById(audit.getRegulationId());
            assertTrue(regOpt.isPresent(), "regulation_master record must exist for regulation_id " + audit.getRegulationId());
        }
    }

    @Test
    @DisplayName("Audit Step 10 & 11: Zero Fabrication & Confidence Score Consistency")
    public void testZeroFabricationAndConfidenceConsistency() {
        log.info("Executing Audit Step 10 & 11: Zero Fabrication & Confidence Score Audit...");

        List<HsRegulatoryCoverageAuditEntity> auditList = coverageAuditRepository.findByCountry("United States");

        for (HsRegulatoryCoverageAuditEntity audit : auditList) {
            if (Boolean.FALSE.equals(audit.getRegulationFound())) {
                assertNull(audit.getRegulationId(), "regulation_id MUST be null when regulation_found=false (zero fabrication)");
                assertEquals("NOT_FOUND", audit.getMappingMethod(), "mapping_method MUST be NOT_FOUND");
                assertEquals(0.0, audit.getConfidenceScore(), "confidence_score MUST be 0.0 for NOT_FOUND");
                assertEquals("NO_SPECIFIC_REGULATION_FOUND", audit.getCoverageStatus());
            }

            if ("EXACT_NATIONAL_CODE".equals(audit.getMappingMethod())) {
                assertEquals(0.95, audit.getConfidenceScore(), 0.01, "EXACT confidence must be 0.95");
            } else if ("HS6".equals(audit.getMappingMethod())) {
                assertEquals(0.80, audit.getConfidenceScore(), 0.01, "HS6 confidence must be 0.80");
            } else if ("HS4_HEADING".equals(audit.getMappingMethod())) {
                assertEquals(0.65, audit.getConfidenceScore(), 0.01, "HS4 confidence must be 0.65");
            } else if ("HS2_CHAPTER".equals(audit.getMappingMethod())) {
                assertEquals(0.50, audit.getConfidenceScore(), 0.01, "HS2 confidence must be 0.50");
            }
        }
    }

    @Test
    @DisplayName("Audit Step 12: HS Code Format Validation — 2-10 numeric digits only")
    public void testHsCodeFormatValidation() {
        log.info("Executing Audit Step 12: HS Code Format Validation...");

        List<HsMasterEntity> masterList = hsMasterRepository.findByCountry("United States");
        for (HsMasterEntity hs : masterList) {
            assertNotNull(hs.getNationalCode(), "national_code must not be null");
            assertFalse(hs.getNationalCode().isBlank(), "national_code must not be blank");
            assertTrue(hs.getNationalCode().matches("^[0-9]{2,10}$"),
                    "HS code must consist of 2 to 10 numeric digits: " + hs.getNationalCode());
        }
    }
}
