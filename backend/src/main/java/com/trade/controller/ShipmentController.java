package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentStatusRequest;
import com.trade.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Shipment management and tracking for Logistics Partners and Exporters.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * List all shipments assigned to the authenticated logistics partner.
     */
    @GetMapping
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getMyShipments(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ShipmentResponse> shipments =
                shipmentService.getAllShipmentsForPartner(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipments));
    }

    /**
     * List all shipments for the authenticated exporter.
     */
    @GetMapping("/exporter")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getExporterShipments(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ShipmentResponse> shipments =
                shipmentService.getAllShipmentsForExporter(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipments));
    }

    /**
     * Get a shipment by ID (accessible by assigned logistics partner or exporter owner).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ShipmentResponse shipment = shipmentService.getShipmentById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    /**
     * Get shipment associated with an order ID.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        ShipmentResponse shipment = shipmentService.getShipmentByOrderId(orderId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    /**
     * Update shipment status and add a tracking event entry.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ShipmentResponse shipment =
                shipmentService.updateShipmentStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Shipment status updated", shipment));
    }
}
