package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.extractor.HsExtractor;
import com.cbec.ai.pipeline.extractor.HsExtractorFactory;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.util.DownloadManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
public class Stage2ExtractionService {

    private final HsExtractorFactory extractorFactory;
    private final DownloadManager downloadManager;

    public Stage2ExtractionService(HsExtractorFactory extractorFactory, DownloadManager downloadManager) {
        this.extractorFactory = extractorFactory;
        this.downloadManager = downloadManager;
    }

    /**
     * Stage 2: Extraction.
     * Invokes country Strategy Extractor, archives download file, persists into `hs_raw`.
     */
    public List<RawHsRecordDto> extract(InputStream inputStream, SourceMetadataDto metadata, Long executionId) {
        log.info("Stage 2 [Extraction] - Invoking strategy extractor for country: {}", metadata.getCountry());

        HsExtractor extractor = extractorFactory.getExtractor(metadata.getCountry());
        List<RawHsRecordDto> rawRecords = extractor.extract(inputStream, metadata, executionId);

        log.info("Stage 2 [Extraction] - Extracted {} raw records using extractor [{}]", rawRecords.size(), extractor.getClass().getSimpleName());
        return rawRecords;
    }
}
