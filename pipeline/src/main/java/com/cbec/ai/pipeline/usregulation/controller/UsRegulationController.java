package com.cbec.ai.pipeline.usregulation.controller;

import com.cbec.ai.pipeline.regulation.service.RegulationAiProcessorService.GenericAiProcessingMetrics;
import com.cbec.ai.pipeline.usregulation.service.UsRegulationPipelineProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline")
public class UsRegulationController {

    private final UsRegulationPipelineProcessorService usPipelineService;

    public UsRegulationController(UsRegulationPipelineProcessorService usPipelineService) {
        this.usPipelineService = usPipelineService;
    }

    /**
     * United States Regulation Intelligence Pipeline Endpoint
     * POST /api/v1/pipeline/process-us-regulations
     */
    @PostMapping("/process-us-regulations")
    public ResponseEntity<GenericAiProcessingMetrics> processUsRegulations() {
        return ResponseEntity.ok(usPipelineService.executeUsRegulationPipeline());
    }
}
