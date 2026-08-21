package com.cbec.ai.pipeline.stage;

import com.cbec.ai.pipeline.model.dto.ExtractionReportDto;
import com.cbec.ai.pipeline.model.dto.NormalizedHsRecordDto;
import com.cbec.ai.pipeline.model.entity.RejectedRecordEntity;
import com.cbec.ai.pipeline.repository.RejectedRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class Stage4ValidationService {

    private final RejectedRecordRepository rejectedRecordRepository;

    public Stage4ValidationService(RejectedRecordRepository rejectedRecordRepository) {
        this.rejectedRecordRepository = rejectedRecordRepository;
    }

    /**
     * Stage 4: Validation.
     * Enforces mathematical hierarchy chain integrity:
     * 1. Chapter exists (length == 2)
     * 2. Heading starts with Chapter (heading.startsWith(chapter))
     * 3. HS6 starts with Heading (hs6.startsWith(heading))
     * 4. National Code starts with HS6 (nationalCode.startsWith(hs6))
     * 5. Non-empty official description & duplicate check.
     */
    public List<NormalizedHsRecordDto> validate(List<NormalizedHsRecordDto> records, ExtractionReportDto report, Long executionId) {
        log.info("Stage 4 [Validation] - Validating mathematical hierarchy chain for {} normalized records", records.size());
        List<NormalizedHsRecordDto> validRecords = new ArrayList<>();
        List<RejectedRecordEntity> rejectionsToSave = new ArrayList<>();
        Set<String> seenNationalCodes = new HashSet<>();

        for (NormalizedHsRecordDto record : records) {
            String nationalCode = record.getNationalCode();
            String chapter = record.getChapter();
            String heading = record.getHeading();
            String hs6 = record.getHs6();
            String desc = record.getOfficialDescription();

            // 1. Numeric and Minimum Code Length Check (>= 6 digits)
            if (nationalCode == null || nationalCode.length() < 6 || !nationalCode.matches("\\d+")) {
                String reason = "INVALID_NATIONAL_CODE: Code must be numeric and >= 6 digits: '" + nationalCode + "'";
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalInvalid(report.getTotalInvalid() + 1);
                continue;
            }

            // 2. Chapter Prefix Validation (length == 2 and starts national code)
            if (chapter == null || chapter.length() != 2 || !nationalCode.startsWith(chapter)) {
                String reason = "HIERARCHY_CHAPTER_FAIL: National code '" + nationalCode + "' does not start with chapter '" + chapter + "'";
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalInvalid(report.getTotalInvalid() + 1);
                continue;
            }

            // 3. Heading Prefix Validation (heading.startsWith(chapter))
            if (heading == null || heading.length() != 4 || !heading.startsWith(chapter) || !nationalCode.startsWith(heading)) {
                String reason = "HIERARCHY_HEADING_FAIL: Heading '" + heading + "' does not start with chapter '" + chapter + "' or national code";
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalInvalid(report.getTotalInvalid() + 1);
                continue;
            }

            // 4. HS6 Prefix Validation (hs6.startsWith(heading) & nationalCode.startsWith(hs6))
            if (hs6 == null || hs6.length() != 6 || !hs6.startsWith(heading) || !nationalCode.startsWith(hs6)) {
                String reason = "HIERARCHY_HS6_FAIL: HS6 '" + hs6 + "' does not start with heading '" + heading + "' or national code";
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalInvalid(report.getTotalInvalid() + 1);
                continue;
            }

            // 5. Mandatory Official Description Check
            if (desc == null || desc.trim().isEmpty()) {
                String reason = "MISSING_DESCRIPTION: Official description is empty for code: " + nationalCode;
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalInvalid(report.getTotalInvalid() + 1);
                continue;
            }

            // 6. Batch Deduplication Check per Territory
            String uniqueKey = record.getCustomsTerritory() + ":" + nationalCode;
            if (seenNationalCodes.contains(uniqueKey)) {
                String reason = "DUPLICATE_ROW: Duplicate national code skipped in batch: " + nationalCode;
                logAndPersistRejection(report, rejectionsToSave, executionId, record, reason, "Stage 4 Validation");
                report.setTotalDuplicates(report.getTotalDuplicates() + 1);
                continue;
            }

            seenNationalCodes.add(uniqueKey);
            validRecords.add(record);
        }

        if (!rejectionsToSave.isEmpty()) {
            rejectedRecordRepository.saveAll(rejectionsToSave);
            log.info("Persisted {} rejected audit records into rejected_records table", rejectionsToSave.size());
        }

        log.info("Stage 4 [Validation] - Mathematical hierarchy validation finished. Valid: {}, Invalid: {}, Duplicates: {}",
                validRecords.size(), report.getTotalInvalid(), report.getTotalDuplicates());
        return validRecords;
    }

    private void logAndPersistRejection(
            ExtractionReportDto report,
            List<RejectedRecordEntity> rejectionsToSave,
            Long executionId,
            NormalizedHsRecordDto record,
            String reason,
            String stage) {

        log.warn("Validation Rejection - Reason: {} | Territory: {} | Country: {} | Code: {}",
                reason, record.getCustomsTerritory(), record.getCountry(), record.getNationalCode());
        report.getErrorLogs().add("[" + stage + "] " + reason);

        rejectionsToSave.add(RejectedRecordEntity.builder()
                .executionId(executionId)
                .rawRecord("National Code: " + record.getNationalCode() + " | Desc: " + record.getOfficialDescription())
                .reason(reason)
                .pipelineStage(stage)
                .country(record.getCountry() != null ? record.getCountry() : "UNKNOWN")
                .source(record.getSource() != null ? record.getSource() : "OFFICIAL_SOURCE")
                .build());
    }
}
