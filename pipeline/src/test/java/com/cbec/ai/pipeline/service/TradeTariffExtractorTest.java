package com.cbec.ai.pipeline.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TradeTariffExtractorTest {

    @Autowired
    private TradeTariffDownloaderService downloaderService;

    @Autowired
    private TradeTariffExtractorService extractorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUkTariffJsonExtractionFromLivePayload() {
        TradeTariffDownloaderService.DownloadSummary downloadSummary =
                downloaderService.downloadOfficialUkTariffPayload("UK_TARIFF_2026");

        TradeTariffExtractorService.UkExtractionSummary summary =
                extractorService.extractUkTariffData(downloadSummary.getFilePath(), 99L, "UK_TARIFF_2026");

        assertNotNull(summary);
        assertEquals("United Kingdom", summary.getCountry());
        assertTrue(summary.getTotalExtracted() > 0);

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hs_raw WHERE country = 'United Kingdom' AND execution_id = 99", Long.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }
}
