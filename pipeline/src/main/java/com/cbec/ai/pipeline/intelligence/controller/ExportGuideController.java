package com.cbec.ai.pipeline.intelligence.controller;

import com.cbec.ai.pipeline.intelligence.service.ExportGuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ExportGuideController {

    private final ExportGuideService exportGuideService;

    public ExportGuideController(ExportGuideService exportGuideService) {
        this.exportGuideService = exportGuideService;
    }

    /**
     * Unified Export Guide API
     * GET /api/v1/export-guide?country=United Kingdom&hsCode=09011100
     */
    @GetMapping("/export-guide")
    public ResponseEntity<ExportGuideService.ExportGuideDto> getExportGuide(
            @RequestParam(name = "country", defaultValue = "United Kingdom") String country,
            @RequestParam(name = "hsCode", defaultValue = "09011100") String hsCode) {
        return ResponseEntity.ok(exportGuideService.getExportGuide(country, hsCode));
    }
}
