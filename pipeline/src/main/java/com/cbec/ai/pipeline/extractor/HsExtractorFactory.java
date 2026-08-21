package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.exception.HsPipelineException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HsExtractorFactory {

    private final List<HsExtractor> extractors;

    public HsExtractorFactory(List<HsExtractor> extractors) {
        this.extractors = extractors;
    }

    /**
     * Resolves matching country extractor using Strategy Pattern.
     */
    public HsExtractor getExtractor(String country) {
        return extractors.stream()
                .filter(e -> e.supportsCountry(country))
                .findFirst()
                .orElseThrow(() -> new HsPipelineException("No dedicated extractor implementation found for country: " + country));
    }
}
