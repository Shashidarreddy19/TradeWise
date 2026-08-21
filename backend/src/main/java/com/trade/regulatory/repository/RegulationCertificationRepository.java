package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationCertificationRepository extends JpaRepository<RegulationCertificationEntity, Long> {
    List<RegulationCertificationEntity> findByRegulationId(Long regulationId);
    List<RegulationCertificationEntity> findByRegulationIdIn(List<Long> regulationIds);
}
