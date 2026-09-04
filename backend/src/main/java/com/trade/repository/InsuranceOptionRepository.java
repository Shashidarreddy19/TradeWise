package com.trade.repository;

import com.trade.entity.InsuranceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceOptionRepository extends JpaRepository<InsuranceOption, Long> {

    Optional<InsuranceOption> findByPolicyType(String policyType);
}
