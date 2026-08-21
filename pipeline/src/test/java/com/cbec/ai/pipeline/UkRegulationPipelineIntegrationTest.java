package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.ukregulation.service.UkRegulationPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UkRegulationPipelineIntegrationTest {

    @Autowired
    private UkRegulationPipelineProcessorService ukRegulationPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUkRegulationPipelineExecution() {
        // Run pipeline first time
        UkRegulationPipelineProcessorService.UkRegulationExecutionReport report =
                ukRegulationPipelineProcessorService.processUkRegulations();

        assertNotNull(report);
        assertEquals("SUCCESS", report.getStatus());
        assertEquals("United Kingdom", report.getCountry());
        assertTrue(report.getSources() >= 5);
        assertTrue(report.getDownloaded() > 0);
        assertTrue(report.getRecordsInserted() > 0);

        System.out.println("================================================================================");
        System.out.println("UK REGULATION INTELLIGENCE ETL PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Country: " + report.getCountry());
        System.out.println("Total Sources Managed: " + report.getSources());
        System.out.println("Documents Downloaded: " + report.getDownloaded());
        System.out.println("HTML Documents Extracted: " + report.getHtmlExtracted());
        System.out.println("PDF Documents Extracted: " + report.getPdfExtracted());
        System.out.println("Raw Regulation Records Inserted: " + report.getRecordsInserted());

        // Verification SQL Queries
        Long sourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_source WHERE country='United Kingdom'", Long.class);
        Long downloadCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_download_history WHERE country='United Kingdom'", Long.class);
        Long rawCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_raw WHERE country='United Kingdom'", Long.class);
        Long sha256ValidCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_download_history WHERE country='United Kingdom' AND sha256 IS NOT NULL AND sha256 != '' AND sha256 != 'FAILED'", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total UK Sources in regulation_source: " + sourceCount);
        System.out.println("2. Total UK Download History Entries in regulation_download_history: " + downloadCount);
        System.out.println("3. Total Valid SHA-256 Hashes Generated: " + sha256ValidCount);
        System.out.println("4. Total UK Raw Regulation Records in regulation_raw: " + rawCount);

        assertTrue(sourceCount >= 5);
        assertTrue(downloadCount > 0);
        assertTrue(sha256ValidCount > 0);
        assertTrue(rawCount > 0);

        // Test Idempotency: Re-running pipeline should not duplicate regulation sources
        UkRegulationPipelineProcessorService.UkRegulationExecutionReport report2 =
                ukRegulationPipelineProcessorService.processUkRegulations();

        Long sourceCountAfterIdempotency = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_source WHERE country='United Kingdom'", Long.class);

        assertEquals(sourceCount, sourceCountAfterIdempotency, "Source count should remain identical on idempotent re-run");
        System.out.println("5. Pipeline Idempotency Verified (Source Count After Re-Run: " + sourceCountAfterIdempotency + ")");
        System.out.println("================================================================================");
    }
}
