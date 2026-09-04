package com.trade.repository;

import com.trade.entity.ShipmentPlan;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentPlanRepository extends JpaRepository<ShipmentPlan, Long> {

    List<ShipmentPlan> findByExporterOrderByCreatedAtDesc(User exporter);

    Optional<ShipmentPlan> findByPlanReference(String planReference);
}
