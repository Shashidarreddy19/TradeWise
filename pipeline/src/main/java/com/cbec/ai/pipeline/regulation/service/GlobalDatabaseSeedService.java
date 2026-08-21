package com.cbec.ai.pipeline.regulation.service;

import com.cbec.ai.pipeline.repository.*;
import com.cbec.ai.pipeline.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Production Service that populates MySQL TradeData database using real official dataset files
 * (hts_2026_basic_edition_csv.csv, 관세청_HS부호_20260101.xlsx, 01-99-2026-eng.pdf,
 * B2XX00232026XXXXB0100 (1).csv, HSCodeMaster-v3.3customers.xlsx, ESTAT-CN2026.rdf, ITC-HS_2022.pdf)
 * and real official government regulatory documents.
 */
@Service
@Slf4j
public class GlobalDatabaseSeedService {

    private final HsMasterRepository hsMasterRepository;
    private final RegulationDownloaderService downloaderService;
    private final HsRegulatoryCoverageService coverageService;
    private final RegulationHsEvidenceVerificationService evidenceVerificationService;
    private final RegulationPipelineService regulationPipelineService;

    private final USHTSCsvPipelineProcessorService usHtsCsvPipelineProcessorService;
    private final SouthKoreaPipelineProcessorService southKoreaPipelineProcessorService;
    private final CanadaPipelineProcessorService canadaPipelineProcessorService;
    private final HongKongPipelineProcessorService hongKongPipelineProcessorService;
    private final UaeGccPipelineProcessorService uaeGccPipelineProcessorService;
    private final EuCnRdfPipelineProcessorService euCnRdfPipelineProcessorService;
    private final IndiaItcHsPdfExtractorService indiaItcHsPdfExtractorService;
    private final IndiaEtlPipelineProcessorService indiaEtlPipelineProcessorService;
    private final HsRegulatoryCoverageAuditRepository coverageAuditRepository;
    private final RegulationHsEvidenceVerificationRepository evidenceVerificationRepository;

    public GlobalDatabaseSeedService(
            HsMasterRepository hsMasterRepository,
            RegulationDownloaderService downloaderService,
            HsRegulatoryCoverageService coverageService,
            RegulationHsEvidenceVerificationService evidenceVerificationService,
            RegulationPipelineService regulationPipelineService,
            USHTSCsvPipelineProcessorService usHtsCsvPipelineProcessorService,
            SouthKoreaPipelineProcessorService southKoreaPipelineProcessorService,
            CanadaPipelineProcessorService canadaPipelineProcessorService,
            HongKongPipelineProcessorService hongKongPipelineProcessorService,
            UaeGccPipelineProcessorService uaeGccPipelineProcessorService,
            EuCnRdfPipelineProcessorService euCnRdfPipelineProcessorService,
            IndiaItcHsPdfExtractorService indiaItcHsPdfExtractorService,
            IndiaEtlPipelineProcessorService indiaEtlPipelineProcessorService,
            HsRegulatoryCoverageAuditRepository coverageAuditRepository,
            RegulationHsEvidenceVerificationRepository evidenceVerificationRepository) {
        this.hsMasterRepository = hsMasterRepository;
        this.downloaderService = downloaderService;
        this.coverageService = coverageService;
        this.evidenceVerificationService = evidenceVerificationService;
        this.regulationPipelineService = regulationPipelineService;
        this.usHtsCsvPipelineProcessorService = usHtsCsvPipelineProcessorService;
        this.southKoreaPipelineProcessorService = southKoreaPipelineProcessorService;
        this.canadaPipelineProcessorService = canadaPipelineProcessorService;
        this.hongKongPipelineProcessorService = hongKongPipelineProcessorService;
        this.uaeGccPipelineProcessorService = uaeGccPipelineProcessorService;
        this.euCnRdfPipelineProcessorService = euCnRdfPipelineProcessorService;
        this.indiaItcHsPdfExtractorService = indiaItcHsPdfExtractorService;
        this.indiaEtlPipelineProcessorService = indiaEtlPipelineProcessorService;
        this.coverageAuditRepository = coverageAuditRepository;
        this.evidenceVerificationRepository = evidenceVerificationRepository;
    }

    @Transactional
    public void purgeSampleDataIfPresent() {
        long count = hsMasterRepository.count();
        if (count > 0 && count <= 500) {
            log.info("Purging legacy 345 sample records from MySQL TradeData database...");
            try {
                coverageAuditRepository.deleteAllInBatch();
                evidenceVerificationRepository.deleteAllInBatch();
                hsMasterRepository.deleteAllInBatch();
                log.info("Sample records purged successfully.");
            } catch (Exception e) {
                log.warn("Sample purge exception (ignoring if tables clean): {}", e.getMessage());
            }
        }
    }

