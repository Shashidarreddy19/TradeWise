package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;

import java.io.InputStream;
import java.util.List;

/**
 * Strategy interface for country-specific official HS data extractors.
 */
public interface HsExtractor {

    /**
     * Determines whether this extractor handles the target country.
     */
    boolean supportsCountry(String country);

    /**
     * Returns country code identifier.
     */
    String getCountryCode();

    /**
     * Extracts raw records from input stream.
     */
    List<RawHsRecordDto> extract(InputStream input, SourceMetadataDto metadata, Long executionId);
}
