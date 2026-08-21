package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class UAEExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("United Arab Emirates") || country.equalsIgnoreCase("UAE") || country.equalsIgnoreCase("AE"));
    }

    @Override
    public String getCountryCode() {
        return "AE";
    }
}
