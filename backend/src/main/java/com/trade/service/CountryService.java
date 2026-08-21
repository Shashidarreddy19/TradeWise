package com.trade.service;

import com.trade.dto.country.CountryResponse;

import java.util.List;

/**
 * Contract for country lookup operations.
 */
public interface CountryService {

    List<CountryResponse> getAllCountries();
}
