package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CurrencyIndicatorsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyIndicatorsRepository extends JpaRepository<CurrencyIndicatorsEntity, Long> {
    Optional<CurrencyIndicatorsEntity> findByCountryAndYear(String country, Integer year);
    List<CurrencyIndicatorsEntity> findByCountry(String country);
}
