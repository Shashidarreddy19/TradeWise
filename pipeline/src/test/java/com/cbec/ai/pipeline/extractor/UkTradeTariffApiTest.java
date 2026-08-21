package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.extractor.impl.UkTradeTariffApiAdapter;
import com.cbec.ai.pipeline.model.dto.RawHsRecordDto;
import com.cbec.ai.pipeline.model.dto.SourceMetadataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UkTradeTariffApiTest {

    private UkTradeTariffApiAdapter ukAdapter;

    @BeforeEach
    void setUp() {
        ukAdapter = new UkTradeTariffApiAdapter(new ObjectMapper());
    }

    @Test
    void testSupportsUkCountryNameAndCode() {
        assertTrue(ukAdapter.supportsCountry("United Kingdom"));
        assertTrue(ukAdapter.supportsCountry("UK"));
        assertTrue(ukAdapter.supportsCountry("GB"));
        assertFalse(ukAdapter.supportsCountry("India"));
        assertEquals("GB", ukAdapter.getCountryCode());
    }
}
