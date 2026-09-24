package com.trade.service;

import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentStatusRequest;

import java.util.List;

/**
 * Contract for shipment management and tracking operations.
 */
public interface ShipmentService {

    List<ShipmentResponse> getAllShipmentsForPartner(String partnerEmail);

    List<ShipmentResponse> getAllShipmentsForExporter(String exporterEmail);

    ShipmentResponse getShipmentById(Long id, String userEmail);

    ShipmentResponse getShipmentByOrderId(Long orderId, String userEmail);

    ShipmentResponse updateShipmentStatus(Long id, ShipmentStatusRequest request, String partnerEmail);
}
