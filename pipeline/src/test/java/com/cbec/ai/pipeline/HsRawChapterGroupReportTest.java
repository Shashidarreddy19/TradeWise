package com.cbec.ai.pipeline;

import com.cbec.ai.pipeline.service.IndiaItcHsPdfExtractorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class HsRawChapterGroupReportTest {

    @Autowired
    private IndiaItcHsPdfExtractorService pdfExtractorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void runChapterBreakdownReport() {
        // Ensure PDF extraction has populated hs_raw
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hs_raw WHERE dataset_version = 'ITC_HS_2022'", Long.class);
        if (count == null || count == 0) {
            System.out.println("Extracting ITC-HS_2022.pdf into hs_raw...");
            pdfExtractorService.extractItcHsPdf("ITC-HS_2022.pdf");
        }

        String sql = "SELECT SUBSTRING(raw_national_code, 1, 2) AS chapter, COUNT(*) AS total " +
                     "FROM hs_raw " +
                     "WHERE dataset_version = 'ITC_HS_2022' " +
                     "GROUP BY SUBSTRING(raw_national_code, 1, 2) " +
                     "ORDER BY chapter";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        System.out.println("================================================================================");
        System.out.println("ITC(HS) 2022 PDF EXTRACTION REPORT - CHAPTER BREAKDOWN IN hs_raw");
        System.out.println("================================================================================");
        System.out.printf("%-10s | %-15s%n", "CHAPTER", "TOTAL RECORDS");
        System.out.println("----------------------------------");

        long grandTotal = 0;
        for (Map<String, Object> row : rows) {
            String chapter = (String) row.get("chapter");
            Long total = ((Number) row.get("total")).longValue();
            grandTotal += total;
            System.out.printf("%-10s | %-15d%n", chapter, total);
        }

        System.out.println("----------------------------------");
        System.out.printf("%-10s | %-15d%n", "GRAND TOTAL", grandTotal);
        System.out.println("================================================================================");
    }
}
