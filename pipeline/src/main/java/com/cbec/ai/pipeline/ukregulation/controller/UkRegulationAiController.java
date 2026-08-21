package com.cbec.ai.pipeline.ukregulation.controller;

import com.cbec.ai.pipeline.ukregulation.service.UkRegulationAiPipelineService;
import com.cbec.ai.pipeline.ukregulation.service.UkRegulationAiProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline")
public class UkRegulationAiController {

    private final UkRegulationAiPipelineService aiPipelineService;

    public UkRegulationAiController(UkRegulationAiPipelineService aiPipelineService) {
        this.aiPipelineService = aiPipelineService;
    }

    /**
     * REST Endpoint for Phase 2: AI Regulation Intelligence Structuring.
     */
    @PostMapping("/process-uk-regulations-ai")
    public ResponseEntity<UkRegulationAiProcessorService.AiProcessingMetrics> processUkRegulationsAi() {
        return ResponseEntity.ok(aiPipelineService.processUkRegulationsAiPipeline());
    }
}
