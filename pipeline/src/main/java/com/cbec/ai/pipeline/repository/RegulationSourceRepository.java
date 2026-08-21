package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegulationSourceRepository extends JpaRepository<RegulationSourceEntity, Long> {

    List<RegulationSourceEntity> findByCountry(String country);

    List<RegulationSourceEntity> findByCountryAndStatus(String country, String status);

    Optional<RegulationSourceEntity> findByCountryAndSourceUrl(String country, String sourceUrl);
}
