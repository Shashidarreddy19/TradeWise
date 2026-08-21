package com.cbec.ai.pipeline.extractor;

import com.cbec.ai.pipeline.extractor.impl.DGFTExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DGFTExtractorTest {

    private DGFTExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DGFTExtractor();
    }

    @Test
    void testSupportsIndiaCountryNameAndCode() {
        assertTrue(extractor.supportsCountry("India"));
        assertTrue(extractor.supportsCountry("IN"));
        assertFalse(extractor.supportsCountry("United States"));
        assertEquals("IN", extractor.getCountryCode());
    }
}
