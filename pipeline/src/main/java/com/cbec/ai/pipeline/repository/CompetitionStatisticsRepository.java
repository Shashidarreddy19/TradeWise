package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.CompetitionStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionStatisticsRepository extends JpaRepository<CompetitionStatisticsEntity, Long> {
    Optional<CompetitionStatisticsEntity> findByHs6AndDestinationCountryAndYear(String hs6, String destinationCountry, Integer year);
    List<CompetitionStatisticsEntity> findByDestinationCountry(String destinationCountry);
    List<CompetitionStatisticsEntity> findByHs6(String hs6);
    List<CompetitionStatisticsEntity> findByHs6AndDestinationCountry(String hs6, String destinationCountry);
}
