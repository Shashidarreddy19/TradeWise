package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.MarketAccessIndicatorsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketAccessIndicatorsRepository extends JpaRepository<MarketAccessIndicatorsEntity, Long> {
    Optional<MarketAccessIndicatorsEntity> findByCountryAndYear(String country, Integer year);
    List<MarketAccessIndicatorsEntity> findByCountry(String country);
}
