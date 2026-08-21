package com.cbec.ai.pipeline.regulation.controller;

import com.cbec.ai.pipeline.regulation.service.HsInventoryAuditService;
import com.cbec.ai.pipeline.regulation.service.HsRegulatoryCoverageService;
import com.cbec.ai.pipeline.regulation.service.GlobalCoverageAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hs-coverage")
public class HsRegulatoryCoverageController {

    private final HsRegulatoryCoverageService coverageService;
    private final HsInventoryAuditService inventoryService;

    public HsRegulatoryCoverageController(
            HsRegulatoryCoverageService coverageService,
            HsInventoryAuditService inventoryService) {
        this.coverageService = coverageService;
        this.inventoryService = inventoryService;
    }

    /**
     * Step 1: HS Inventory per country
     * GET /api/v1/hs-coverage/inventory
     */
    @GetMapping("/inventory")
    public ResponseEntity<HsInventoryAuditService.GlobalHsInventoryReportDto> getGlobalInventory() {
        return ResponseEntity.ok(inventoryService.buildGlobalInventory());
    }

    /**
     * Step 1: HS Inventory for a specific country
     * GET /api/v1/hs-coverage/inventory/{country}
     */
    @GetMapping("/inventory/{country}")
    public ResponseEntity<HsInventoryAuditService.CountryHsInventoryDto> getCountryInventory(
            @PathVariable String country) {
        return ResponseEntity.ok(inventoryService.buildInventoryForCountry(country));
    }

    /**
     * Steps 2–7: Trigger full coverage build for ALL countries
     * POST /api/v1/hs-coverage/build-all
     */
    @PostMapping("/build-all")
    public ResponseEntity<HsRegulatoryCoverageService.GlobalHsCoverageReportDto> buildAllCoverage() {
        return ResponseEntity.ok(coverageService.buildCoverageForAllCountries());
    }

    /**
     * Steps 2–7: Trigger coverage build for a specific country
     * POST /api/v1/hs-coverage/build/{country}
     */
    @PostMapping("/build/{country}")
    public ResponseEntity<HsRegulatoryCoverageService.CountryCoverageReportDto> buildCountryCoverage(
            @PathVariable String country) {
        return ResponseEntity.ok(coverageService.buildCoverageForCountry(country));
    }

    /**
     * Step 8–9: Global coverage report (reads from hs_regulatory_coverage_audit)
     * GET /api/v1/hs-coverage/report
     */
    @GetMapping("/report")
    public ResponseEntity<HsRegulatoryCoverageService.GlobalHsCoverageReportDto> getGlobalReport() {
        HsInventoryAuditService.GlobalHsInventoryReportDto inventory = inventoryService.buildGlobalInventory();
        List<HsRegulatoryCoverageService.CountryCoverageReportDto> reports = new java.util.ArrayList<>();
        long globalTotal = 0;
        long globalMapped = 0;
        for (String country : GlobalCoverageAuditService.ALL_11_COUNTRIES) {
            HsInventoryAuditService.CountryHsInventoryDto inv = inventory.getCountryInventories().get(country);
            long total = inv != null ? inv.getTotalHsCodes() : 0;
            HsRegulatoryCoverageService.CountryCoverageReportDto report = coverageService.buildCountryReport(country, (int) total);
            reports.add(report);
            globalTotal += total;
            globalMapped += report.getMappedHsCodes();
        }
        double globalPct = globalTotal == 0 ? 0.0 : Math.round((globalMapped * 100.0 / globalTotal) * 100.0) / 100.0;
        return ResponseEntity.ok(HsRegulatoryCoverageService.GlobalHsCoverageReportDto.builder()
                .countriesAudited(reports.size())
                .globalTotalHsCodes(globalTotal)
                .globalMappedHsCodes(globalMapped)
                .globalUnmappedHsCodes(globalTotal - globalMapped)
                .globalCoveragePercent(globalPct)
                .countryReports(reports)
                .build());
    }

    /**
     * Step 8: Per-country coverage report
     * GET /api/v1/hs-coverage/report/{country}
     */
    @GetMapping("/report/{country}")
    public ResponseEntity<HsRegulatoryCoverageService.CountryCoverageReportDto> getCountryReport(
            @PathVariable String country) {
        HsInventoryAuditService.CountryHsInventoryDto inv = inventoryService.buildInventoryForCountry(country);
        return ResponseEntity.ok(coverageService.buildCountryReport(country, (int) inv.getTotalHsCodes()));
    }

    /**
     * Step 11: Query compliance profile for a specific country + HS code
     * GET /api/v1/hs-coverage/query?country=United States&hsCode=61091000
     */
    @GetMapping("/query")
    public ResponseEntity<HsRegulatoryCoverageService.HsCodeComplianceProfileDto> queryCompliance(
            @RequestParam String country,
            @RequestParam String hsCode) {
        return ResponseEntity.ok(coverageService.queryCoverageByHsCode(country, hsCode));
    }
}
