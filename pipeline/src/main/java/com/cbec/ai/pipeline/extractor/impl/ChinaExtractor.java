package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class ChinaExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("China") || country.equalsIgnoreCase("CN"));
    }

    @Override
    public String getCountryCode() {
        return "CN";
    }
}