    public void checkAndSeedDatabase() {
        log.info("Checking database seed status for MySQL TradeData...");
        long count = hsMasterRepository.count();
        log.info("Current hs_master count: {}", count);
        if (count < 100) {
            log.info("hs_master is empty or has minimal rows. Ingesting full 11-country real database dataset...");
            seedAllCountriesInDatabase();
        } else {
            log.info("hs_master already populated with {} records.", count);
        }
    }

    public void seedAllCountriesInDatabase() {
        log.info("==========================================================================================");
        log.info("STARTING REAL DATASET INGESTION INTO MYSQL TRADEDATA DATABASE FOR ALL 11 DESTINATION MARKETS");
        log.info("==========================================================================================");

        // 1. Process United States HTS 2026 CSV file
        try {
            if (new File("hts_2026_basic_edition_csv.csv").exists()) {
                log.info("Ingesting US HTS 2026 dataset from hts_2026_basic_edition_csv.csv...");
                usHtsCsvPipelineProcessorService.processUsHtsCsv("hts_2026_basic_edition_csv.csv", "2026.1");
            }
        } catch (Exception e) {
            log.warn("US HTS file processing fallback: {}", e.getMessage());
        }

        // 2. Process South Korea Customs HSK Excel files
        try {
            if (new File("관세청_HS부호_20260101.xlsx").exists()) {
                log.info("Ingesting South Korea HSK dataset from 관세청_HS부호_20260101.xlsx...");
                southKoreaPipelineProcessorService.processSouthKoreaPipeline("관세청_HS부호_20260101.xlsx", "관세청_품목번호별 관세율표_20260211.xlsx", "2026.1");
            }
        } catch (Exception e) {
            log.warn("South Korea Excel file processing fallback: {}", e.getMessage());
        }

        // 3. Process Canada CBSA Tariff PDF file
        try {
            if (new File("01-99-2026-eng.pdf").exists()) {
                log.info("Ingesting Canada CBSA Tariff dataset from 01-99-2026-eng.pdf...");
                canadaPipelineProcessorService.processCanadaPipeline("01-99-2026-eng.pdf", "2026.1");
            }
        } catch (Exception e) {
            log.warn("Canada PDF processing fallback: {}", e.getMessage());
        }

        // 4. Process Hong Kong HKHS CSV file
        try {
            if (new File("B2XX00232026XXXXB0100 (1).csv").exists()) {
                log.info("Ingesting Hong Kong HKHS dataset from B2XX00232026XXXXB0100 (1).csv...");
                hongKongPipelineProcessorService.processHongKongPipeline("B2XX00232026XXXXB0100 (1).csv", "2026.1");
            }
        } catch (Exception e) {
            log.warn("Hong Kong CSV processing fallback: {}", e.getMessage());
        }

        // 5. Process UAE GCC Tariff Excel file
        try {
            if (new File("HSCodeMaster-v3.3customers.xlsx").exists()) {
                log.info("Ingesting UAE GCC Tariff dataset from HSCodeMaster-v3.3customers.xlsx...");
                uaeGccPipelineProcessorService.processUaePipeline("HSCodeMaster-v3.3customers.xlsx", "2026.1");
            }
        } catch (Exception e) {
            log.warn("UAE GCC Excel processing fallback: {}", e.getMessage());
        }

        // 6. Process EU CN 2026 RDF file for Germany & Netherlands
        try {
            if (new File("ESTAT-CN2026.rdf").exists()) {
                log.info("Ingesting EU CN 2026 dataset from ESTAT-CN2026.rdf for Germany & Netherlands...");
                euCnRdfPipelineProcessorService.processEuCnRdfPipeline("Germany", "ESTAT-CN2026.rdf", "2026.1");
                euCnRdfPipelineProcessorService.processEuCnRdfPipeline("Netherlands", "ESTAT-CN2026.rdf", "2026.1");
            }
        } catch (Exception e) {
            log.warn("EU CN RDF processing fallback: {}", e.getMessage());
        }

        // 7. Process India ITC(HS) 2022 PDF file
        try {
            if (new File("ITC-HS_2022.pdf").exists()) {
                log.info("Ingesting India ITC-HS 2022 dataset from ITC-HS_2022.pdf...");
                indiaItcHsPdfExtractorService.extractItcHsPdf("ITC-HS_2022.pdf");
                indiaEtlPipelineProcessorService.processIndiaPipeline();
            }
        } catch (Exception e) {
            log.warn("India ITC-HS PDF processing fallback: {}", e.getMessage());
        }

        // 7. Execute Regulation Pipeline, Coverage Matcher, and Substantive Evidence Verification across all 11 markets
        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            try {
                log.info("Processing regulations & building coverage audit for country [{}]...", country);
                regulationPipelineService.processRegulationsForCountry(country);
                coverageService.buildCoverageForCountry(country);
                evidenceVerificationService.auditCountryEvidence(country);
            } catch (Exception e) {
                log.warn("Regulation processing for {}: {}", country, e.getMessage());
            }
        }

        log.info("Real dataset ingestion into MySQL TradeData complete. Total hs_master records: {}", hsMasterRepository.count());
    }
}
