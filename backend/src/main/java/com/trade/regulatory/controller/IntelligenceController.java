package com.trade.regulatory.controller;

import com.trade.regulatory.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Intelligence APIs: Cost Estimation, Market Opportunity,
 * Government Incentives, Negotiation Assistant.
 * All use real data — no fabrication.
 */
@RestController
@RequestMapping("/api/v1")
public class IntelligenceController {

    private final CostEstimationService costService;
    private final MarketOpportunityService marketService;
    private final NegotiationAssistantService negotiationService;
    private final GovernmentIncentivesService incentivesService;

    public IntelligenceController(
            CostEstimationService costService,
            MarketOpportunityService marketService,
            NegotiationAssistantService negotiationService,
            GovernmentIncentivesService incentivesService) {
        this.costService = costService;
        this.marketService = marketService;
        this.negotiationService = negotiationService;
        this.incentivesService = incentivesService;
    }

    // ── Cost Estimation ───────────────────────────────────────────────────────

    @PostMapping("/cost-estimation")
    public ResponseEntity<?> estimateCostPost(@RequestBody Map<String, Object> body) {
        String country = str(body, "country");
        String hsCode = str(body, "hsCode");
        if (country.isBlank() || hsCode.isBlank())
            return bad("country and hsCode are required");

        BigDecimal value = decimal(str(body, "productValue"), BigDecimal.ZERO);
        int qty = integer(str(body, "quantity"), 1);
        String currency = str(body, "currency");

        return ResponseEntity.ok(costService.estimateCost(country, hsCode, value, qty, currency));
    }

    @GetMapping("/cost-estimation")
    public ResponseEntity<?> estimateCostGet(
            @RequestParam String country,
            @RequestParam String hsCode,
            @RequestParam(defaultValue = "0") String value,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "USD") String currency) {
        return ResponseEntity.ok(costService.estimateCost(
                country, hsCode, decimal(value, BigDecimal.ZERO), quantity, currency));
    }

    // ── Market Opportunity ───────────────────────────────────────────────────

    @GetMapping("/market-opportunity/{hsCode}")
    public ResponseEntity<?> rankAllCountries(@PathVariable String hsCode) {
        return ResponseEntity.ok(marketService.rankCountries(hsCode, null));
    }

    @GetMapping("/market-opportunity/{country}/{hsCode}")
    public ResponseEntity<?> analyzeOneCountry(
            @PathVariable String country, @PathVariable String hsCode) {
        Map<String, Object> result = marketService.rankCountries(hsCode, List.of(country));
        List<?> r = (List<?>) result.get("rankings");
        return ResponseEntity.ok(r != null && !r.isEmpty() ? r.get(0) :
                Map.of("country", country, "hsCode", hsCode, "message", "No analysis available"));
    }

    @PostMapping("/market-opportunity/rank")
    public ResponseEntity<?> rankCountries(@RequestBody Map<String, Object> body) {
        String hsCode = str(body, "hsCode");
        if (hsCode.isBlank()) return bad("hsCode is required");

        @SuppressWarnings("unchecked")
        List<String> countries = body.containsKey("countries")
                ? (List<String>) body.get("countries") : null;

        return ResponseEntity.ok(marketService.rankCountries(hsCode, countries));
    }

    // ── Government Incentives ─────────────────────────────────────────────────

    @GetMapping("/incentives/{country}")
    public ResponseEntity<?> getIncentivesByCountry(@PathVariable String country) {
        var list = incentivesService.getForCountry(country);
        return ResponseEntity.ok(Map.of(
                "country", country,
                "count", list.size(),
                "incentives", list,
                "dataNote", list.isEmpty()
                        ? "No verified incentive data available for " + country
                        : "Real government schemes only — unverified schemes are excluded"
        ));
    }

    @GetMapping("/incentives/{country}/{hsCode}")
    public ResponseEntity<?> getIncentivesByCountryAndHs(
            @PathVariable String country, @PathVariable String hsCode) {
        var list = incentivesService.getForCountryAndHs(country, hsCode);
        return ResponseEntity.ok(Map.of(
                "country", country,
                "hsCode", hsCode,
                "count", list.size(),
                "incentives", list,
                "dataNote", list.isEmpty()
                        ? "No verified incentive data available for this combination"
                        : "Real government schemes only"
        ));
    }

    // ── Negotiation Assistant ─────────────────────────────────────────────────

    @PostMapping("/ai/negotiation-assistant")
    public ResponseEntity<?> negotiate(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(negotiationService.generateNegotiationAdvice(body));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private BigDecimal decimal(String s, BigDecimal fallback) {
        try { return new BigDecimal(s); } catch (Exception e) { return fallback; }
    }

    private int integer(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
