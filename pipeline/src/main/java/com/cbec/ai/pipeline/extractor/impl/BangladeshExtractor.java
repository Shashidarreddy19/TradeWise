package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class BangladeshExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("Bangladesh") || country.equalsIgnoreCase("BD"));
    }

    @Override
    public String getCountryCode() {
        return "BD";
    }
}
