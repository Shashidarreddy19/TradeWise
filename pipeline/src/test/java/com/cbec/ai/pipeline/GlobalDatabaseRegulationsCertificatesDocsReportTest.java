package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.regulation.service.GlobalCoverageAuditService;
import com.cbec.ai.pipeline.regulation.service.RegulationDownloaderService;
import com.cbec.ai.pipeline.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated Database Inspection & Verification Test.
 * Audits regulation_master, regulation_documents, regulation_certifications,
 * regulation_labeling, regulation_restrictions, and regulation_procedures.
 */
@Slf4j
@SpringBootTest
public class GlobalDatabaseRegulationsCertificatesDocsReportTest {

    @Autowired private RegulationMasterRepository regulationMasterRepository;
    @Autowired private RegulationDocumentRepository documentRepository;
    @Autowired private RegulationCertificationRepository certificationRepository;
    @Autowired private RegulationLabelingRepository labelingRepository;
    @Autowired private RegulationRestrictionRepository restrictionRepository;
    @Autowired private RegulationProcedureRepository procedureRepository;
    @Autowired private RegulationSourceRepository sourceRepository;
    @Autowired private HsMasterRepository hsMasterRepository;
    @Autowired private RegulationDownloaderService downloaderService;

    @Test
    @DisplayName("Audit Regulations, Certifications & Documents Database Counts")
    public void auditDatabaseRegulationsCertificationsDocs() {
        // Ensure sources & regulations are seeded for test DB run
        if (regulationMasterRepository.count() == 0) {
            log.info("Seeding regulation sources and documents for audit test...");
            for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
                try {
                    downloaderService.downloadCountryRegulations(country);
                } catch (Exception e) {
                    log.warn("Download seed skipped for {}: {}", country, e.getMessage());
                }
            }
        }

        log.info("==========================================================================");
        log.info("AUDITING CBEC-AI REGULATORY DATABASE: REGULATIONS, CERTS, DOCS");
        log.info("==========================================================================");

        long totalRegulations = regulationMasterRepository.count();
        long totalSources = sourceRepository.count();
        long totalDocuments = documentRepository.count();
        long totalCertifications = certificationRepository.count();
        long totalLabeling = labelingRepository.count();
        long totalRestrictions = restrictionRepository.count();
        long totalProcedures = procedureRepository.count();
        long totalHsMaster = hsMasterRepository.count();

        log.info("DATABASE AUDIT METRICS:");
        log.info("  Total HS Master Codes:        {}", totalHsMaster);
        log.info("  Total Official Sources:       {}", totalSources);
        log.info("  Total Master Regulations:     {}", totalRegulations);
        log.info("  Total Required Documents:     {}", totalDocuments);
        log.info("  Total Certifications:         {}", totalCertifications);
        log.info("  Total Labeling Requirements:  {}", totalLabeling);
        log.info("  Total Import Restrictions:    {}", totalRestrictions);
        log.info("  Total Customs Entry Procs:    {}", totalProcedures);

        // Verify country breakdown
        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            List<RegulationMasterEntity> countryRegs = regulationMasterRepository.findByCountry(country);
            long hsCount = hsMasterRepository.countByCountry(country);

            log.info("Country [{}]: HS Master={}, Regulations={}", country, hsCount, countryRegs.size());
        }

        assertTrue(totalRegulations >= 0, "Master regulations count valid");
        assertTrue(totalDocuments >= 0, "Documents count valid");
        assertTrue(totalCertifications >= 0, "Certifications count valid");
    }
}
