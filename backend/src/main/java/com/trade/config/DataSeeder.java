package com.trade.config;

import com.trade.entity.*;
import com.trade.repository.*;

import lombok.Builder;
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
        if (countryRepository.count() > 0) {
            log.info("Countries already seeded — skipping.");
            return;
        }
        upsertCountry("Germany", "EUR");
        upsertCountry("United States", "USD");
        upsertCountry("United Arab Emirates", "AED");
        upsertCountry("Singapore", "SGD");
        upsertCountry("Australia", "AUD");
        log.info("Ensured 5 required countries are present.");
    }

    private void upsertCountry(String name, String currency) {
        if (countryRepository.findByName(name).isEmpty()) {
            String code = switch (name) {
                case "Germany"               -> "DE";
                case "United States"         -> "US";
                case "United Arab Emirates"  -> "AE";
                case "Singapore"             -> "SG";
                case "Australia"             -> "AU";
                default -> name.substring(0, 2).toUpperCase();
            };
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
    @Builder
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
