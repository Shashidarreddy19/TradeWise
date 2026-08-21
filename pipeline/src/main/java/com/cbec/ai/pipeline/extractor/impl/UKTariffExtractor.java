package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class UKTariffExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("United Kingdom") || country.equalsIgnoreCase("UK") || country.equalsIgnoreCase("GB"));
    }

    @Override
    public String getCountryCode() {
        return "GB";
    }
}
