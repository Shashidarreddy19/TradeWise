package com.trade.regulatory.service;

import com.trade.regulatory.entity.GovernmentIncentiveEntity;
import com.trade.regulatory.repository.GovernmentIncentiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Government incentive lookup service.
 * Returns ONLY real government schemes stored in TradeData.
 * NEVER fabricates schemes, benefits, or eligibility criteria.
 */
@Service
@Transactional(readOnly = true, transactionManager = "tradeDataTransactionManager")
public class GovernmentIncentivesService {

    private final GovernmentIncentiveRepository repo;

    public GovernmentIncentivesService(GovernmentIncentiveRepository repo) {
        this.repo = repo;
    }

    public List<GovernmentIncentiveEntity> getForCountry(String country) {
        return repo.findByCountryAndStatus(country, "ACTIVE");
    }

    public List<GovernmentIncentiveEntity> getForCountryAndHs(String country, String hsCode) {
        String chapter = hsCode.length() >= 2 ? hsCode.substring(0, 2) : "";
        String hs6 = hsCode.length() >= 6 ? hsCode.substring(0, 6) : hsCode;
        List<GovernmentIncentiveEntity> results = repo.findApplicable(country, chapter, hs6);
        if (results.isEmpty()) {
            // Fall back to all country incentives if none specifically applicable
            return repo.findByCountryAndStatus(country, "ACTIVE");
        }
        return results;
    }
}
