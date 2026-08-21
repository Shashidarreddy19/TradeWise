package com.cbec.ai.pipeline.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution metrics report produced at the conclusion of an extraction job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionReportDto {
    private String jobTitle;
    private String country;
    private String sourceName;
    private long totalRecordsFound;
    private long totalInserted;
    private long totalSkipped;
    private long totalDuplicates;
    private long totalInvalid;
    private long processingTimeMs;
    @Builder.Default
    private List<String> errorLogs = new ArrayList<>();
    @Builder.Default
    private List<String> rejectionSummary = new ArrayList<>();
}
