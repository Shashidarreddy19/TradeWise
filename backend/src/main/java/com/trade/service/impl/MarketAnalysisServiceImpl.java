package com.trade.service.impl;

import com.trade.dto.market.MarketAnalysisRequest;
import com.trade.dto.market.MarketAnalysisResponse;
import com.trade.entity.Compliance;
import com.trade.entity.Country;
import com.trade.entity.Product;
import com.trade.exception.ResourceNotFoundException;
import com.trade.repository.ComplianceRepository;
import com.trade.repository.CountryRepository;
import com.trade.repository.ProductRepository;
import com.trade.service.MarketAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Retrieves compliance and trade information from the database.
 *
 * NO AI is used here. This service is intentionally structured so that
 * a future /api/rag/* module can call this service to get structured context
 * and then augment it with LLM-generated insights, without modifying this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketAnalysisServiceImpl implements MarketAnalysisService {

    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;
    private final ComplianceRepository complianceRepository;

    @Override
    @Transactional(readOnly = true)
    public MarketAnalysisResponse analyze(MarketAnalysisRequest request) {
        // Resolve product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Resolve country
        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country", "id", request.getCountryId()));

        // Build the base response from product + country data
        MarketAnalysisResponse.MarketAnalysisResponseBuilder builder = MarketAnalysisResponse.builder()
                .countryId(country.getId())
                .countryName(country.getName())
                .currency(country.getCurrency())
                .productId(product.getId())
                .productName(product.getName())
                .hsCode(product.getHsCode())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getCategoryName());

        // Look up compliance data for this country + category combination
        Optional<Compliance> complianceOpt = complianceRepository
                .findByCountryIdAndCategoryId(country.getId(), product.getCategory().getId());

        if (complianceOpt.isPresent()) {
            Compliance compliance = complianceOpt.get();
            builder
                .customsDuty(compliance.getCustomsDuty())
                .requiredCertificates(compliance.getRequiredCertificates())
                .requiredDocuments(compliance.getRequiredDocuments())
                .restrictedItems(compliance.getRestrictedItems())
                .transitTime(compliance.getTransitTime())
                .recommendedPort(compliance.getRecommendedPort())
                .complianceDataAvailable(true);

            log.info("Market analysis returned compliance data for country=[{}] category=[{}]",
                    country.getName(), product.getCategory().getCategoryName());
        } else {
            builder.complianceDataAvailable(false);
            log.warn("No compliance record found for country=[{}] category=[{}]",
                    country.getName(), product.getCategory().getCategoryName());
        }

        return builder.build();
    }
}
