package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.regulation.service.ComplianceQueryService;
import com.cbec.ai.pipeline.regulation.service.RegulationAuditService;
import com.cbec.ai.pipeline.regulation.service.RegulationPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class ComplianceQueryEngineIntegrationTest {

    @Autowired
    private RegulationPipelineService pipelineService;

    @Autowired
    private RegulationAuditService auditService;

    @Autowired
    private ComplianceQueryService complianceQueryService;

    @Test
    @DisplayName("Test Global Database Audit Endpoint & Service")
    public void testGlobalDatabaseAudit() {
        log.info("Testing Global Database Audit Service...");

        // Ensure UK, US, Canada regulations are populated
        pipelineService.processRegulationsForCountry("United Kingdom");
        pipelineService.processRegulationsForCountry("United States");
        pipelineService.processRegulationsForCountry("Canada");

        RegulationAuditService.GlobalRegulationAuditReportDto report = auditService.generateAuditReport();

        assertNotNull(report);
        assertEquals(11, report.getTotalCountriesCovered());
        assertNotNull(report.getCountryAudits());
        assertFalse(report.getCountryAudits().isEmpty());

        log.info("Global Database Audit Report generated successfully. Total countries: {}", report.getTotalCountriesCovered());
    }

    @Test
    @DisplayName("Test Compliance Query Engine for UK, United States, and Canada")
    public void testComplianceQueryEngine() {
        log.info("Testing Compliance Query Engine API...");

        // 1. United States Compliance Query Test
        pipelineService.processRegulationsForCountry("United States");
        ComplianceQueryService.ComplianceCheckRequestDto usReq = ComplianceQueryService.ComplianceCheckRequestDto.builder()
                .country("United States")
                .hsCode("33049900")
                .product("Cosmetic cream")
                .build();

        ComplianceQueryService.ComplianceCheckResponseDto usResp = complianceQueryService.checkCompliance(usReq);
        assertNotNull(usResp);
        assertEquals("United States", usResp.getCountry());
        assertEquals("33049900", usResp.getHsCode());
        assertFalse(usResp.getRegulations().isEmpty(), "US Compliance Check should return matched regulations");
        assertNotNull(usResp.getConfidence());
        assertTrue(usResp.getConfidence() > 0.0);
        log.info("US Compliance Check passed with {} regulations and confidence {}", usResp.getRegulations().size(), usResp.getConfidence());

        // 2. United Kingdom Compliance Query Test
        pipelineService.processRegulationsForCountry("United Kingdom");
        ComplianceQueryService.ComplianceCheckRequestDto ukReq = ComplianceQueryService.ComplianceCheckRequestDto.builder()
                .country("United Kingdom")
                .hsCode("85171200")
                .product("Mobile smartphone")
                .build();

        ComplianceQueryService.ComplianceCheckResponseDto ukResp = complianceQueryService.checkCompliance(ukReq);
        assertNotNull(ukResp);
        assertEquals("United Kingdom", ukResp.getCountry());
        assertFalse(ukResp.getRegulations().isEmpty(), "UK Compliance Check should return matched regulations");

        // Detailed verification of UK regulation evidence and structure
        ComplianceQueryService.RegulationMasterDto ukReg = ukResp.getRegulations().get(0);
        assertNotNull(ukReg.getId(), "UK Regulation ID must be present");
        assertNotNull(ukReg.getAuthority(), "UK Regulation authority must be present");
        assertNotNull(ukReg.getSummary(), "UK Regulation summary must be present");
        assertNotNull(ukReg.getSourceUrl(), "UK Regulation source URL must be present");
        assertTrue(ukReg.getSourceUrl().startsWith("http"), "UK Regulation source URL must be a valid HTTP/HTTPS URL");

        assertFalse(ukResp.getSources().isEmpty(), "UK Compliance check should return official sources");
        assertNotNull(ukResp.getSources().get(0).getSourceUrl(), "UK Source URL must not be null");
        assertNotNull(ukResp.getSources().get(0).getAuthority(), "UK Authority must not be null");

        // Verify presence of applicable regulatory requirements (documents/certifications/labeling/restrictions/procedures)
        boolean hasReqs = !ukResp.getDocuments().isEmpty() || !ukResp.getCertifications().isEmpty()
                || !ukResp.getLabeling().isEmpty() || !ukResp.getRestrictions().isEmpty()
                || !ukResp.getProcedures().isEmpty();
        assertTrue(hasReqs, "UK Compliance Check must return actual regulatory requirements");
        log.info("UK Compliance Check passed with {} regulation(s), source URL: {}", ukResp.getRegulations().size(), ukReg.getSourceUrl());

        // 3. Canada Compliance Query Test
        pipelineService.processRegulationsForCountry("Canada");
        ComplianceQueryService.ComplianceCheckRequestDto caReq = ComplianceQueryService.ComplianceCheckRequestDto.builder()
                .country("Canada")
                .hsCode("02011000")
                .product("Bovine carcasses")
                .build();

        ComplianceQueryService.ComplianceCheckResponseDto caResp = complianceQueryService.checkCompliance(caReq);
        assertNotNull(caResp);
        assertEquals("Canada", caResp.getCountry());
        assertFalse(caResp.getRegulations().isEmpty(), "Canada Compliance Check should return matched regulations");
        log.info("All Compliance Query Engine Integration Tests PASSED!");
    }
}
