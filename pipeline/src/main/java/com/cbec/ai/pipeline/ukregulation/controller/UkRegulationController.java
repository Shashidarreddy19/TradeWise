package com.cbec.ai.pipeline.ukregulation.controller;

import com.cbec.ai.pipeline.ukregulation.service.UkRegulationPipelineProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline")
public class UkRegulationController {

    private final UkRegulationPipelineProcessorService pipelineProcessorService;

    public UkRegulationController(UkRegulationPipelineProcessorService pipelineProcessorService) {
        this.pipelineProcessorService = pipelineProcessorService;
    }

    /**
     * Complete Phase 1 UK Regulation Intelligence ETL Pipeline Endpoint.
     */
    @PostMapping("/process-uk-regulations")
    public ResponseEntity<UkRegulationPipelineProcessorService.UkRegulationExecutionReport> processUkRegulations() {
        return ResponseEntity.ok(pipelineProcessorService.processUkRegulations());
    }
}
