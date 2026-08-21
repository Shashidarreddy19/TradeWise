package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.entity.HsRawEntity;
import com.cbec.ai.pipeline.parser.HsParser;
import com.cbec.ai.pipeline.parser.HsParserFactory;
import com.cbec.ai.pipeline.repository.HsRawRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractHsExtractor implements HsExtractor {

    @Autowired
    protected HsParserFactory parserFactory;

    @Autowired
    protected HsRawRepository hsRawRepository;

    @Override
    public List<RawHsRecordDto> extract(InputStream input, SourceMetadataDto metadata, Long executionId) {
        log.info("Executing extractor [{}] for country: {} (Territory: {})",
                getClass().getSimpleName(), metadata.getCountry(), metadata.getCustomsTerritory());

        HsParser parser = parserFactory.getParser(metadata.getFileType());
        List<RawHsRecordDto> rawRecords = parser.parse(input, metadata);

        // Store into Raw Layer (hs_raw) to ensure zero data loss BEFORE any chapter filtering
        if (executionId != null && !rawRecords.isEmpty()) {
            List<HsRawEntity> rawEntities = new ArrayList<>();
            for (RawHsRecordDto dto : rawRecords) {
                rawEntities.add(HsRawEntity.builder()
                        .executionId(executionId)
                        .customsTerritory(metadata.getCustomsTerritory() != null ? metadata.getCustomsTerritory() : "GLOBAL")
                        .country(dto.getCountry() != null ? dto.getCountry() : metadata.getCountry())
                        .rawNationalCode(dto.getRawHsCode())
                        .rawDescription(dto.getRawDescription())
                        .unit(dto.getUnit())
                        .sourceName(dto.getSource() != null ? dto.getSource() : metadata.getSourceName())
                        .datasetVersion(dto.getVersion() != null ? dto.getVersion() : metadata.getVersion())
                        .build());
            }
            hsRawRepository.saveAll(rawEntities);
            log.info("Saved {} unedited raw audit records into hs_raw for execution ID {}", rawEntities.size(), executionId);
        }

        return rawRecords;
    }
}
