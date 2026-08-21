package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.regulation.service.HsInventoryAuditService;
import com.cbec.ai.pipeline.regulation.service.HsRegulatoryCoverageService;
import com.cbec.ai.pipeline.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HS Regulatory Coverage Pipeline.
 *
 * Seeds a realistic slice of hs_master + regulation data in H2
 * to validate the full 4-level HS hierarchy matching logic.
 *
 * Uses only evidence-backed matching (no fabricated data).
 */
@SpringBootTest
public class HsRegulatoryCoverageTest {

    @Autowired private HsRegulatoryCoverageService coverageService;
    @Autowired private HsInventoryAuditService inventoryService;
    @Autowired private HsRegulatoryCoverageAuditRepository coverageAuditRepository;
    @Autowired private HsMasterRepository hsMasterRepository;
    @Autowired private RegulationMasterRepository regulationMasterRepository;
    @Autowired private RegulationHsMappingRepository hsMappingRepository;
    @Autowired private RegulationDocumentRepository documentRepository;
    @Autowired private RegulationCertificationRepository certificationRepository;
    @Autowired private RegulationLabelingRepository labelingRepository;
    @Autowired private RegulationRestrictionRepository restrictionRepository;
    @Autowired private RegulationProcedureRepository procedureRepository;

    /**
     * Seed test HS master + regulation data once per test class run.
     * Covers multiple matching scenarios:
     *  - EXACT_NATIONAL_CODE match (8-digit code)
     *  - HS6 match
     *  - HS4_HEADING match
     *  - HS2_CHAPTER match
     *  - NOT_FOUND (no matching regulation)
     */
    @BeforeEach
    void seedTestData() {
        // Idempotent seeding: only insert if not already present
        if (hsMasterRepository.countByCountry("United States") >= 5) return;

        // ---- HS Master Data (United States sample) ----
        // Code 61091000 → chapter=61, heading=6109, hs6=610910, national=61091000 (EXACT match)
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("61").heading("6109").hs6("610910").nationalCode("61091000")
                .codeLength(8).nomenclatureType("HTS").category("Apparel & Garments")
                .officialDescription("T-shirts, singlets and other vests of cotton").unit("DOZ")
                .datasetVersion("2025").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // Code 33049900 → chapter=33, heading=3304, hs6=330499, national=33049900 (HS6 match via regulation)
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("33").heading("3304").hs6("330499").nationalCode("33049900")
                .codeLength(8).nomenclatureType("HTS").category("Cosmetics")
                .officialDescription("Beauty or make-up preparations, skin care").unit("KG")
                .datasetVersion("2025").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // Code 30049010 → chapter=30, heading=3004, hs6=300490, national=30049010 (HS4 heading match)
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("30").heading("3004").hs6("300490").nationalCode("30049010")
                .codeLength(8).nomenclatureType("HTS").category("Pharmaceuticals")
                .officialDescription("Other medicaments, not elsewhere specified").unit("KG")
                .datasetVersion("2025").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // Code 09041100 → chapter=09, heading=0904, hs6=090411, national=09041100 (HS2 chapter match)
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("09").heading("0904").hs6("090411").nationalCode("09041100")
                .codeLength(8).nomenclatureType("HTS").category("Spices")
                .officialDescription("Pepper of the genus Piper - dried").unit("KG")
                .datasetVersion("2025").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // Code 71131900 → chapter=71, heading=7113, hs6=711319, national=71131900 (NOT_FOUND scenario)
        hsMasterRepository.save(HsMasterEntity.builder()
                .country("United States").customsTerritory("US")
                .chapter("71").heading("7113").hs6("711319").nationalCode("71131900")
                .codeLength(8).nomenclatureType("HTS").category("Gems & Jewellery")
                .officialDescription("Jewellery of other precious metals").unit("GR")
                .datasetVersion("2025").isCurrent(true).isActive(true)
                .lastVerified(LocalDateTime.now()).build());

        // ---- Regulation Master Data ----
        // Reg 1: Exact national code match for 61091000
        RegulationMasterEntity reg1 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States")
                .authority("US CBP / FTC")
                .title("US Textile and Apparel Import Requirements — HTS 61091000")
                .regulationType("IMPORT_DOCUMENTATION,LABELING")
                .summary("Country-of-origin labeling, fiber content disclosure, and customs entry requirements for T-shirts under HTS 61091000")
                .sourceUrl("https://www.cbp.gov/trade/priority-issues/textiles")
                .confidenceScore(0.95)
                .build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg1.getId())
                .nationalCode("61091000").hs6("610910").heading("6109").chapter("61")
                .mappingMethod("EXPLICIT_SOURCE_MAPPING").confidence(0.95).confidenceScore(0.95)
                .sourceReference("CBP Textile Enforcement Reference — HTS 6109.10 — https://www.cbp.gov/trade/priority-issues/textiles").build());

        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg1.getId()).documentName("Commercial Invoice").mandatory(true).build());
        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg1.getId()).documentName("Packing List").mandatory(true).build());
        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg1.getId()).documentName("Country of Origin Declaration (CBP Form 3461)").mandatory(true).build());
        certificationRepository.save(RegulationCertificationEntity.builder()
                .regulationId(reg1.getId()).certificationName("FTC Textile Fiber Products Identification Act Compliance").mandatory(true).build());
        labelingRepository.save(RegulationLabelingEntity.builder()
                .regulationId(reg1.getId()).requirement("Country of origin label in English").build());
        labelingRepository.save(RegulationLabelingEntity.builder()
                .regulationId(reg1.getId()).requirement("Fiber content disclosure (%)").build());

        // Reg 2: HS6-level match for 330499 cosmetics
        RegulationMasterEntity reg2 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States")
                .authority("US FDA")
                .title("FDA Cosmetic Safety Requirements — HS6 330499")
                .regulationType("PRODUCT_SAFETY,LABELING,IMPORT_DOCUMENTATION")
                .summary("FDA registration and ingredient safety requirements for cosmetic preparations under HS 3304.99")
                .sourceUrl("https://www.fda.gov/cosmetics/cosmetics-laws-regulations")
                .confidenceScore(0.90)
                .build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg2.getId())
                .hs6("330499").heading("3304").chapter("33")
                .mappingMethod("HS6_SUBHEADING").confidence(0.90).confidenceScore(0.90)
                .sourceReference("FDA Cosmetics Import Alert — HS 3304.99 — https://www.fda.gov/cosmetics").build());

        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg2.getId()).documentName("FDA Cosmetic Facility Registration").mandatory(false).build());
        certificationRepository.save(RegulationCertificationEntity.builder()
                .regulationId(reg2.getId()).certificationName("FDA Adverse Event Reporting Compliance").mandatory(true).build());
        restrictionRepository.save(RegulationRestrictionEntity.builder()
                .regulationId(reg2.getId()).restrictionType("PROHIBITED_INGREDIENTS")
                .description("Prohibited color additives and ingredients per FDA 21 CFR Part 700").build());

        // Reg 3: HS4-heading level match for chapter 30 (Pharmaceuticals — heading 3004)
        RegulationMasterEntity reg3 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States")
                .authority("US FDA")
                .title("FDA Drug Import Requirements — Heading 3004")
                .regulationType("PHARMA,IMPORT_PERMITS,CERTIFICATIONS")
                .summary("FDA prior notice, drug registration, and import licensing requirements for pharmaceuticals under heading 3004")
                .sourceUrl("https://www.fda.gov/drugs/international-drug-programs/import-and-export-drugs")
                .confidenceScore(0.85)
                .build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg3.getId())
                .heading("3004").chapter("30")
                .mappingMethod("HEADING_4DIGIT").confidence(0.85).confidenceScore(0.85)
                .sourceReference("FDA Drug Import Requirements Heading 3004 — https://www.fda.gov/drugs/international-drug-programs").build());

        certificationRepository.save(RegulationCertificationEntity.builder()
                .regulationId(reg3.getId()).certificationName("FDA Drug Registration (NDC)").mandatory(true).build());
        certificationRepository.save(RegulationCertificationEntity.builder()
                .regulationId(reg3.getId()).certificationName("FDA Prior Notice of Drug Imports").mandatory(true).build());
        procedureRepository.save(RegulationProcedureEntity.builder()
                .regulationId(reg3.getId()).procedureName("FDA Prior Notice Submission")
                .description("Submit prior notice via PNSI at least 2 hours before importation").stepOrder(1).build());

        // Reg 4: HS2-chapter match for chapter 09 (Spices)
        RegulationMasterEntity reg4 = regulationMasterRepository.save(RegulationMasterEntity.builder()
                .country("United States")
                .authority("USDA FSIS / FDA")
                .title("USDA-FDA SPS Requirements — Chapter 09 Spices")
                .regulationType("SPS,FOOD,IMPORT_DOCUMENTATION")
                .summary("FDA Prior Notice and USDA phytosanitary requirements for spices under HS chapter 09")
                .sourceUrl("https://www.ams.usda.gov/services/imports-exports/spices")
                .confidenceScore(0.70)
                .build());

        hsMappingRepository.save(RegulationHsMappingEntity.builder()
                .regulationId(reg4.getId())
                .chapter("09")
                .mappingMethod("CHAPTER_2DIGIT").confidence(0.70).confidenceScore(0.70)
                .sourceReference("USDA AMS Spice Import Program — Chapter 09 — https://www.ams.usda.gov/services/imports-exports/spices").build());

        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg4.getId()).documentName("FDA Prior Notice of Imported Food").mandatory(true).build());
        documentRepository.save(RegulationDocumentEntity.builder()
                .regulationId(reg4.getId()).documentName("USDA Phytosanitary Certificate").mandatory(true).build());

        // NOTE: No regulation created for chapter 71 (Jewellery) → will produce NOT_FOUND
    }

    // -------------------------------------------------------------------------
    // TESTS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: HS Inventory Audit — Verify hs_master counts for seeded data")
    public void testHsInventoryAudit() {
        HsInventoryAuditService.GlobalHsInventoryReportDto report = inventoryService.buildGlobalInventory();

        assertNotNull(report);
        assertEquals(11, report.getCountriesAudited());

        // United States has 5 seeded HS codes
        HsInventoryAuditService.CountryHsInventoryDto usInv = report.getCountryInventories().get("United States");
        assertNotNull(usInv);
        assertEquals(5, usInv.getTotalHsCodes(), "US should have 5 seeded HS codes");
        assertEquals(5, usInv.getHs2Count(), "US should have 5 distinct chapters (09, 30, 33, 61, 71)");
        assertEquals(5, usInv.getHs4Count(), "US should have 5 distinct headings");
        assertEquals(5, usInv.getHs6Count(), "US should have 5 distinct HS6 codes");
        assertEquals(5, usInv.getNationalCodeCount(), "US should have 5 national (8-digit) codes");

        System.out.println("\n=== HS CODE INVENTORY AUDIT ===");
        System.out.printf("%-30s | Total: %6d | HS2: %4d | HS4: %5d | HS6: %5d | National(8+): %6d%n",
                "United States", usInv.getTotalHsCodes(), usInv.getHs2Count(), usInv.getHs4Count(),
                usInv.getHs6Count(), usInv.getNationalCodeCount());
    }

    @Test
    @DisplayName("Test 2: Coverage Build — Verify 4-level HS hierarchy matching")
    public void testHsRegulatoryCoverageMatchingLogic() {
        HsRegulatoryCoverageService.CountryCoverageReportDto report =
                coverageService.buildCoverageForCountry("United States");

        assertNotNull(report);
        assertEquals("United States", report.getCountry());
        assertEquals(5, report.getTotalHsCodes(), "US should process 5 seeded HS codes");
        assertEquals(4, report.getMappedHsCodes(), "4 of 5 HS codes should be matched");
        assertEquals(1, report.getUnmappedHsCodes(), "1 HS code (711319) should be NOT_FOUND");
        assertEquals(1, report.getNoSpecificRegulationFound(), "Exactly 1 NOT_FOUND expected");
        assertEquals(80.0, report.getCoveragePercent(), 0.1, "Coverage should be 80.00%");

        // Verify individual audit rows
        var exactMatch = coverageAuditRepository.findByCountryAndHsCode("United States", "61091000");
        assertTrue(exactMatch.isPresent(), "61091000 must be in coverage audit");
        assertEquals(HsRegulatoryCoverageService.METHOD_EXACT, exactMatch.get().getMappingMethod(), "61091000 should match EXACT_NATIONAL_CODE");
        assertEquals(3, exactMatch.get().getDocumentsFound(), "61091000 should have 3 documents");
        assertEquals(1, exactMatch.get().getCertificationsFound(), "61091000 should have 1 certification");

        var hs6Match = coverageAuditRepository.findByCountryAndHsCode("United States", "33049900");
        assertTrue(hs6Match.isPresent(), "33049900 must be in coverage audit");
        assertEquals(HsRegulatoryCoverageService.METHOD_HS6, hs6Match.get().getMappingMethod(), "33049900 should match HS6");

        var hs4Match = coverageAuditRepository.findByCountryAndHsCode("United States", "30049010");
        assertTrue(hs4Match.isPresent(), "30049010 must be in coverage audit");
        assertEquals(HsRegulatoryCoverageService.METHOD_HS4, hs4Match.get().getMappingMethod(), "30049010 should match HS4_HEADING");

        var hs2Match = coverageAuditRepository.findByCountryAndHsCode("United States", "09041100");
        assertTrue(hs2Match.isPresent(), "09041100 must be in coverage audit");
        assertEquals(HsRegulatoryCoverageService.METHOD_HS2, hs2Match.get().getMappingMethod(), "09041100 should match HS2_CHAPTER");

        var notFound = coverageAuditRepository.findByCountryAndHsCode("United States", "71131900");
        assertTrue(notFound.isPresent(), "71131900 must be in coverage audit");
        assertEquals(HsRegulatoryCoverageService.STATUS_NOT_FOUND, notFound.get().getCoverageStatus(), "71131900 should be NOT_FOUND (no regulation for chapter 71)");
        assertEquals(HsRegulatoryCoverageService.METHOD_NOT_FOUND, notFound.get().getMappingMethod(), "71131900 should have NOT_FOUND mapping method");
        assertFalse(notFound.get().getRegulationFound(), "71131900 should have regulation_found=false");
        assertNull(notFound.get().getRegulationId(), "71131900 must not have a regulation_id (no fabrication)");

        System.out.println("\n=== COVERAGE MATCHING TEST RESULTS ===");
        System.out.printf("61091000 → %s (conf=%.2f)%n", exactMatch.get().getMappingMethod(), exactMatch.get().getConfidenceScore());
        System.out.printf("33049900 → %s (conf=%.2f)%n", hs6Match.get().getMappingMethod(), hs6Match.get().getConfidenceScore());
        System.out.printf("30049010 → %s (conf=%.2f)%n", hs4Match.get().getMappingMethod(), hs4Match.get().getConfidenceScore());
        System.out.printf("09041100 → %s (conf=%.2f)%n", hs2Match.get().getMappingMethod(), hs2Match.get().getConfidenceScore());
        System.out.printf("71131900 → %s (NO FABRICATION)%n", notFound.get().getMappingMethod());
        System.out.printf("Coverage: %d/%d = %.2f%%%n", report.getMappedHsCodes(), report.getTotalHsCodes(), report.getCoveragePercent());
    }

    @Test
    @DisplayName("Test 3: Data Quality — Zero duplicates, zero fabricated data, valid statuses")
    public void testCoverageDataQuality() {
        coverageService.buildCoverageForCountry("United States");

        List<HsRegulatoryCoverageAuditEntity> allRows = coverageAuditRepository.findByCountry("United States");
        assertFalse(allRows.isEmpty(), "Coverage audit should have rows for United States");

        // Zero duplicates
        Set<String> uniqueKeys = allRows.stream()
                .map(r -> r.getCountry() + "|" + r.getHsCode())
                .collect(Collectors.toSet());
        assertEquals(allRows.size(), uniqueKeys.size(), "ZERO DUPLICATES expected");

        // Valid statuses
        Set<String> validStatuses = Set.of(
                HsRegulatoryCoverageService.STATUS_COMPLETE,
                HsRegulatoryCoverageService.STATUS_PARTIAL,
                HsRegulatoryCoverageService.STATUS_NOT_FOUND,
                HsRegulatoryCoverageService.STATUS_UNAVAILABLE);

        for (HsRegulatoryCoverageAuditEntity row : allRows) {
            assertNotNull(row.getCountry());
            assertNotNull(row.getHsCode());
            assertFalse(row.getHsCode().isBlank());
            assertTrue(validStatuses.contains(row.getCoverageStatus()),
                    "Invalid status: " + row.getCoverageStatus());
            assertTrue(row.getDocumentsFound() >= 0, "Documents count must be >= 0");
            assertTrue(row.getCertificationsFound() >= 0, "Certifications count must be >= 0");
            assertTrue(row.getRestrictionsFound() >= 0, "Restrictions count must be >= 0");
            assertTrue(row.getConfidenceScore() >= 0.0, "Confidence must be >= 0.0");
            assertTrue(row.getConfidenceScore() <= 1.0, "Confidence must be <= 1.0");

            if (Boolean.TRUE.equals(row.getRegulationFound())) {
                assertNotNull(row.getRegulationId(), "regulation_id required when regulation_found=true");
                assertNotNull(row.getMappingMethod());
                assertNotEquals(HsRegulatoryCoverageService.METHOD_NOT_FOUND, row.getMappingMethod());
                assertNotNull(row.getSourceUrl(), "Source URL must exist for grounded regulations");
            }

            if (Boolean.FALSE.equals(row.getRegulationFound())) {
                assertNull(row.getRegulationId(), "regulation_id MUST be null when NOT_FOUND (no fabrication)");
                assertTrue(
                        HsRegulatoryCoverageService.STATUS_NOT_FOUND.equals(row.getCoverageStatus()) ||
                                HsRegulatoryCoverageService.STATUS_UNAVAILABLE.equals(row.getCoverageStatus()));
            }
        }

        long notFound = allRows.stream().filter(r -> HsRegulatoryCoverageService.STATUS_NOT_FOUND.equals(r.getCoverageStatus())).count();
        System.out.println("\n=== DATA QUALITY AUDIT ===");
        System.out.println("Total audit rows:                " + allRows.size());
        System.out.println("Unique country+hs_code keys:     " + uniqueKeys.size());
        System.out.println("Duplicate rows:                  0 (verified)");
        System.out.println("Rows with fabricated data:       0 (verified)");
        System.out.println("Rows with missing source URL:    0 (all regulation_found=true rows have source_url)");
        System.out.println("NO_SPECIFIC_REG_FOUND rows:      " + notFound);
        System.out.println("All assertions PASSED");
    }

    @Test
    @DisplayName("Test 4: Compliance Profile Query — Returns correct evidence for HS code")
    public void testHsCodeComplianceProfileQuery() {
        coverageService.buildCoverageForCountry("United States");

        // Query exact match — should return full evidence
        HsRegulatoryCoverageService.HsCodeComplianceProfileDto profile =
                coverageService.queryCoverageByHsCode("United States", "61091000");
        assertNotNull(profile);
        assertEquals("United States", profile.getCountry());
        assertEquals("61091000", profile.getHsCode());
        assertEquals(HsRegulatoryCoverageService.METHOD_EXACT, profile.getMappingMethod());
        assertNotNull(profile.getRegulationTitle());
        assertNotNull(profile.getAuthority());
        assertNotNull(profile.getSourceUrl());
        assertTrue(profile.getDocumentsFound() >= 3, "Should have ≥3 documents");

        // Query NOT_FOUND — should return empty profile, not throw
        HsRegulatoryCoverageService.HsCodeComplianceProfileDto notFoundProfile =
                coverageService.queryCoverageByHsCode("United States", "71131900");
        assertNotNull(notFoundProfile);
        assertEquals(HsRegulatoryCoverageService.STATUS_NOT_FOUND, notFoundProfile.getCoverageStatus());

        System.out.println("\n=== COMPLIANCE PROFILE QUERY ===");
        System.out.println("HS Code 61091000:");
        System.out.println("  Regulation:  " + profile.getRegulationTitle());
        System.out.println("  Authority:   " + profile.getAuthority());
        System.out.println("  Method:      " + profile.getMappingMethod());
        System.out.println("  Confidence:  " + profile.getConfidenceScore());
        System.out.println("  Source URL:  " + profile.getSourceUrl());
        System.out.println("  Documents:   " + profile.getDocumentsFound());
        System.out.println("  Certs:       " + profile.getCertificationsFound());
        System.out.println("  Restrictions:" + profile.getRestrictionsFound());
        System.out.println("HS Code 71131900 (Jewellery):");
        System.out.println("  Status:      " + notFoundProfile.getCoverageStatus() + " (no fabrication)");
    }
}
