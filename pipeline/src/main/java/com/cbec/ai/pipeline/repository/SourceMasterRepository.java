package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.SourceMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceMasterRepository extends JpaRepository<SourceMasterEntity, Long> {
    List<SourceMasterEntity> findByCountry(String country);
    List<SourceMasterEntity> findByCustomsTerritory(String customsTerritory);
    List<SourceMasterEntity> findByIsActiveTrue();
}
