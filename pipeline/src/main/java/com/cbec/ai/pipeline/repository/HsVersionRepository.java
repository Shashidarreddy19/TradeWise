package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.HsVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HsVersionRepository extends JpaRepository<HsVersionEntity, Long> {
    List<HsVersionEntity> findByCustomsTerritoryAndNationalCode(String customsTerritory, String nationalCode);
    List<HsVersionEntity> findByHsMasterId(Long hsMasterId);
}
