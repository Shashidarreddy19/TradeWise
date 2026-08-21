package com.trade.regulatory.controller;

import com.trade.regulatory.service.RegulatoryRetrievalService;
import com.trade.regulatory.service.RegulatoryRetrievalService.ComplianceScore;
import com.trade.regulatory.service.RegulatoryRetrievalService.RegulatoryResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Export guidance endpoint.
 * Generates structured export guidance from real regulatory data.
 */
@RestController
@RequestMapping("/api/v1")
public class ExportGuideController {

    private final RegulatoryRetrievalService retrievalService;

    public ExportGuideController(RegulatoryRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    /**
     * GET /api/v1/export-guide/{country}/{hsCode}
     * Returns step-by-step export guidance derived from real regulatory data.
     */
    @GetMapping("/export-guide/{country}/{hsCode}")
    public ResponseEntity<?> getExportGuide(
            @PathVariable String country,
            @PathVariable String hsCode) {

        RegulatoryResult reg = retrievalService.getRegulations(country, hsCode);
        if (!reg.supported) {
            return ResponseEntity.badRequest().body(Map.of("error", reg.error));
        }
        ComplianceScore score = retrievalService.calculateCompliance(reg);

        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("country", reg.country);
        guide.put("hsCode", reg.hsCode);
        guide.put("productDescription", reg.productDescription);
        guide.put("complianceScore", score.numericScore);
        guide.put("complexity", score.complexity);
        guide.put("matchType", reg.matchType);
        guide.put("confidence", reg.confidence);

        // Build steps from real regulatory data
        List<Map<String, Object>> steps = new ArrayList<>();
        int stepNum = 1;

        // Step 1: HS Classification
        steps.add(Map.of(
                "step", stepNum++,
                "title", "Verify HS Classification",
                "description", "Confirm HS code " + hsCode + " with customs broker. Product: " +
                        (reg.productDescription != null ? reg.productDescription : "Check official tariff schedule"),
                "status", "REQUIRED"
        ));

        // Step 2: Required documents
        if (!reg.documents.isEmpty()) {
            List<String> docs = reg.documents.stream()
                    .map(d -> d.getDocumentName() + (Boolean.TRUE.equals(d.getMandatory()) ? " (Mandatory)" : ""))
                    .toList();
            steps.add(Map.of(
                    "step", stepNum++,
                    "title", "Prepare Required Documents (" + docs.size() + ")",
                    "description", "Documents needed: " + String.join(", ", docs),
                    "documents", docs,
                    "status", "REQUIRED"
            ));
        }

        // Step 3: Certifications
        if (!reg.certifications.isEmpty()) {
            List<String> certs = reg.certifications.stream()
                    .map(c -> c.getCertificationName() + (Boolean.TRUE.equals(c.getMandatory()) ? " (Mandatory)" : ""))
                    .toList();
            steps.add(Map.of(
                    "step", stepNum++,
                    "title", "Obtain Required Certifications (" + certs.size() + ")",
                    "description", "Certifications required: " + String.join(", ", certs),
                    "certifications", certs,
                    "status", "REQUIRED"
            ));
        }

        // Step 4: Labeling
        if (!reg.labeling.isEmpty()) {
            List<String> labels = reg.labeling.stream()
                    .map(l -> l.getRequirement())
                    .toList();
            steps.add(Map.of(
                    "step", stepNum++,
                    "title", "Apply Labeling Requirements (" + labels.size() + ")",
                    "description", String.join("; ", labels),
                    "status", "REQUIRED"
            ));
        }

        // Step 5: Check restrictions
        if (!reg.restrictions.isEmpty()) {
            List<String> restricts = reg.restrictions.stream()
                    .map(r -> "[" + r.getRestrictionType() + "] " + r.getDescription())
                    .toList();
            steps.add(Map.of(
                    "step", stepNum++,
                    "title", "Verify Restrictions/Prohibitions (" + restricts.size() + ")",
                    "description", String.join("; ", restricts),
                    "status", "CRITICAL"
            ));
        }

        // Step 6: Customs procedures
        if (!reg.procedures.isEmpty()) {
            List<String> procs = reg.procedures.stream()
                    .map(p -> p.getProcedureName() + ": " + p.getDescription())
                    .toList();
            steps.add(Map.of(
                    "step", stepNum++,
                    "title", "Follow Customs Procedures (" + procs.size() + " steps)",
                    "description", String.join("; ", procs),
                    "procedures", procs,
                    "status", "REQUIRED"
            ));
        }

        // Step 7: Submit declaration
        steps.add(Map.of(
                "step", stepNum++,
                "title", "Submit Export Declaration",
                "description", "File shipping bill/export declaration with customs authority",
                "status", "REQUIRED"
        ));

        guide.put("steps", steps);
        guide.put("totalSteps", steps.size());

        // Official sources
        List<Map<String, String>> sources = reg.sources.stream()
                .map(s -> Map.of(
                        "authority", s.getAuthority() != null ? s.getAuthority() : "",
                        "title", s.getTitle() != null ? s.getTitle() : "",
                        "url", s.getSourceUrl() != null ? s.getSourceUrl() : ""
                ))
                .toList();
        guide.put("sources", sources);

        return ResponseEntity.ok(guide);
    }
}
