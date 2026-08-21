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
}
