package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.UkTradeTariffEtlProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UkTradeTariffPipelineIntegrationTest {

    @Autowired
    private UkTradeTariffEtlProcessorService ukTradeTariffEtlProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testLiveUkTradeTariffOAuth2AndEtlPipelineExecution() {
        UkTradeTariffEtlProcessorService.UkEtlSummary summary =
                ukTradeTariffEtlProcessorService.processUkPipeline("UK_TARIFF_2026");

        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertEquals("United Kingdom", summary.getCountry());
        assertTrue(summary.getTotalDownloadedBytes() > 0);
        assertTrue(summary.getTotalHeadingsDownloaded() > 0);
        assertTrue(summary.getTotalCommoditiesDownloaded() > 0);
        assertTrue(summary.getTotalRawExtracted() > 0);
        assertTrue(summary.getTotalMasterLoaded() > 0);

        System.out.println("================================================================================");
        System.out.println("UNITED KINGDOM HMRC TRADE TARIFF OAUTH2 & ETL LIVE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Downloaded Bytes: " + summary.getTotalDownloadedBytes());
        System.out.println("Headings Downloaded from HMRC API: " + summary.getTotalHeadingsDownloaded());
        System.out.println("Commodities Downloaded from HMRC API: " + summary.getTotalCommoditiesDownloaded());
        System.out.println("Raw Extracted: " + summary.getTotalRawExtracted());
        System.out.println("Validated Inserted: " + summary.getTotalValidatedInserted());
        System.out.println("Master Loaded: " + summary.getTotalMasterLoaded());
        System.out.println("Execution Time: " + summary.getExecutionTimeMs() + " ms");

        Long totalUkRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'United Kingdom'", Long.class);
        Long totalUkValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'United Kingdom'", Long.class);
        Long totalUkMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United Kingdom'", Long.class);
        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United Kingdom' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'United Kingdom' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- DATABASE VERIFICATION METRICS ---");
        System.out.println("1. Total UK hs_raw: " + totalUkRaw);
        System.out.println("2. Total UK hs_validated: " + totalUkValidated);
        System.out.println("3. Total UK hs_master (Supported 21 Chapters): " + totalUkMaster);
        System.out.println("7. Null descriptions in UK hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in UK hs_master: " + invalidHierarchy);

        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        System.out.println("\n--- RECORDS PER CHAPTER IN UK hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList("SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country = 'United Kingdom' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
