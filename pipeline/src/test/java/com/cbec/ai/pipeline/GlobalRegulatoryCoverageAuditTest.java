package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.regulation.service.GlobalCoverageAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GlobalRegulatoryCoverageAuditTest {

    @Autowired
    private GlobalCoverageAuditService auditService;

    @Test
    @DisplayName("Test 1: Real Regulatory Data Quality & Evidence Audit Across 11 Target Markets")
    public void testGlobalCoverageAudit() {
        GlobalCoverageAuditService.GlobalCoverageReportDto report = auditService.executeGlobalIngestionAndAudit();

        assertNotNull(report);
        assertEquals(11, report.getTotalCountriesAudited(), "Must audit all 11 target markets");
        assertTrue(report.getTotalSourcesCollected() > 0, "Collected official sources must be greater than zero");
        assertTrue(report.getOverallDataQualityScore() >= 80.0, "Global Data Quality Score must exceed 80.0%");
        assertNotNull(report.getCountryReports());
        assertEquals(11, report.getCountryReports().size());

        report.getCountryReports().forEach(cr -> {
            assertNotNull(cr.getCountry());
            assertTrue(cr.getSourceCoveragePercent() > 0, "Source coverage percent must be > 0 for " + cr.getCountry());
            assertTrue(cr.getDownloadSuccessPercent() > 0, "Download success percent must be > 0 for " + cr.getCountry());
            assertTrue(cr.getEvidenceTraceabilityPercent() >= 90.0, "Evidence traceability must be >= 90.0% for " + cr.getCountry());
            assertNotNull(cr.getManualAuditSamples());
            assertFalse(cr.getManualAuditSamples().isEmpty(), "Manual audit representative samples must be present for " + cr.getCountry());
            assertNotNull(cr.getFinalStatus());
            assertFalse(cr.getFinalStatus().isBlank());
        });
    }
}
