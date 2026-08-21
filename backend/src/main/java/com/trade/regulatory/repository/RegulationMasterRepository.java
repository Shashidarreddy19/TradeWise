package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationMasterRepository extends JpaRepository<RegulationMasterEntity, Long> {

    List<RegulationMasterEntity> findByCountry(String country);

    List<RegulationMasterEntity> findByCountryAndRegulationType(String country, String regulationType);
}
