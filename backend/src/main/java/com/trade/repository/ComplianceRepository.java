package com.trade.repository;

import com.trade.entity.Compliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplianceRepository extends JpaRepository<Compliance, Long> {

    /**
     * Find compliance rules for a specific country and product category.
     * This is the primary query used by the market-analysis endpoint.
     */
    Optional<Compliance> findByCountryIdAndCategoryId(Long countryId, Long categoryId);

    boolean existsByCountryIdAndCategoryId(Long countryId, Long categoryId);
}
