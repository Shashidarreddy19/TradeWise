package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.CanadaPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CanadaPipelineIntegrationTest {

    @Autowired
    private CanadaPipelineProcessorService canadaPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCanadaCustomsTariffPdfPipelineExecution() {
        CanadaPipelineProcessorService.CanadaEtlExecutionReport report =
                canadaPipelineProcessorService.processCanadaPipeline("01-99-2026-eng.pdf", "CANADA_TARIFF_2026");

        assertNotNull(report);
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("Canada", report.getCountry());
        assertEquals("01-99-2026-eng.pdf", report.getPdfFileName());
        assertTrue(report.getPagesScanned() > 0);
        assertTrue(report.getChaptersFound() > 0);
        assertTrue(report.getSectionsFound() > 0);
        assertTrue(report.getRecordsExtracted() > 0);
        assertTrue(report.getRecordsLoadedHsMaster() > 0);

        System.out.println("================================================================================");
        System.out.println("CANADA CUSTOMS TARIFF PDF ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Country: " + report.getCountry());
        System.out.println("PDF File Name: " + report.getPdfFileName());
        System.out.println("Pages Scanned: " + report.getPagesScanned());
        System.out.println("Chapters Found: " + report.getChaptersFound());
        System.out.println("Sections Found: " + report.getSectionsFound());
        System.out.println("Tariff Codes Extracted: " + report.getRecordsExtracted());
        System.out.println("Rows Inserted into hs_raw: " + report.getRecordsInsertedHsRaw());
        System.out.println("Rows Validated: " + report.getRecordsValidated());
        System.out.println("Rows Rejected: " + report.getRecordsRejected());
        System.out.println("Rows Category Mapped: " + report.getRecordsCategoryMapped());
        System.out.println("Rows Loaded into hs_master: " + report.getRecordsLoadedHsMaster());
        System.out.println("Rows Loaded into hs_versions: " + report.getRecordsLoadedHsVersions());
        System.out.println("Execution Time: " + report.getExecutionTimeMs() + " ms");

        // Verification SQL Queries
        Long totalCaRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country='Canada'", Long.class);
        Long totalCaValidated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_validated WHERE country='Canada'", Long.class);
        Long totalCaMaster = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Canada'", Long.class);
        Long nullNationalCodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Canada' AND national_code IS NULL", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Canada' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Canada' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total Canada hs_raw: " + totalCaRaw);
        System.out.println("2. Total Canada hs_validated: " + totalCaValidated);
        System.out.println("3. Total Canada hs_master (Supported 21 Chapters): " + totalCaMaster);
        System.out.println("5. Null national_code count in Canada hs_master: " + nullNationalCodes);
        System.out.println("7. Null descriptions in Canada hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in Canada hs_master: " + invalidHierarchy);

        assertEquals(0L, nullNationalCodes);
        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        List<Map<String, Object>> duplicateCheck = jdbcTemplate.queryForList(
                "SELECT national_code, COUNT(*) AS total FROM hs_master WHERE country='Canada' GROUP BY national_code HAVING COUNT(*) > 1");
        System.out.println("6. Duplicate national_code count in Canada hs_master: " + duplicateCheck.size());
        assertEquals(0, duplicateCheck.size());

        System.out.println("\n--- RECORDS PER CHAPTER IN CANADA hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList(
                "SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country='Canada' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
