package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class SaudiExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("Saudi Arabia") || country.equalsIgnoreCase("SA"));
    }

    @Override
    public String getCountryCode() {
        return "SA";
    }
}
