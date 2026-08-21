package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class USHTSExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("United States") || country.equalsIgnoreCase("US") || country.equalsIgnoreCase("USA"));
    }

    @Override
    public String getCountryCode() {
        return "US";
    }
}
