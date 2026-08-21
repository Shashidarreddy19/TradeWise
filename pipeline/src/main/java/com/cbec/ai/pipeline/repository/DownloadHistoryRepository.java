package com.cbec.ai.pipeline.repository;

import com.cbec.ai.pipeline.model.entity.DownloadHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadHistoryRepository extends JpaRepository<DownloadHistoryEntity, Long> {
    List<DownloadHistoryEntity> findByCountry(String country);
}
