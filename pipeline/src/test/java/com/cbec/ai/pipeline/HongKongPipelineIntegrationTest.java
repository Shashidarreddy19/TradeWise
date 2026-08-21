package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.HongKongPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HongKongPipelineIntegrationTest {

    @Autowired
    private HongKongPipelineProcessorService hongKongPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testHongKongCsvPipelineExecution() {
        HongKongPipelineProcessorService.HkEtlExecutionReport report =
                hongKongPipelineProcessorService.processHongKongPipeline("B2XX00232026XXXXB0100 (1).csv", "HKHS_2026");

        assertNotNull(report);
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("B2XX00232026XXXXB0100 (1).csv", report.getFileName());
        assertTrue(report.getRowsScanned() > 0);
        assertTrue(report.getRowsExtracted() > 0);
        assertTrue(report.getRowsInsertedHsMaster() > 0);

        System.out.println("================================================================================");
        System.out.println("HONG KONG HARMONIZED SYSTEM CSV ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("CSV File Name: " + report.getFileName());
        System.out.println("CSV Rows Scanned: " + report.getRowsScanned());
        System.out.println("Rows Extracted: " + report.getRowsExtracted());
        System.out.println("Rows Rejected: " + report.getRowsRejected());
        System.out.println("Rows Inserted into hs_raw: " + report.getRowsInsertedHsRaw());
        System.out.println("Rows Inserted into hs_validated: " + report.getRowsInsertedHsValidated());
        System.out.println("Rows Inserted into hs_master: " + report.getRowsInsertedHsMaster());
        System.out.println("Rows Inserted into hs_versions: " + report.getRowsInsertedHsVersions());
        System.out.println("Execution Time: " + report.getExecutionTimeMs() + " ms");

        // Verification SQL Queries
        Long totalHkRaw = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country='Hong Kong'", Long.class);
        Long totalHkValidated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_validated WHERE country='Hong Kong'", Long.class);
        Long totalHkMaster = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Hong Kong'", Long.class);
        Long nullNationalCodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Hong Kong' AND national_code IS NULL", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Hong Kong' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_master WHERE country='Hong Kong' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total HK hs_raw: " + totalHkRaw);
        System.out.println("2. Total HK hs_validated: " + totalHkValidated);
        System.out.println("3. Total HK hs_master (Supported 21 Chapters): " + totalHkMaster);
        System.out.println("5. Null national_code count in HK hs_master: " + nullNationalCodes);
        System.out.println("7. Null descriptions in HK hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in HK hs_master: " + invalidHierarchy);

        assertEquals(0L, nullNationalCodes);
        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        List<Map<String, Object>> duplicateCheck = jdbcTemplate.queryForList(
                "SELECT national_code, COUNT(*) AS total FROM hs_master WHERE country='Hong Kong' GROUP BY national_code HAVING COUNT(*) > 1");
        System.out.println("6. Duplicate national_code count in HK hs_master: " + duplicateCheck.size());
        assertEquals(0, duplicateCheck.size());

        System.out.println("\n--- RECORDS PER CHAPTER IN HK hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList(
                "SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country='Hong Kong' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
