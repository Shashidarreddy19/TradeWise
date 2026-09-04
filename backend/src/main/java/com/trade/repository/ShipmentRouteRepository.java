package com.trade.repository;

import com.trade.entity.ShipmentPlan;
import com.trade.entity.ShipmentRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRouteRepository extends JpaRepository<ShipmentRoute, Long> {

    List<ShipmentRoute> findByShipmentPlan(ShipmentPlan shipmentPlan);
}
