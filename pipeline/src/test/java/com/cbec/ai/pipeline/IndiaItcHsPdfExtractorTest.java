package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.IndiaItcHsPdfExtractorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IndiaItcHsPdfExtractorTest {

    @Autowired
    private IndiaItcHsPdfExtractorService extractorService;

    @Test
    void testExtractItcHsPdf() {
        IndiaItcHsPdfExtractorService.PdfExtractionSummary summary = extractorService.extractItcHsPdf("ITC-HS_2022.pdf");
        assertNotNull(summary);
        assertEquals("COMPLETED", summary.getStatus());
        assertTrue(summary.getTotalPagesProcessed() > 0);
        assertTrue(summary.getTotalRowsExtracted() > 0);
        assertTrue(summary.getTotalRowsInserted() > 0);
        System.out.println("Extracted " + summary.getTotalRowsExtracted() + " 8-digit ITC(HS) tariff rows into hs_raw table");
    }
}
