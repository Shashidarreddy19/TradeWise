package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.country.CountryResponse;
import com.trade.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public endpoint for fetching supported export destination countries.
 *
 * GET  /api/countries
 * POST /api/countries/ensure
 */
@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAllCountries() {
        List<CountryResponse> countries = countryService.getAllCountries();
        return ResponseEntity.ok(ApiResponse.success(countries));
    }

    @PostMapping("/ensure")
    public ResponseEntity<ApiResponse<CountryResponse>> ensureCountry(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String code = body.get("code");
        String currency = body.get("currency");
        CountryResponse country = countryService.ensureCountry(name, code, currency);
        return ResponseEntity.ok(ApiResponse.success("Country ensured", country));
    }
}

