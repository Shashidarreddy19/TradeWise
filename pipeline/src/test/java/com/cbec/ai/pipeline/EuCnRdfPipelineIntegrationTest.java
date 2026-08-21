package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.EuCnRdfPipelineProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EuCnRdfPipelineIntegrationTest {

    @Autowired
    private EuCnRdfPipelineProcessorService euCnRdfPipelineProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEuCnRdfPipelineExecutionForGermanyAndNetherlands() {
        // Step 1: Execute for Germany
        EuCnRdfPipelineProcessorService.EuCnRdfSummary germanySummary =
                euCnRdfPipelineProcessorService.processEuCnRdfPipeline("Germany", "ESTAT-CN2026.rdf", "CN_2026");

        assertNotNull(germanySummary);
        assertEquals("COMPLETED", germanySummary.getStatus());
        assertEquals("Germany", germanySummary.getCountry());
        assertTrue(germanySummary.getTotalRawExtracted() > 0);
        assertTrue(germanySummary.getTotalMasterLoaded() > 0);

        // Step 2: Execute for Netherlands (reusing same StAX streaming engine)
        EuCnRdfPipelineProcessorService.EuCnRdfSummary netherlandsSummary =
                euCnRdfPipelineProcessorService.processEuCnRdfPipeline("Netherlands", "ESTAT-CN2026.rdf", "CN_2026");

        assertNotNull(netherlandsSummary);
        assertEquals("COMPLETED", netherlandsSummary.getStatus());
        assertEquals("Netherlands", netherlandsSummary.getCountry());
        assertTrue(netherlandsSummary.getTotalRawExtracted() > 0);
        assertTrue(netherlandsSummary.getTotalMasterLoaded() > 0);

        // Step 3: Database Verification Queries
        System.out.println("================================================================================");
        System.out.println("EU COMBINED NOMENCLATURE CN 2026 RDF/XML STAX PIPELINE VERIFICATION REPORT");
        System.out.println("================================================================================");
        System.out.println("Germany Concepts Extracted: " + germanySummary.getTotalRawExtracted() + " | Master Loaded: " + germanySummary.getTotalMasterLoaded());
        System.out.println("Netherlands Concepts Extracted: " + netherlandsSummary.getTotalRawExtracted() + " | Master Loaded: " + netherlandsSummary.getTotalMasterLoaded());

        Long germanyRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'Germany'", Long.class);
        Long germanyValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'Germany'", Long.class);
        Long germanyMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'Germany'", Long.class);

        Long netherlandsRaw = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE country = 'Netherlands'", Long.class);
        Long netherlandsValidated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_validated WHERE country = 'Netherlands'", Long.class);
        Long netherlandsMaster = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE country = 'Netherlands'", Long.class);

        Long nullDescriptions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE customs_territory = 'EU' AND (official_description IS NULL OR official_description = '')", Long.class);
        Long invalidHierarchy = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_master WHERE customs_territory = 'EU' AND (SUBSTRING(national_code, 1, 2) != chapter OR SUBSTRING(national_code, 1, 4) != heading OR SUBSTRING(national_code, 1, 6) != hs6)", Long.class);

        System.out.println("\n--- GERMANY DATABASE METRICS ---");
        System.out.println("1. Germany hs_raw: " + germanyRaw);
        System.out.println("2. Germany hs_validated: " + germanyValidated);
        System.out.println("3. Germany hs_master (Supported 21 Chapters): " + germanyMaster);

        System.out.println("\n--- NETHERLANDS DATABASE METRICS ---");
        System.out.println("1. Netherlands hs_raw: " + netherlandsRaw);
        System.out.println("2. Netherlands hs_validated: " + netherlandsValidated);
        System.out.println("3. Netherlands hs_master (Supported 21 Chapters): " + netherlandsMaster);

        System.out.println("\n--- SHARED EU INTEGRITY METRICS ---");
        System.out.println("7. Null descriptions in EU hs_master: " + nullDescriptions);
        System.out.println("8. Invalid hierarchy rows in EU hs_master: " + invalidHierarchy);

        assertEquals(0L, nullDescriptions);
        assertEquals(0L, invalidHierarchy);

        System.out.println("\n--- RECORDS PER CHAPTER IN GERMANY hs_master ---");
        List<Map<String, Object>> chRows = jdbcTemplate.queryForList("SELECT chapter, COUNT(*) AS total FROM hs_master WHERE country = 'Germany' GROUP BY chapter ORDER BY chapter");
        for (Map<String, Object> r : chRows) {
            System.out.printf("Chapter %-5s | %d%n", r.get("chapter"), ((Number) r.get("total")).longValue());
        }

        System.out.println("================================================================================");
    }
}
