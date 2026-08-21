package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.UaeGccPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UaeGccPipelineIntegrationTest {

    @Autowired
    private UaeGccPipelineProcessorService uaeGccPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUaeGccExcelPipelineExecution() {
        UaeGccPipelineProcessorService.UaeEtlExecutionReport report =
                uaeGccPipelineProcessorService.processUaePipeline("HSCodeMaster-v3.3customers.xlsx", "GCC_TARIFF_v3.3");

        assertNotNull(report);
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("HSCodeMaster-v3.3customers.xlsx", report.getWorkbookName());
        assertEquals("HSCodeMaster", report.getWorksheetName());
        assertTrue(report.getRowsScanned() > 0);
        assertTrue(report.getRowsExtracted() > 0);
        assertTrue(report.getRowsInsertedHsMaster() > 0);

        System.out.println("================================================================================");
        System.out.println("UNITED ARAB EMIRATES GCC CUSTOMS TARIFF EXCEL ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Workbook Name: " + report.getWorkbookName());
        System.out.println("Worksheet Name: " + report.getWorksheetName());
        System.out.println("Total Worksheets: " + report.getTotalWorksheets());
        System.out.println("Rows Scanned: " + report.getRowsScanned());
        System.out.println("Rows Extracted: " + report.getRowsExtracted());
        System.out.println("Rows Rejected: " + report.getRowsRejected());
        System.out.println("Rows Inserted into hs_raw: " + report.getRowsInsertedHsRaw());
        System.out.println("Rows Inserted into hs_validated: " + report.getRowsInsertedHsValidated());
        System.out.println("Rows Inserted into hs_master: " + report.getRowsInsertedHsMaster());
        System.out.println("Rows Inserted into hs_versions: " + report.getRowsInsertedHsVersions());
        System.out.println("Execution Time: " + report.getExecutionTimeMs() + " ms");

        // Verification SQL Queries
        Long totalUaeRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country='United Arab Emirates'", Long.class);
        Long totalUaeValidated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_validated WHERE country='United Arab Emirates'", Long.class);
        Long totalUaeMaster = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='United Arab Emirates'", Long.class);
        Long nullNationalCodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='United Arab Emirates' AND national_code IS NULL", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='United Arab Emirates' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='United Arab Emirates' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total UAE hs_raw: " + totalUaeRaw);
        System.out.println("2. Total UAE hs_validated: " + totalUaeValidated);
        System.out.println("3. Total UAE hs_master (Supported 21 Chapters): " + totalUaeMaster);
        System.out.println("5. Null national_code count in UAE hs_master: " + nullNationalCodes);
        System.out.println("7. Null descriptions in UAE hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in UAE hs_master: " + invalidHierarchy);

        assertEquals(0L, nullNationalCodes);
        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        List<Map<String, Object>> duplicateCheck = jdbcTemplate.queryForList(
                "SELECT national_code, COUNT(*) AS total FROM hs_master WHERE country='United Arab Emirates' GROUP BY national_code HAVING COUNT(*) > 1");
        System.out.println("6. Duplicate national_code count in UAE hs_master: " + duplicateCheck.size());
        assertEquals(0, duplicateCheck.size());

        System.out.println("\n--- RECORDS PER CHAPTER IN UAE hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList(
                "SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country='United Arab Emirates' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
