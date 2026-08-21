package com.trade.regulatory.controller;

import com.trade.regulatory.dto.HsClassificationRequest;
import com.trade.regulatory.dto.HsClassificationResponse;
import com.trade.regulatory.service.HsClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HS Code Classification API.
 * AI-assisted classification of products to Indian HS/ITC codes.
 *
 * POST /api/v1/hs/classify — main classification endpoint
 * POST /api/v1/hs/classify/explain — classification with detailed explanation
 */
@RestController
@RequestMapping("/api/v1/hs")
@RequiredArgsConstructor
public class HsClassificationController {

    private final HsClassificationService classificationService;

    /**
     * POST /api/v1/hs/classify
     * Main HS classification endpoint.
     * Accepts product details, returns ranked HS code candidates with confidence scores.
     */
    @PostMapping("/classify")
    public ResponseEntity<HsClassificationResponse> classifyProduct(
            @RequestBody HsClassificationRequest request) {

        // Default origin to India if not specified
        if (request.getOriginCountry() == null || request.getOriginCountry().isBlank()) {
            request.setOriginCountry("India");
        }

        HsClassificationResponse response = classificationService.classify(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/hs/classify/explain
     * Same as classify but returns additional explanation metadata.
     */
    @PostMapping("/classify/explain")
    public ResponseEntity<HsClassificationResponse> classifyWithExplanation(
            @RequestBody HsClassificationRequest request) {
        if (request.getOriginCountry() == null || request.getOriginCountry().isBlank()) {
            request.setOriginCountry("India");
        }
        return ResponseEntity.ok(classificationService.classify(request));
    }

    /**
     * POST /api/v1/hs/recommend
     * Alias for /classify — matches the product recommendation engine spec.
     * Accepts: productName, category, description, material, usage, productType,
     *          additionalSpecifications, originCountry
     */
    @PostMapping("/recommend")
    public ResponseEntity<HsClassificationResponse> recommendHsCode(
            @RequestBody HsClassificationRequest request) {
        if (request.getOriginCountry() == null || request.getOriginCountry().isBlank()) {
            request.setOriginCountry("India");
        }
        return ResponseEntity.ok(classificationService.classify(request));
    }

    /**
     * GET /api/v1/hs/candidates?product=...&category=...
     * Quick candidate lookup without full AI ranking.
     */
    @GetMapping("/candidates")
    public ResponseEntity<HsClassificationResponse> getCandidates(
            @RequestParam String product,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String material) {

        HsClassificationRequest request = HsClassificationRequest.builder()
                .productName(product)
                .category(category != null ? category : "")
                .material(material != null ? material : "")
                .description(product)
                .originCountry("India")
                .build();

        return ResponseEntity.ok(classificationService.classify(request));
    }
}
