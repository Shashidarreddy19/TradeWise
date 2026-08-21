package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class HongKongExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("Hong Kong") || country.equalsIgnoreCase("HK"));
    }

    @Override
    public String getCountryCode() {
        return "HK";
    }
}
