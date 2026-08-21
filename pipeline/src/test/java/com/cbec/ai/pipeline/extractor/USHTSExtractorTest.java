package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.extractor.impl.USHTSExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class USHTSExtractorTest {

    private USHTSExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new USHTSExtractor();
    }

    @Test
    void testSupportsUSCountryNameAndCode() {
        assertTrue(extractor.supportsCountry("United States"));
        assertTrue(extractor.supportsCountry("US"));
        assertTrue(extractor.supportsCountry("USA"));
        assertFalse(extractor.supportsCountry("India"));
        assertEquals("US", extractor.getCountryCode());
    }
}
