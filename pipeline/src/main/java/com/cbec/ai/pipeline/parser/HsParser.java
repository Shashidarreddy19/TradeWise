package com.cbec.ai.pipeline.parser;

import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.cbec.ai.pipeline.model.enums.FileTypeEnum;

import java.io.InputStream;
import java.util.List;

/**
 * Common interface for all official HS Data Parsers.
 */
public interface HsParser {
    /**
     * Checks whether this parser supports the given file type.
     */
    boolean supports(FileTypeEnum fileType);

    /**
     * Extracts raw HS records from an InputStream.
     * @param inputStream Source data stream
     * @param metadata Source metadata (country, version, source URL)
     * @return List of extracted raw records
     */
    List<RawHsRecordDto> parse(InputStream inputStream, SourceMetadataDto metadata);
}
