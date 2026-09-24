package com.trade.service.impl;

import com.trade.dto.country.CountryResponse;
import com.trade.repository.CountryRepository;
import com.trade.service.CountryService;
import com.trade.util.MappingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CountryResponse> getAllCountries() {
        return countryRepository.findAll()
                .stream()
                .map(MappingUtil::toCountryResponse)
                .toList();
    }

    @Override
    @Transactional
    public CountryResponse ensureCountry(String name, String code, String currency) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Country name must not be blank");
        }
        String trimmedName = name.trim();
        var existing = countryRepository.findByNameIgnoreCase(trimmedName);
        if (existing.isPresent()) {
            return MappingUtil.toCountryResponse(existing.get());
        }

        String resolvedCode = (code != null && !code.isBlank())
                ? code.trim().toUpperCase()
                : (trimmedName.length() >= 2 ? trimmedName.substring(0, 2).toUpperCase() : "XX");

        var existingByCode = countryRepository.findByCodeIgnoreCase(resolvedCode);
        if (existingByCode.isPresent()) {
            return MappingUtil.toCountryResponse(existingByCode.get());
        }

        String resolvedCurrency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "USD";

        com.trade.entity.Country newCountry = com.trade.entity.Country.builder()
                .name(trimmedName)
                .code(resolvedCode)
                .currency(resolvedCurrency)
                .active(true)
                .build();

        newCountry = countryRepository.save(newCountry);
        return MappingUtil.toCountryResponse(newCountry);
    }
}

