package com.cbec.ai.pipeline.intelligence.controller;

import com.cbec.ai.pipeline.intelligence.service.CountryComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CountryComparisonController {

    private final CountryComparisonService comparisonService;

    public CountryComparisonController(CountryComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * Country Comparison API
     * GET /api/v1/compare?hsCode=09011100&countries=United Kingdom,Germany,Japan,UAE
     */
    @GetMapping("/compare")
    public ResponseEntity<CountryComparisonService.CountryComparisonResponseDto> compareCountries(
            @RequestParam(name = "hsCode", defaultValue = "09011100") String hsCode,
            @RequestParam(name = "countries", defaultValue = "UAE,United Kingdom,Germany,Canada,Japan") String countriesParam) {

        List<String> countryList = Arrays.asList(countriesParam.split(","));
        return ResponseEntity.ok(comparisonService.compareCountries(hsCode, countryList));
    }
}
