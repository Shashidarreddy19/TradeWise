package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.TariffStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffStatisticsRepository extends JpaRepository<TariffStatisticsEntity, Long> {
    Optional<TariffStatisticsEntity> findByHs6AndDestinationCountryAndYear(String hs6, String destinationCountry, Integer year);
    List<TariffStatisticsEntity> findByDestinationCountry(String destinationCountry);
    List<TariffStatisticsEntity> findByHs6(String hs6);
    List<TariffStatisticsEntity> findByHs6AndDestinationCountry(String hs6, String destinationCountry);
}
