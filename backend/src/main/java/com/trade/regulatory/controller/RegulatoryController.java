package com.trade.regulatory.controller;

import com.trade.regulatory.entity.*;
import com.trade.regulatory.repository.*;
import com.trade.regulatory.service.RegulatoryRetrievalService;
import com.trade.regulatory.service.RegulatoryRetrievalService.ComplianceScore;
import com.trade.regulatory.service.RegulatoryRetrievalService.RegulatoryResult;
import com.trade.regulatory.service.RegulatoryKnowledgeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Regulatory intelligence REST API.
 * Reads from TradeData (real government source data).
 * All endpoints are authenticated through Spring Security.
 */
@RestController
@RequestMapping("/api/v1")
public class RegulatoryController {

    private final RegulatoryRetrievalService retrievalService;
    private final HsMasterRepository hsMasterRepo;
    private final CountryMasterRepository countryRepo;
    private final RegulationSourceRepository sourceRepo;
    private final RegulatoryKnowledgeService knowledgeService;

    public RegulatoryController(
            RegulatoryRetrievalService retrievalService,
            HsMasterRepository hsMasterRepo,
            CountryMasterRepository countryRepo,
            RegulationSourceRepository sourceRepo,
            RegulatoryKnowledgeService knowledgeService) {
        this.retrievalService = retrievalService;
        this.hsMasterRepo = hsMasterRepo;
        this.countryRepo = countryRepo;
        this.sourceRepo = sourceRepo;
        this.knowledgeService = knowledgeService;
    }

