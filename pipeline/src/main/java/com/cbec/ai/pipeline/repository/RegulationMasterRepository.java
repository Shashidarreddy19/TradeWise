package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegulationMasterRepository extends JpaRepository<RegulationMasterEntity, Long> {

    List<RegulationMasterEntity> findByCountry(String country);

    Optional<RegulationMasterEntity> findByCountryAndTitle(String country, String title);

    List<RegulationMasterEntity> findByIdIn(List<Long> ids);

    List<RegulationMasterEntity> findByCountryAndIdIn(String country, List<Long> ids);
}
