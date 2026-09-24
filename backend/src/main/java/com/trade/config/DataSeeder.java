package com.trade.config;

import com.trade.entity.*;
import com.trade.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with reference data on first startup.
 * Idempotent — checks existence before inserting.
 *
 * Seeded data:
 *  - 5 supported countries (for InternationalTrade order/shipment workflow)
 *  - 24 product categories
 *
 * NOTE: Regulatory compliance data (documents, certifications, labeling,
 * restrictions, procedures) now comes from the TradeData database populated
 * by the pipeline project. See RegulatoryRetrievalService.
 */
@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CountryRepository countryRepository;
    private final ProductCategoryRepository categoryRepository;

    public DataSeeder(CountryRepository countryRepository,
                      ProductCategoryRepository categoryRepository) {
        this.countryRepository = countryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCountries();
        seedCategories();
    }

    // ── Countries ────────────────────────────────────────────────────────────

    private void seedCountries() {
        // Idempotently ensure each destination country is present
        upsertCountry("Germany", "DE", "EUR");
        upsertCountry("United States", "US", "USD");
        upsertCountry("United Arab Emirates", "AE", "AED");
        upsertCountry("Singapore", "SG", "SGD");
        upsertCountry("Australia", "AU", "AUD");
        upsertCountry("Saudi Arabia", "SA", "SAR");
        upsertCountry("United Kingdom", "GB", "GBP");
        upsertCountry("Netherlands", "NL", "EUR");
        upsertCountry("Canada", "CA", "CAD");
        upsertCountry("Japan", "JP", "JPY");
        upsertCountry("South Korea", "KR", "KRW");
        upsertCountry("China", "CN", "CNY");
        upsertCountry("Hong Kong", "HK", "HKD");
        upsertCountry("Bangladesh", "BD", "BDT");
        upsertCountry("Malaysia", "MY", "MYR");
        upsertCountry("Indonesia", "ID", "IDR");
        upsertCountry("Vietnam", "VN", "VND");
        upsertCountry("Thailand", "TH", "THB");
        upsertCountry("South Africa", "ZA", "ZAR");
        upsertCountry("Brazil", "BR", "BRL");
        upsertCountry("France", "FR", "EUR");
        upsertCountry("Italy", "IT", "EUR");
        upsertCountry("Kuwait", "KW", "KWD");
        upsertCountry("Qatar", "QA", "QAR");
        upsertCountry("Oman", "OM", "OMR");
        upsertCountry("Bahrain", "BH", "BHD");
        log.info("Ensured required export destination countries are present in database.");
    }

    private void upsertCountry(String name, String code, String currency) {
        if (countryRepository.findByNameIgnoreCase(name).isEmpty() && countryRepository.findByCodeIgnoreCase(code).isEmpty()) {
            countryRepository.save(
                Country.builder()
                    .name(name)
                    .code(code)
                    .currency(currency)
                    .active(true)
                    .build()
            );
            log.info("Inserted country: {} ({})", name, code);
        }
    }


    // ── Categories ───────────────────────────────────────────────────────────
    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already seeded — skipping.");
            return;
        }

        List<ProductCategory> categories = List.of(
            ProductCategory.builder().categoryName("Agricultural Products").build(),
            ProductCategory.builder().categoryName("Spices").build(),
            ProductCategory.builder().categoryName("Food Products").build(),
            ProductCategory.builder().categoryName("Processed Foods").build(),
            ProductCategory.builder().categoryName("Marine Products").build(),
            ProductCategory.builder().categoryName("Textiles").build(),
            ProductCategory.builder().categoryName("Apparel & Garments").build(),
            ProductCategory.builder().categoryName("Home Textiles").build(),
            ProductCategory.builder().categoryName("Leather Products").build(),
            ProductCategory.builder().categoryName("Footwear").build(),
            ProductCategory.builder().categoryName("Handicrafts").build(),
            ProductCategory.builder().categoryName("Ceramics & Pottery").build(),
            ProductCategory.builder().categoryName("Glassware").build(),
            ProductCategory.builder().categoryName("Jewellery & Gems").build(),
            ProductCategory.builder().categoryName("Chemicals").build(),
            ProductCategory.builder().categoryName("Cosmetics & Personal Care").build(),
            ProductCategory.builder().categoryName("Pharmaceuticals").build(),
            ProductCategory.builder().categoryName("Plastics & Rubber").build(),
            ProductCategory.builder().categoryName("Electronics").build(),
            ProductCategory.builder().categoryName("Engineering Goods").build(),
            ProductCategory.builder().categoryName("Machinery").build(),
            ProductCategory.builder().categoryName("Automotive Components").build(),
            ProductCategory.builder().categoryName("Furniture & Wood").build(),
            ProductCategory.builder().categoryName("Others").build()
        );

        categoryRepository.saveAll(categories);
        log.info("Seeded {} product categories.", categories.size());
    }
}
