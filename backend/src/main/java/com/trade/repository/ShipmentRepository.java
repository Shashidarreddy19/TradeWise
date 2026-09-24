package com.trade.repository;

import com.trade.entity.Shipment;
import com.trade.entity.ShipmentStatus;
import com.trade.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @EntityGraph(attributePaths = {
            "order", "order.product", "order.destinationCountry", "order.exporter", "order.exporter.exporterProfile",
            "logisticsPartner", "logisticsPartner.logisticsProfile", "trackingEvents"
    })
    List<Shipment> findByLogisticsPartner(User logisticsPartner);

    List<Shipment> findByLogisticsPartnerId(Long logisticsPartnerId);

    @EntityGraph(attributePaths = {
            "order", "order.product", "order.destinationCountry", "order.exporter", "order.exporter.exporterProfile",
            "logisticsPartner", "logisticsPartner.logisticsProfile", "trackingEvents"
    })
    List<Shipment> findByOrderExporter(User exporter);

    List<Shipment> findByLogisticsPartnerIdAndShipmentStatus(Long logisticsPartnerId, ShipmentStatus status);

    long countByLogisticsPartnerAndShipmentStatus(User logisticsPartner, ShipmentStatus status);

    long countByLogisticsPartnerAndShipmentStatusNot(User logisticsPartner, ShipmentStatus status);

    long countByLogisticsPartner(User logisticsPartner);

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);
}