    /**
     * GET /api/v1/regulations/{country}/{hsCode}
     * Full regulatory profile with hierarchical HS matching.
     */
    @GetMapping("/regulations/{country}/{hsCode}")
    public ResponseEntity<?> getRegulations(
            @PathVariable String country,
            @PathVariable String hsCode) {

        RegulatoryResult result = retrievalService.getRegulations(country, hsCode);
        if (!result.supported) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.error
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("country", result.country);
        response.put("hsCode", result.hsCode);
        response.put("productDescription", result.productDescription);
        response.put("category", result.category);
        response.put("matchType", result.matchType);
        response.put("confidence", result.confidence);
        response.put("regulationFound", result.regulationFound);

        // Check if DB returned sufficient product-specific regulatory details
        // Chapter-level matches (HS2) are too generic — augment with knowledge base
        boolean hasSpecificData = (result.documents != null && result.documents.size() >= 5)
                || (result.certifications != null && result.certifications.size() >= 3);
        boolean isGenericMatch = "HS2_CHAPTER".equals(result.matchType) 
                || "NOT_FOUND".equals(result.matchType)
                || "COVERAGE_AUDIT".equals(result.matchType);

        if (hasSpecificData && !isGenericMatch) {
            // Use structured DB data
            response.put("regulations", result.regulations);
            response.put("documents", result.documents);
            response.put("certifications", result.certifications);
            response.put("labeling", result.labeling);
            response.put("restrictions", result.restrictions);
            response.put("procedures", result.procedures);
            response.put("sources", result.sources);
            response.put("dataSource", "STRUCTURED_DB");
        } else {
            // Fallback to knowledge-based regulations
            Map<String, Object> kb = knowledgeService.getKnowledgeBasedRegulations(
                    country, hsCode, result.productDescription, result.category);
            response.put("import_regulations", kb.get("import_regulations"));
            response.put("customs_rules", kb.get("customs_rules"));
            response.put("labeling_requirements", kb.get("labeling_requirements"));
            response.put("packaging_requirements", List.of());
            response.put("restricted_products", kb.get("restricted_products"));
            response.put("required_documents", kb.get("required_documents"));
            response.put("certifications_list", kb.get("certifications"));
            // Also put in standard field names for backward compatibility
            response.put("regulations", kb.get("import_regulations"));
            response.put("documents", kb.get("required_documents"));
            response.put("certifications", kb.get("certifications"));
            response.put("labeling", kb.get("labeling_requirements"));
            response.put("restrictions", kb.get("restricted_products"));
            response.put("procedures", kb.get("customs_rules"));
            response.put("sources", List.of(Map.of("source", "Knowledge Base", "url", "")));
            response.put("dataSource", "KNOWLEDGE_BASE");
            response.put("disclaimer", kb.get("disclaimer"));
        }

        // Always add anti-dumping and packaging verification (regardless of DB vs knowledge source)
        response.put("antiDumping", knowledgeService.getAntiDumpingStatus(country, hsCode, result.productDescription));
        response.put("packagingVerification", knowledgeService.getPackagingRequirements(country, hsCode, result.productDescription));

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/compliance/{country}/{hsCode}
     * Compliance score calculation.
     */
    @GetMapping("/compliance/{country}/{hsCode}")
    public ResponseEntity<?> getCompliance(
            @PathVariable String country,
            @PathVariable String hsCode) {

        RegulatoryResult regResult = retrievalService.getRegulations(country, hsCode);
        if (!regResult.supported) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", regResult.error));
        }
        ComplianceScore score = retrievalService.calculateCompliance(regResult);

        // Use knowledge-based scoring when DB data is generic (chapter-level match)
        // or when there's insufficient specific data
        boolean isGenericMatch = "HS2_CHAPTER".equals(regResult.matchType)
                || "NOT_FOUND".equals(regResult.matchType)
                || "COVERAGE_AUDIT".equals(regResult.matchType);
        boolean hasMinimalData = score.documentsCount < 5 && score.certificationsCount < 3;

        if (isGenericMatch || hasMinimalData) {
            Map<String, Object> kb = knowledgeService.getKnowledgeBasedRegulations(
                    country, hsCode, regResult.productDescription, regResult.category);
            score.numericScore = knowledgeService.calculateScore(kb);
            score.complexity = knowledgeService.getComplexity(kb);
            score.documentsCount = ((List<?>) kb.getOrDefault("required_documents", List.of())).size();
            score.certificationsCount = ((List<?>) kb.getOrDefault("certifications", List.of())).size();
            score.labelingCount = ((List<?>) kb.getOrDefault("labeling_requirements", List.of())).size();
            score.restrictionsCount = ((List<?>) kb.getOrDefault("restricted_products", List.of())).size();
            score.proceduresCount = ((List<?>) kb.getOrDefault("customs_rules", List.of())).size();
            score.regulationFound = true;
            score.regulationFound = true;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("country", regResult.country);
        response.put("hsCode", regResult.hsCode);
        response.put("complianceScore", score.numericScore);
        response.put("complexity", score.complexity);
        response.put("matchType", score.matchType);
        response.put("confidence", score.confidence);
        response.put("regulationFound", score.regulationFound);
        response.put("documentsCount", score.documentsCount);
        response.put("certificationsCount", score.certificationsCount);
        response.put("labelingCount", score.labelingCount);
        response.put("restrictionsCount", score.restrictionsCount);
        response.put("proceduresCount", score.proceduresCount);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/documents/{country}/{hsCode}
     */
    @GetMapping("/documents/{country}/{hsCode}")
    public ResponseEntity<?> getDocuments(
            @PathVariable String country,
            @PathVariable String hsCode) {
        RegulatoryResult r = retrievalService.getRegulations(country, hsCode);
        if (!r.supported) return ResponseEntity.badRequest().body(Map.of("error", r.error));
        return ResponseEntity.ok(Map.of("documents", r.documents, "matchType", r.matchType, "confidence", r.confidence));
    }

    /**
     * GET /api/v1/certificates/{country}/{hsCode}
     */
    @GetMapping("/certificates/{country}/{hsCode}")
    public ResponseEntity<?> getCertificates(
            @PathVariable String country,
            @PathVariable String hsCode) {
        RegulatoryResult r = retrievalService.getRegulations(country, hsCode);
        if (!r.supported) return ResponseEntity.badRequest().body(Map.of("error", r.error));
        return ResponseEntity.ok(Map.of("certifications", r.certifications, "matchType", r.matchType, "confidence", r.confidence));
    }

    /**
     * GET /api/v1/requirements/{country}/{hsCode}
     * Aggregate: documents + certifications + labeling + restrictions + procedures
     */
    @GetMapping("/requirements/{country}/{hsCode}")
    public ResponseEntity<?> getRequirements(
            @PathVariable String country,
            @PathVariable String hsCode) {
        RegulatoryResult r = retrievalService.getRegulations(country, hsCode);
        if (!r.supported) return ResponseEntity.badRequest().body(Map.of("error", r.error));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("country", r.country);
        resp.put("hsCode", r.hsCode);
        resp.put("matchType", r.matchType);
        resp.put("confidence", r.confidence);
        resp.put("documents", r.documents);
        resp.put("certifications", r.certifications);
        resp.put("labeling", r.labeling);
        resp.put("restrictions", r.restrictions);
        resp.put("procedures", r.procedures);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/v1/hs/search?query=...&country=...&page=0&size=20
     * HS code and product description search.
     */
    @GetMapping("/hs/search")
    public ResponseEntity<?> searchHs(
            @RequestParam String query,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<HsMasterEntity> results;
        PageRequest pageReq = PageRequest.of(page, Math.min(size, 100));

        if (country != null && !country.isBlank()) {
            results = hsMasterRepo.searchByCountryAndQuery(country, query, pageReq);
        } else if (query.matches("\\d+")) {
            results = hsMasterRepo.searchByCodePrefix(query, pageReq);
        } else {
            results = hsMasterRepo.searchByDescription(query, pageReq);
        }

        return ResponseEntity.ok(Map.of(
                "results", results.getContent(),
                "totalElements", results.getTotalElements(),
                "totalPages", results.getTotalPages(),
                "page", page
        ));
    }

    /**
     * GET /api/v1/hs/{hsCode}
     * Single HS code lookup (across all countries).
     */
    @GetMapping("/hs/{hsCode}")
    public ResponseEntity<?> getHsCode(@PathVariable String hsCode) {
        String clean = hsCode.replaceAll("[^0-9]", "");
        if (clean.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "HS code too short"));
        }
        // Try as national code first, then hs6, then heading
        Page<HsMasterEntity> results = hsMasterRepo.searchByCodePrefix(clean, PageRequest.of(0, 50));
        return ResponseEntity.ok(Map.of(
                "results", results.getContent(),
                "totalElements", results.getTotalElements()
        ));
    }

    /**
     * GET /api/v1/countries
     * List supported destination countries from TradeData.
     */
    @GetMapping("/countries")
    public ResponseEntity<?> getCountries() {
        List<CountryMasterEntity> countries = countryRepo.findByActiveTrue();
        return ResponseEntity.ok(Map.of("countries", countries));
    }

    /**
     * GET /api/v1/dashboard/statistics
     * Regulatory database coverage statistics.
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalHsCodes", hsMasterRepo.countByIsCurrentTrue());
        stats.put("hsByTerritory", hsMasterRepo.countByTerritory());
        stats.put("totalSources", sourceRepo.count());
        stats.put("supportedCountries", countryRepo.findByActiveTrue().size());
        return ResponseEntity.ok(stats);
    }
}
