package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationRawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationRawRepository extends JpaRepository<RegulationRawEntity, Long> {

    List<RegulationRawEntity> findByCountry(String country);

    List<RegulationRawEntity> findByCountryAndAuthority(String country, String authority);

    List<RegulationRawEntity> findByDownloadId(Long downloadId);
}
