package com.cbec.ai.pipeline.ukregulation.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class UkRegulationAiConfiguration {

    @Value("${nvidia.api.key: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}")
    private String nvidiaApiKey;

    @Value("${nvidia.base.url:https://integrate.api.nvidia.com/v1}")
    private String nvidiaBaseUrl;

    @Value("${nvidia.model:nvidia/nemotron-3-ultra-550b-a55b}")
    private String nvidiaModel;

    @Value("${ai.batch-size:10}")
    private int batchSize;

    @Value("${ai.temperature:0.2}")
    private double temperature;

    @Value("${ai.maxTokens:16384}")
    private int maxTokens;
}
