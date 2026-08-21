package com.trade.service;

import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentStatusRequest;

import java.util.List;

/**
 * Contract for shipment management operations.
 * Shipments are auto-created when a logistics partner accepts a request.
 * Partners only list, view, and update status.
 */
public interface ShipmentService {

    List<ShipmentResponse> getAllShipmentsForPartner(String partnerEmail);

    ShipmentResponse getShipmentById(Long id, String partnerEmail);

    ShipmentResponse updateShipmentStatus(Long id, ShipmentStatusRequest request, String partnerEmail);
}
