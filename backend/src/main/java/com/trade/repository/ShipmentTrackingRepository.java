package com.trade.repository;

import com.trade.entity.Shipment;
import com.trade.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByShipmentOrderByCreatedAtDesc(Shipment shipment);

    List<ShipmentTracking> findByShipmentOrderByCreatedAtAsc(Shipment shipment);
}
