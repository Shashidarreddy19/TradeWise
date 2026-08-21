package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.RegulationDownloadHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationDownloadHistoryRepository extends JpaRepository<RegulationDownloadHistoryEntity, Long> {

    List<RegulationDownloadHistoryEntity> findByCountry(String country);

    List<RegulationDownloadHistoryEntity> findBySourceId(Long sourceId);

    java.util.Optional<RegulationDownloadHistoryEntity> findTopBySourceIdOrderByDownloadTimeDesc(Long sourceId);
}
