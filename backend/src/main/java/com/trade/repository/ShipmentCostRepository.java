package com.trade.repository;

import com.trade.entity.ShipmentCost;
import com.trade.entity.ShipmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentCostRepository extends JpaRepository<ShipmentCost, Long> {

    Optional<ShipmentCost> findByShipmentPlan(ShipmentPlan shipmentPlan);
}
