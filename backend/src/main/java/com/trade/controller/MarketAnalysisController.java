package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.market.MarketAnalysisRequest;
import com.trade.dto.market.MarketAnalysisResponse;
import com.trade.service.MarketAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns structured market and compliance information for a product + country pair.
 * Data is sourced from the Compliance table — NO AI involved.
 *
 * POST /api/market-analysis
 *
 * This endpoint is intentionally designed to be the data provider for a future
 * RAG module at /api/rag/* which can call this internally and enrich with AI.
 */
@RestController
@RequestMapping("/api/market-analysis")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXPORTER')")
public class MarketAnalysisController {

    private final MarketAnalysisService marketAnalysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<MarketAnalysisResponse>> analyze(
            @Valid @RequestBody MarketAnalysisRequest request) {

        MarketAnalysisResponse result = marketAnalysisService.analyze(request);
        return ResponseEntity.ok(ApiResponse.success("Market analysis data retrieved", result));
    }
}
