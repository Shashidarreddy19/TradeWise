package com.cbec.ai.pipeline.controller;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import com.cbec.ai.pipeline.service.DatasetDownloaderService;
import com.cbec.ai.pipeline.service.HsEtlPipelineOrchestrator;
import com.cbec.ai.pipeline.service.IndiaEtlPipelineProcessorService;
import com.cbec.ai.pipeline.service.AustraliaPipelineProcessorService;
import com.cbec.ai.pipeline.service.CanadaPipelineProcessorService;
import com.cbec.ai.pipeline.service.JapanPipelineProcessorService;
import com.cbec.ai.pipeline.service.SouthKoreaPipelineProcessorService;
import com.cbec.ai.pipeline.service.EUTaricEtlProcessorService;
import com.cbec.ai.pipeline.service.EuCnRdfPipelineProcessorService;
import com.cbec.ai.pipeline.service.HongKongPipelineProcessorService;
import com.cbec.ai.pipeline.service.IndiaItcHsPdfExtractorService;
import com.cbec.ai.pipeline.service.UaeGccPipelineProcessorService;
import com.cbec.ai.pipeline.service.UkTradeTariffEtlProcessorService;
import com.cbec.ai.pipeline.service.USHTSCsvPipelineProcessorService;
import com.cbec.ai.pipeline.service.USHTSEtlProcessorService;
import com.cbec.ai.pipeline.util.OfficialSourceFetcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineController {

    private final HsEtlPipelineOrchestrator orchestrator;
    private final OfficialSourceFetcher officialSourceFetcher;
    private final DatasetDownloaderService datasetDownloaderService;
    private final IndiaItcHsPdfExtractorService indiaItcHsPdfExtractorService;
    private final IndiaEtlPipelineProcessorService indiaEtlPipelineProcessorService;
    private final USHTSEtlProcessorService usHtsEtlProcessorService;
    private final USHTSCsvPipelineProcessorService usHtsCsvPipelineProcessorService;
    private final EUTaricEtlProcessorService euTaricEtlProcessorService;
    private final EuCnRdfPipelineProcessorService euCnRdfPipelineProcessorService;
    private final UkTradeTariffEtlProcessorService ukTradeTariffEtlProcessorService;
    private final UaeGccPipelineProcessorService uaeGccPipelineProcessorService;
    private final HongKongPipelineProcessorService hongKongPipelineProcessorService;
    private final AustraliaPipelineProcessorService australiaPipelineProcessorService;
    private final CanadaPipelineProcessorService canadaPipelineProcessorService;
    private final JapanPipelineProcessorService japanPipelineProcessorService;
    private final SouthKoreaPipelineProcessorService southKoreaPipelineProcessorService;

    private final HsMasterRepository hsMasterRepository;
    private final HsRawRepository hsRawRepository;
    private final HsValidatedRepository hsValidatedRepository;
    private final HsVersionRepository hsVersionRepository;
    private final PipelineExecutionRepository executionRepository;
    private final RejectedRecordRepository rejectedRecordRepository;
    private final DownloadHistoryRepository downloadHistoryRepository;
    private final SourceMasterRepository sourceMasterRepository;
    private final CategoryMasterRepository categoryMasterRepository;

    public PipelineController(
            HsEtlPipelineOrchestrator orchestrator,
            OfficialSourceFetcher officialSourceFetcher,
            DatasetDownloaderService datasetDownloaderService,
            IndiaItcHsPdfExtractorService indiaItcHsPdfExtractorService,
            IndiaEtlPipelineProcessorService indiaEtlPipelineProcessorService,
            USHTSEtlProcessorService usHtsEtlProcessorService,
            USHTSCsvPipelineProcessorService usHtsCsvPipelineProcessorService,
            EUTaricEtlProcessorService euTaricEtlProcessorService,
            EuCnRdfPipelineProcessorService euCnRdfPipelineProcessorService,
            UkTradeTariffEtlProcessorService ukTradeTariffEtlProcessorService,
            UaeGccPipelineProcessorService uaeGccPipelineProcessorService,
            HongKongPipelineProcessorService hongKongPipelineProcessorService,
            AustraliaPipelineProcessorService australiaPipelineProcessorService,
            CanadaPipelineProcessorService canadaPipelineProcessorService,
            JapanPipelineProcessorService japanPipelineProcessorService,
            SouthKoreaPipelineProcessorService southKoreaPipelineProcessorService,
            HsMasterRepository hsMasterRepository,
            HsRawRepository hsRawRepository,
            HsValidatedRepository hsValidatedRepository,
            HsVersionRepository hsVersionRepository,
            PipelineExecutionRepository executionRepository,
            RejectedRecordRepository rejectedRecordRepository,
            DownloadHistoryRepository downloadHistoryRepository,
            SourceMasterRepository sourceMasterRepository,
            CategoryMasterRepository categoryMasterRepository) {
        this.orchestrator = orchestrator;
        this.officialSourceFetcher = officialSourceFetcher;
        this.datasetDownloaderService = datasetDownloaderService;
        this.indiaItcHsPdfExtractorService = indiaItcHsPdfExtractorService;
        this.indiaEtlPipelineProcessorService = indiaEtlPipelineProcessorService;
        this.usHtsEtlProcessorService = usHtsEtlProcessorService;
        this.usHtsCsvPipelineProcessorService = usHtsCsvPipelineProcessorService;
        this.euTaricEtlProcessorService = euTaricEtlProcessorService;
        this.euCnRdfPipelineProcessorService = euCnRdfPipelineProcessorService;
        this.ukTradeTariffEtlProcessorService = ukTradeTariffEtlProcessorService;
        this.uaeGccPipelineProcessorService = uaeGccPipelineProcessorService;
        this.hongKongPipelineProcessorService = hongKongPipelineProcessorService;
        this.australiaPipelineProcessorService = australiaPipelineProcessorService;
        this.canadaPipelineProcessorService = canadaPipelineProcessorService;
        this.japanPipelineProcessorService = japanPipelineProcessorService;
        this.southKoreaPipelineProcessorService = southKoreaPipelineProcessorService;
        this.hsMasterRepository = hsMasterRepository;
        this.hsRawRepository = hsRawRepository;
        this.hsValidatedRepository = hsValidatedRepository;
        this.hsVersionRepository = hsVersionRepository;
        this.executionRepository = executionRepository;
        this.rejectedRecordRepository = rejectedRecordRepository;
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.sourceMasterRepository = sourceMasterRepository;
        this.categoryMasterRepository = categoryMasterRepository;
    }

    /**
     * Complete South Korea Customs Tariff Excel ETL Pipeline.
     */
    @PostMapping("/process-south-korea")
    public ResponseEntity<SouthKoreaPipelineProcessorService.SouthKoreaEtlExecutionReport> processSouthKoreaEtl(
            @RequestParam(required = false, defaultValue = "관세청_HS부호_20260101.xlsx") String classificationExcelPath,
            @RequestParam(required = false, defaultValue = "관세청_품목번호별 관세율표_20260211.xlsx") String tariffExcelPath,
            @RequestParam(required = false, defaultValue = "HSK_2026") String version) {
        return ResponseEntity.ok(southKoreaPipelineProcessorService.processSouthKoreaPipeline(classificationExcelPath, tariffExcelPath, version));
    }

    /**
     * Complete Japan Customs Tariff Web Crawling ETL Pipeline.
     */
    @PostMapping("/process-japan")
    public ResponseEntity<JapanPipelineProcessorService.JapanEtlExecutionReport> processJapanEtl(
            @RequestParam(required = false, defaultValue = "JAPAN_TARIFF_2026") String version) {
        return ResponseEntity.ok(japanPipelineProcessorService.processJapanPipeline(version));
    }

    /**
     * Complete Canada Customs Tariff PDF ETL Pipeline (CBSA 2026 PDF).
     */
    @PostMapping("/process-canada")
    public ResponseEntity<CanadaPipelineProcessorService.CanadaEtlExecutionReport> processCanadaEtl(
            @RequestParam(required = false, defaultValue = "01-99-2026-eng.pdf") String pdfPath,
            @RequestParam(required = false, defaultValue = "CANADA_TARIFF_2026") String version) {
        return ResponseEntity.ok(canadaPipelineProcessorService.processCanadaPipeline(pdfPath, version));
    }

    /**
     * Complete Australia Customs Tariff Web Crawling ETL Pipeline (ABF Schedule 3).
     */
    @PostMapping("/process-australia")
    public ResponseEntity<AustraliaPipelineProcessorService.AustraliaEtlExecutionReport> processAustraliaEtl(
            @RequestParam(required = false, defaultValue = "AU_TARIFF_2026") String version) {
        return ResponseEntity.ok(australiaPipelineProcessorService.processAustraliaPipeline(version));
    }

    /**
     * Complete Hong Kong Harmonized System CSV ETL Pipeline.
     */
    @PostMapping("/process-hongkong")
    public ResponseEntity<HongKongPipelineProcessorService.HkEtlExecutionReport> processHongKongEtl(
            @RequestParam(required = false, defaultValue = "B2XX00232026XXXXB0100 (1).csv") String csvPath,
            @RequestParam(required = false, defaultValue = "HKHS_2026") String version) {
        return ResponseEntity.ok(hongKongPipelineProcessorService.processHongKongPipeline(csvPath, version));
    }

    /**
     * Complete United Arab Emirates GCC Customs Tariff Excel ETL Pipeline.
     */
    @PostMapping("/process-uae")
    public ResponseEntity<UaeGccPipelineProcessorService.UaeEtlExecutionReport> processUaeEtl(
            @RequestParam(required = false, defaultValue = "HSCodeMaster-v3.3customers.xlsx") String excelPath,
            @RequestParam(required = false, defaultValue = "GCC_TARIFF_v3.3") String version) {
        return ResponseEntity.ok(uaeGccPipelineProcessorService.processUaePipeline(excelPath, version));
    }

    /**
     * Complete 100% Live United Kingdom Trade Tariff OAuth2 & ETL Pipeline (HMRC API).
     */
    @PostMapping("/process-uk")
    public ResponseEntity<UkTradeTariffEtlProcessorService.UkEtlSummary> processUkEtl(
            @RequestParam(required = false, defaultValue = "UK_TARIFF_2026") String version) {
        return ResponseEntity.ok(ukTradeTariffEtlProcessorService.processUkPipeline(version));
    }

    /**
     * Complete StAX Streaming EU CN 2026 RDF/XML ETL Pipeline (ESTAT-CN2026.rdf 171 MB).
     */
    @PostMapping("/process-eu-rdf")
    public ResponseEntity<EuCnRdfPipelineProcessorService.EuCnRdfSummary> processEuCnRdfEtl(
            @RequestParam(required = false, defaultValue = "Germany") String country,
            @RequestParam(required = false, defaultValue = "ESTAT-CN2026.rdf") String rdfPath,
            @RequestParam(required = false, defaultValue = "CN_2026") String version) {
        return ResponseEntity.ok(euCnRdfPipelineProcessorService.processEuCnRdfPipeline(country, rdfPath, version));
    }

    /**
     * Complete EU Combined Nomenclature & TARIC ETL Pipeline supporting Germany, Netherlands, and all EU member states.
     */
    @PostMapping("/process-eu")
    public ResponseEntity<EUTaricEtlProcessorService.EuEtlExecutionSummary> processEuEtl(
            @RequestParam(required = false, defaultValue = "Germany") String country,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false, defaultValue = "EU_TARIC_2026") String version) {
        return ResponseEntity.ok(euTaricEtlProcessorService.processEuPipeline(country, sourceUrl, version));
    }

    /**
     * Complete Streaming CSV ETL Pipeline for USITC HTS 2026 Basic Edition.
     */
    @PostMapping("/process-usa-csv")
    public ResponseEntity<USHTSCsvPipelineProcessorService.UsCsvEtlSummary> processUsaCsvEtl(
            @RequestParam(required = false, defaultValue = "hts_2026_basic_edition_csv.csv") String csvPath,
            @RequestParam(required = false, defaultValue = "HTS_2026") String version) {
        return ResponseEntity.ok(usHtsCsvPipelineProcessorService.processUsHtsCsv(csvPath, version));
    }

    /**
     * Complete United States HTS ETL Pipeline: Download -> Extract -> Validate -> Category Map -> Master Load
     */
    @PostMapping("/process-usa")
    public ResponseEntity<USHTSEtlProcessorService.UsEtlExecutionSummary> processUsaEtl(
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false, defaultValue = "US_HTS_2026") String version) {
        return ResponseEntity.ok(usHtsEtlProcessorService.processUsPipeline(sourceUrl, version));
    }

    /**
     * Executes the complete India ETL pipeline: hs_raw -> hs_validated -> hs_master.
     */
    @PostMapping("/process-india-etl")
    public ResponseEntity<IndiaEtlPipelineProcessorService.EtlExecutionSummary> processIndiaEtl() {
        return ResponseEntity.ok(indiaEtlPipelineProcessorService.processIndiaPipeline());
    }

    /**
     * Production PDF Extraction for Indian ITC(HS) 2022 PDF into `hs_raw` table ONLY.
     */
    @PostMapping("/extract-pdf")
    public ResponseEntity<IndiaItcHsPdfExtractorService.PdfExtractionSummary> extractItcHsPdf(
            @RequestParam(required = false, defaultValue = "ITC-HS_2022.pdf") String pdfPath) {
        return ResponseEntity.ok(indiaItcHsPdfExtractorService.extractItcHsPdf(pdfPath));
    }

    /**
     * Executes the complete 10-step extraction pipeline for a country.
     */
    @PostMapping("/run-country")
    public ResponseEntity<ExtractionReportDto> runPipelineForCountry(
            @RequestParam String country,
            @RequestParam(required = false) String customUrlOrPath) {

        if (customUrlOrPath != null && !customUrlOrPath.isEmpty()) {
            if (customUrlOrPath.startsWith("http")) {
                try (InputStream is = officialSourceFetcher.fetchFromUrl(customUrlOrPath)) {
                    return ResponseEntity.ok(orchestrator.runPipeline(country, country + " Official Tariff", customUrlOrPath, "2026.1", is));
                } catch (Exception e) {
                    throw new HsPipelineException("Failed to fetch official URL for " + country + ": " + e.getMessage(), e);
                }
            } else {
                Path path = Paths.get(customUrlOrPath);
                if (Files.exists(path)) {
                    try (InputStream is = new FileInputStream(path.toFile())) {
                        return ResponseEntity.ok(orchestrator.runPipeline(country, country + " Official Tariff", customUrlOrPath, "2026.1", is));
                    } catch (Exception e) {
                        throw new HsPipelineException("Failed running pipeline from file: " + e.getMessage(), e);
                    }
                }
            }
        }

        ExtractionReportDto report = orchestrator.runPipelineForCountry(country);
        return ResponseEntity.ok(report);
    }

    /**
     * Checks if a newer dataset version is available on official HTTP server via ETag/Last-Modified.
     */
    @GetMapping("/check-incremental")
    public ResponseEntity<Map<String, Object>> checkIncrementalUpdate(
            @RequestParam String country,
            @RequestParam String sourceUrl) {
        boolean newerAvailable = datasetDownloaderService.isNewerVersionAvailable(sourceUrl, country);
        Map<String, Object> resp = new HashMap<>();
        resp.put("country", country);
        resp.put("sourceUrl", sourceUrl);
        resp.put("newerVersionAvailable", newerAvailable);
        return ResponseEntity.ok(resp);
    }

    /**
     * Downloads and extracts data directly from an official live government URL.
     */
    @PostMapping("/run-url")
    public ResponseEntity<ExtractionReportDto> runPipelineFromOfficialUrl(
            @RequestParam String country,
            @RequestParam String sourceName,
            @RequestParam String sourceUrl,
            @RequestParam(required = false, defaultValue = "2026.1") String version) {

        try (InputStream is = officialSourceFetcher.fetchFromUrl(sourceUrl)) {
            ExtractionReportDto report = orchestrator.runPipeline(country, sourceName, sourceUrl, version, is);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            throw new HsPipelineException("Error fetching and executing pipeline from official URL: " + e.getMessage(), e);
        }
    }

    /**
     * Executes pipeline from a local file path.
     */
    @PostMapping("/run-file")
    public ResponseEntity<ExtractionReportDto> runPipelineFromFile(
            @RequestParam String country,
            @RequestParam String sourceName,
            @RequestParam String filePath,
            @RequestParam(required = false, defaultValue = "2026.1") String version) {

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new HsPipelineException("File not found at specified path: " + filePath);
        }

        try (InputStream is = new FileInputStream(path.toFile())) {
            ExtractionReportDto report = orchestrator.runPipeline(country, sourceName, filePath, version, is);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            throw new HsPipelineException("Error running pipeline from file: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads file for immediate multi-stage pipeline processing.
     */
    @PostMapping("/upload")
    public ResponseEntity<ExtractionReportDto> uploadAndExtract(
            @RequestParam String country,
            @RequestParam String sourceName,
            @RequestParam(required = false, defaultValue = "2026.1") String version,
            @RequestParam("file") MultipartFile file) {

        try (InputStream is = file.getInputStream()) {
            ExtractionReportDto report = orchestrator.runPipeline(country, sourceName, file.getOriginalFilename(), version, is);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            throw new HsPipelineException("Error uploading and processing file: " + e.getMessage(), e);
        }
    }

    /**
     * Queries Active Master Layer records (`hs_master`).
     */
    @GetMapping("/master")
    public ResponseEntity<List<HsMasterEntity>> getMasterRecords(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String customsTerritory,
            @RequestParam(required = false) String category) {
        if (customsTerritory != null && !customsTerritory.isEmpty()) {
            return ResponseEntity.ok(hsMasterRepository.findByCustomsTerritory(customsTerritory));
        } else if (country != null && !country.isEmpty()) {
            return ResponseEntity.ok(hsMasterRepository.findByCountry(country));
        } else if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(hsMasterRepository.findByCategory(category));
        }
        return ResponseEntity.ok(hsMasterRepository.findAll());
    }

    /**
     * Queries Historical Dataset Versions (`hs_versions`).
     */
    @GetMapping("/versions")
    public ResponseEntity<List<HsVersionEntity>> getVersionRecords(
            @RequestParam String customsTerritory,
            @RequestParam String nationalCode) {
        return ResponseEntity.ok(hsVersionRepository.findByCustomsTerritoryAndNationalCode(customsTerritory, nationalCode));
    }

    /**
     * Queries Official Sources Master (`source_master`).
     */
    @GetMapping("/sources")
    public ResponseEntity<List<SourceMasterEntity>> getSources() {
        return ResponseEntity.ok(sourceMasterRepository.findAll());
    }

    /**
     * Queries Normalized Category Master and Child Chapters (`category_master` & `category_chapter`).
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryMasterEntity>> getCategories() {
        return ResponseEntity.ok(categoryMasterRepository.findAll());
    }

    /**
     * Queries Pipeline Execution History (`pipeline_execution`).
     */
    @GetMapping("/executions")
    public ResponseEntity<List<PipelineExecutionEntity>> getExecutions(
            @RequestParam(required = false) String country) {
        if (country != null && !country.isEmpty()) {
            return ResponseEntity.ok(executionRepository.findByCountry(country));
        }
        return ResponseEntity.ok(executionRepository.findAll());
    }

    /**
     * Queries Rejected Records Audit Log (`rejected_records`).
     */
    @GetMapping("/rejections")
    public ResponseEntity<List<RejectedRecordEntity>> getRejections(
            @RequestParam(required = false) Long executionId,
            @RequestParam(required = false) String country) {
        if (executionId != null) {
            return ResponseEntity.ok(rejectedRecordRepository.findByExecutionId(executionId));
        } else if (country != null && !country.isEmpty()) {
            return ResponseEntity.ok(rejectedRecordRepository.findByCountry(country));
        }
        return ResponseEntity.ok(rejectedRecordRepository.findAll());
    }

    /**
     * Queries Download History Audit (`download_history`).
     */
    @GetMapping("/downloads")
    public ResponseEntity<List<DownloadHistoryEntity>> getDownloads(
            @RequestParam(required = false) String country) {
        if (country != null && !country.isEmpty()) {
            return ResponseEntity.ok(downloadHistoryRepository.findByCountry(country));
        }
        return ResponseEntity.ok(downloadHistoryRepository.findAll());
    }

    /**
     * Returns 3-layer database record counts.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hs_raw_count", hsRawRepository.count());
        stats.put("hs_validated_count", hsValidatedRepository.count());
        stats.put("hs_master_count", hsMasterRepository.count());
        stats.put("hs_versions_count", hsVersionRepository.count());
        stats.put("rejections_count", rejectedRecordRepository.count());
        stats.put("executions_count", executionRepository.count());
        return ResponseEntity.ok(stats);
    }
}
