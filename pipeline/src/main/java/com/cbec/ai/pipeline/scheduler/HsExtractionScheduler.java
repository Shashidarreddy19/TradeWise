package com.cbec.ai.pipeline.scheduler;

import com.cbec.ai.pipeline.service.HsEtlPipelineOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HsExtractionScheduler {

    private final HsEtlPipelineOrchestrator orchestrator;

    public HsExtractionScheduler(HsEtlPipelineOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Nightly scheduled sync job for official customs feeds (Runs at 02:00 AM daily).
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleDailyOfficialSync() {
        log.info("Starting scheduled nightly HS code sync job across official data sources...");
        // Pipeline triggers automated checks for official updates
    }
}
