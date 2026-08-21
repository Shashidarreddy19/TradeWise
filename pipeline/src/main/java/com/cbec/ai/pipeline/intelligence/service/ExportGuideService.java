package com.cbec.ai.pipeline.intelligence.service;

import com.cbec.ai.pipeline.model.entity.*;
import com.cbec.ai.pipeline.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ExportGuideService {

    private final HsMasterRepository hsMasterRepository;
    private final RegulationMasterRepository regulationMasterRepository;
    private final RegulationDocumentRepository documentRepository;
    private final RegulationCertificationRepository certificationRepository;
    private final RegulationLabelingRepository labelingRepository;
    private final RegulationRestrictionRepository restrictionRepository;
    private final RegulationProcedureRepository procedureRepository;

    public ExportGuideService(
            HsMasterRepository hsMasterRepository,
            RegulationMasterRepository regulationMasterRepository,
            RegulationDocumentRepository documentRepository,
            RegulationCertificationRepository certificationRepository,
            RegulationLabelingRepository labelingRepository,
            RegulationRestrictionRepository restrictionRepository,
            RegulationProcedureRepository procedureRepository) {
        this.hsMasterRepository = hsMasterRepository;
        this.regulationMasterRepository = regulationMasterRepository;
        this.documentRepository = documentRepository;
        this.certificationRepository = certificationRepository;
        this.labelingRepository = labelingRepository;
        this.restrictionRepository = restrictionRepository;
        this.procedureRepository = procedureRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportGuideDto {
        private String country;
        private String hsCode;
        private String description;
        private Map<String, String> tariff;
        private List<String> documents;
        private List<String> certifications;
        private List<String> restrictions;
        private List<String> labeling;
        private List<String> procedures;
        private List<Map<String, String>> regulations;
    }

    public ExportGuideDto getExportGuide(String country, String hsCode) {
        log.info("Generating Unified Export Guide for Country: '{}', HS Code: '{}'", country, hsCode);

        String description = "Harmonized Tariff Commodity (" + hsCode + ")";
        Map<String, String> tariffMap = new LinkedHashMap<>();
        tariffMap.put("duty", "5.0%");
        tariffMap.put("vat", "20.0%");

        // Interrogate hs_master database records
        Optional<HsMasterEntity> masterOpt = hsMasterRepository.findByCustomsTerritoryAndNationalCode(country, hsCode);
        if (masterOpt.isPresent()) {
            HsMasterEntity hs = masterOpt.get();
            if (hs.getOfficialDescription() != null) description = hs.getOfficialDescription();
            if (hs.getCategory() != null) tariffMap.put("category", hs.getCategory());
        }

        List<RegulationMasterEntity> regMasters = regulationMasterRepository.findByCountry(country);
        Set<String> docs = new LinkedHashSet<>();
        Set<String> certs = new LinkedHashSet<>();
        Set<String> restr = new LinkedHashSet<>();
        Set<String> labels = new LinkedHashSet<>();
        Set<String> procs = new LinkedHashSet<>();
        List<Map<String, String>> regSummaryList = new ArrayList<>();

        for (RegulationMasterEntity reg : regMasters) {
            Long regId = reg.getId();

            documentRepository.findByRegulationId(regId)
                    .forEach(d -> docs.add(d.getDocumentName()));

            certificationRepository.findByRegulationId(regId)
                    .forEach(c -> certs.add(c.getCertificationName()));

            restrictionRepository.findByRegulationId(regId)
                    .forEach(r -> restr.add(r.getDescription()));

            labelingRepository.findByRegulationId(regId)
                    .forEach(l -> labels.add(l.getRequirement()));

            procedureRepository.findByRegulationId(regId)
                    .forEach(p -> procs.add(p.getProcedureName() + ": " + p.getDescription()));

            regSummaryList.add(Map.of(
                    "title", reg.getTitle() != null ? reg.getTitle() : "Trade Regulation",
                    "authority", reg.getAuthority() != null ? reg.getAuthority() : "Customs Authority"
            ));
        }

        // Fallback default compliance lists if empty
        if (docs.isEmpty()) {
            docs.add("Commercial Invoice");
            docs.add("Packing List");
        }
        if (certs.isEmpty()) {
            certs.add("Certificate of Origin");
        }
        if (restr.isEmpty()) {
            restr.add("Standard Import Licensing Rules");
        }
        if (labels.isEmpty()) {
            labels.add("Country of Origin Labeling");
        }
        if (procs.isEmpty()) {
            procs.add("Submit Electronic Customs Import Declaration");
        }
        if (regSummaryList.isEmpty()) {
            regSummaryList.add(Map.of("title", country + " General Import Regulations", "authority", country + " Customs"));
        }

        return ExportGuideDto.builder()
                .country(country)
                .hsCode(hsCode)
                .description(description)
                .tariff(tariffMap)
                .documents(new ArrayList<>(docs))
                .certifications(new ArrayList<>(certs))
                .restrictions(new ArrayList<>(restr))
                .labeling(new ArrayList<>(labels))
                .procedures(new ArrayList<>(procs))
                .regulations(regSummaryList)
                .build();
    }
}
