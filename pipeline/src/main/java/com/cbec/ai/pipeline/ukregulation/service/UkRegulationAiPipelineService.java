package com.cbec.ai.pipeline.ukregulation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UkRegulationAiPipelineService {

    private final UkRegulationAiProcessorService aiProcessorService;

    public UkRegulationAiPipelineService(UkRegulationAiProcessorService aiProcessorService) {
        this.aiProcessorService = aiProcessorService;
    }

    /**
     * Orchestrates Phase 2 AI Regulation Intelligence Structuring Pipeline.
     */
    @Transactional
    public UkRegulationAiProcessorService.AiProcessingMetrics processUkRegulationsAiPipeline() {
        log.info("Orchestrating Phase 2 UK Regulation Intelligence AI Pipeline...");
        return aiProcessorService.processUkRegulationsAi();
    }
}
