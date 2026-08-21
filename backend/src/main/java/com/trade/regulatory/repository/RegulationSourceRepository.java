package com.trade.regulatory.repository;

import com.trade.regulatory.entity.RegulationSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulationSourceRepository extends JpaRepository<RegulationSourceEntity, Long> {
    List<RegulationSourceEntity> findByCountry(String country);
    List<RegulationSourceEntity> findByCountryAndStatus(String country, String status);
}
