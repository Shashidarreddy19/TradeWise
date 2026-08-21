package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CountryRiskIndicatorsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRiskIndicatorsRepository extends JpaRepository<CountryRiskIndicatorsEntity, Long> {
    Optional<CountryRiskIndicatorsEntity> findByCountryAndYear(String country, Integer year);
    List<CountryRiskIndicatorsEntity> findByCountry(String country);
}
