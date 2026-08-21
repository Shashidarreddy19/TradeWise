package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CountryEconomicIndicatorsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryEconomicIndicatorsRepository extends JpaRepository<CountryEconomicIndicatorsEntity, Long> {
    Optional<CountryEconomicIndicatorsEntity> findByCountryAndYear(String country, Integer year);
    List<CountryEconomicIndicatorsEntity> findByCountry(String country);
}
