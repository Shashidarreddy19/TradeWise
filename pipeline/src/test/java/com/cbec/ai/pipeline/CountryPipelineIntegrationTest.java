package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.service.HsEtlPipelineOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CountryPipelineIntegrationTest {

    @Autowired
    private HsEtlPipelineOrchestrator orchestrator;

    @Test
    void testIndiaDgftPipelineExecution() {
        ExtractionReportDto report = orchestrator.runPipelineForCountry("India");
        assertNotNull(report);
        assertEquals("India", report.getCountry());
        assertTrue(report.getTotalRecordsFound() >= 0);
    }

    @Test
    void testUnitedStatesHtsPipelineExecution() {
        ExtractionReportDto report = orchestrator.runPipelineForCountry("United States");
        assertNotNull(report);
        assertEquals("United States", report.getCountry());
        assertTrue(report.getTotalRecordsFound() >= 0);
    }

    @Test
    void testEuGermanyTaricPipelineExecution() {
        ExtractionReportDto report = orchestrator.runPipelineForCountry("Germany");
        assertNotNull(report);
        assertEquals("Germany", report.getCountry());
        assertTrue(report.getTotalRecordsFound() >= 0);
    }

    @Test
    void testUkTradeTariffPipelineExecution() {
        ExtractionReportDto report = orchestrator.runPipelineForCountry("United Kingdom");
        assertNotNull(report);
        assertEquals("United Kingdom", report.getCountry());
        assertTrue(report.getTotalRecordsFound() >= 0);
    }
}
