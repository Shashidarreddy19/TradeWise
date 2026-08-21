package com.cbec.ai.pipeline.intelligence.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class ComplianceComplexityService {

    private final RegulationMasterRepository masterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;
    private final ComplianceComplexityRepository complexityRepository;
    private final ObjectMapper objectMapper;

    public ComplianceComplexityService(
            RegulationMasterRepository masterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository,
            ComplianceComplexityRepository complexityRepository,
            ObjectMapper objectMapper) {
        this.masterRepository = masterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
        this.complexityRepository = complexityRepository;
        this.objectMapper = objectMapper;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexityReportDto {
        private String country;
        private String hsCode;
        private int score;
        private String level;
        private List<String> reasons;
    }

    @Transactional
    public ComplexityReportDto calculateAndSaveComplexity(String country, String hsCode) {
        log.info("Calculating compliance complexity index for country: '{}', HS Code: '{}'", country, hsCode);

        List<RegulationMasterEntity> masters = masterRepository.findByCountry(country);
        List<String> reasons = new ArrayList<>();

        int docCount = 0;
        int certCount = 0;
        int labelCount = 0;
        int restrCount = 0;
        int procCount = 0;

        for (RegulationMasterEntity master : masters) {
            Long masterId = master.getId();
            docCount += documentRepository.findByRegulationId(masterId).size();
            certCount += certificationRepository.findByRegulationId(masterId).size();
            labelCount += labelingRepository.findByRegulationId(masterId).size();
            restrCount += restrictionRepository.findByRegulationId(masterId).size();
            procCount += procedureRepository.findByRegulationId(masterId).size();
        }

        // Base fallback metrics if master entries were sparse
        if (docCount == 0) docCount = 2;
        if (certCount == 0) certCount = 1;
        if (labelCount == 0) labelCount = 1;
        if (restrCount == 0) restrCount = 1;
        if (procCount == 0) procCount = 1;

        int score = Math.min(100, (docCount * 8) + (certCount * 12) + (restrCount * 15) + (labelCount * 8) + (procCount * 7));
        String level;
        if (score <= 35) {
            level = "Low";
        } else if (score <= 65) {
            level = "Medium";
        } else if (score <= 85) {
            level = "High";
        } else {
            level = "Extreme";
        }

        reasons.add(docCount + " mandatory import/export documents required");
        reasons.add(certCount + " mandatory compliance certificates needed");
        reasons.add(restrCount + " active import restriction controls");
        reasons.add(labelCount + " specific product labeling & marking rules");
        reasons.add(procCount + " official customs inspection and declaration procedures");

        try {
            String reasonsJson = objectMapper.writeValueAsString(reasons);
            Optional<ComplianceComplexityEntity> existing = complexityRepository.findByCountryAndHsCode(country, hsCode);
            ComplianceComplexityEntity entity;

            if (existing.isPresent()) {
                entity = existing.get();
                entity.setScore(score);
                entity.setLevel(level);
                entity.setReasons(reasonsJson);
            } else {
                entity = ComplianceComplexityEntity.builder()
                        .country(country)
                        .hsCode(hsCode)
                        .score(score)
                        .level(level)
                        .reasons(reasonsJson)
                        .build();
            }

            complexityRepository.save(entity);
            log.info("Saved compliance complexity score {} ({}) for {}, HS {}", score, level, country, hsCode);

        } catch (Exception e) {
            log.error("Error serializing complexity reasons for {}", country, e);
        }

        return ComplexityReportDto.builder()
                .country(country)
                .hsCode(hsCode)
                .score(score)
                .level(level)
                .reasons(reasons)
                .build();
    }

    public ComplexityReportDto getComplexity(String country, String hsCode) {
        Optional<ComplianceComplexityEntity> existing = complexityRepository.findByCountryAndHsCode(country, hsCode);
        if (existing.isPresent()) {
            ComplianceComplexityEntity entity = existing.get();
            List<String> reasonsList = new ArrayList<>();
            try {
                if (entity.getReasons() != null) {
                    reasonsList = objectMapper.readValue(entity.getReasons(), new TypeReference<List<String>>() {});
                }
            } catch (Exception ignored) {}

            return ComplexityReportDto.builder()
                    .country(entity.getCountry())
                    .hsCode(entity.getHsCode())
                    .score(entity.getScore())
                    .level(entity.getLevel())
                    .reasons(reasonsList)
                    .build();
        }

        return calculateAndSaveComplexity(country, hsCode);
    }
}
