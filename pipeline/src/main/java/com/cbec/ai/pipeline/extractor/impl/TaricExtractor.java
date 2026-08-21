package com.cbec.ai.pipeline.extractor.impl;

import com.cbec.ai.pipeline.extractor.AbstractHsExtractor;
import org.springframework.stereotype.Component;

@Component
public class TaricExtractor extends AbstractHsExtractor {

    @Override
    public boolean supportsCountry(String country) {
        return country != null && (country.equalsIgnoreCase("Netherlands") || country.equalsIgnoreCase("Germany") || country.equalsIgnoreCase("EU") || country.equalsIgnoreCase("NL") || country.equalsIgnoreCase("DE"));
    }

    @Override
    public String getCountryCode() {
        return "EU";
    }
}
