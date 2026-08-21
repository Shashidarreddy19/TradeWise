package com.cbec.ai.pipeline.ukregulation.service;

import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import com.cbec.ai.pipeline.repository.RegulationSourceRepository;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UkRegulationPipelineProcessorService {

    private final RegulationSourceRepository sourceRepository;
    private final UkRegulationDownloaderService downloaderService;
    private final UkRegulationExtractorService extractorService;

    public UkRegulationPipelineProcessorService(
            RegulationSourceRepository sourceRepository,
            UkRegulationDownloaderService downloaderService,
            UkRegulationExtractorService extractorService) {
        this.sourceRepository = sourceRepository;
        this.downloaderService = downloaderService;
        this.extractorService = extractorService;
    }

    @Data
    @Builder
    public static class UkRegulationExecutionReport {
        private String country;
        private int sources;
        private int downloaded;
        private int htmlExtracted;
        private int pdfExtracted;
        private long recordsInserted;
        private String status;
    }

    /**
     * Executes Phase 1 UK Regulation ETL Pipeline: Source Seeding -> Downloading -> Archiving -> Text Extraction -> Raw Audit Population.
     */
    @Transactional
    public UkRegulationExecutionReport processUkRegulations() {
        log.info("Starting Phase 1 UK Regulation Intelligence ETL Pipeline...");

        // 1. Automatically seed initial official UK Regulation sources if they do not exist
        seedInitialUkSources();

        // 2. Download all active UK regulation sources
        List<UkRegulationDownloaderService.DownloadResult> downloadResults = downloaderService.downloadActiveUkRegulations();

        int downloadedCount = 0;
        int htmlCount = 0;
        int pdfCount = 0;
        long totalRecordsInserted = 0;

        for (UkRegulationDownloaderService.DownloadResult download : downloadResults) {
            if (download.isSuccess()) {
                downloadedCount++;
                UkRegulationExtractorService.ExtractionResult extraction = extractorService.extractAndSaveRegulationText(download);

                if ("PDF".equalsIgnoreCase(extraction.getFormat())) {
                    pdfCount++;
                } else {
                    htmlCount++;
                }
                totalRecordsInserted += extraction.getRecordsInserted();
            }
        }

        int totalSources = (int) sourceRepository.findByCountry("United Kingdom").size();

        log.info("================================================================================");
        log.info("COMPLETED UK REGULATION INTELLIGENCE ETL PIPELINE");
        log.info("Sources: {}, Downloaded: {}, HTML Extracted: {}, PDF Extracted: {}, Records Inserted: {}",
                totalSources, downloadedCount, htmlCount, pdfCount, totalRecordsInserted);
        log.info("================================================================================");

        return UkRegulationExecutionReport.builder()
                .country("United Kingdom")
                .sources(totalSources)
                .downloaded(downloadedCount)
                .htmlExtracted(htmlCount)
                .pdfExtracted(pdfCount)
                .recordsInserted(totalRecordsInserted)
                .status("SUCCESS")
                .build();
    }

    private void seedInitialUkSources() {
        List<RegulationSourceEntity> initialSources = List.of(
                RegulationSourceEntity.builder()
                        .country("United Kingdom")
                        .authority("HMRC")
                        .title("Trade Tariff API")
                        .documentType("API")
                        .sourceUrl("https://www.trade-tariff.service.gov.uk/api/v2")
                        .format("JSON")
                        .status("ACTIVE")
                        .build(),

                RegulationSourceEntity.builder()
                        .country("United Kingdom")
                        .authority("GOV.UK")
                        .title("Import Controls")
                        .documentType("HTML")
                        .sourceUrl("https://www.gov.uk/guidance/import-controls")
                        .format("HTML")
                        .status("ACTIVE")
                        .build(),

                RegulationSourceEntity.builder()
                        .country("United Kingdom")
                        .authority("GOV.UK")
                        .title("UK Standards and Regulatory Requirements")
                        .documentType("HTML")
                        .sourceUrl("https://www.gov.uk/guidance/uk-standards-and-regulatory-import-requirements")
                        .format("HTML")
                        .status("ACTIVE")
                        .build(),

                RegulationSourceEntity.builder()
                        .country("United Kingdom")
                        .authority("GOV.UK")
                        .title("Import Goods into UK")
                        .documentType("HTML")
                        .sourceUrl("https://www.gov.uk/import-goods-into-uk")
                        .format("HTML")
                        .status("ACTIVE")
                        .build(),

                RegulationSourceEntity.builder()
                        .country("United Kingdom")
                        .authority("GOV.UK")
                        .title("Product Safety Guidance")
                        .documentType("HTML")
                        .sourceUrl("https://www.gov.uk/guidance/product-safety-for-businesses-a-to-z-of-industry-guidance")
                        .format("HTML")
                        .status("ACTIVE")
                        .build()
        );

        for (RegulationSourceEntity src : initialSources) {
            if (sourceRepository.findByCountryAndSourceUrl("United Kingdom", src.getSourceUrl()).isEmpty()) {
                sourceRepository.save(src);
                log.info("Seeded official UK Regulation Source: '{}' ({})", src.getTitle(), src.getSourceUrl());
            }
        }
    }
}
