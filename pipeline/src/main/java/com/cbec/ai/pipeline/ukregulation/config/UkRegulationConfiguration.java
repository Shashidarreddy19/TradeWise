package com.cbec.ai.pipeline.ukregulation.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class UkRegulationConfiguration {

    @Value("${uk.regulation.download-directory:downloads/UnitedKingdom/Regulations}")
    private String downloadDirectory;

    @Value("${uk.regulation.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36}")
    private String userAgent;

    @Value("${uk.regulation.timeout:60000}")
    private int timeout;

    @Value("${uk.regulation.retry-count:3}")
    private int retryCount;
}
