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
 * Shipment tracking for logistics partners.
 * Shipments are auto-created when a request is accepted — no manual creation endpoint.
 *
 * GET    /api/shipments              – list all shipments assigned to current partner
 * GET    /api/shipments/{id}         – get one shipment
 * PATCH  /api/shipments/{id}/status  – update shipment tracking status
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LOGISTICS')")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getMyShipments(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ShipmentResponse> shipments =
                shipmentService.getAllShipmentsForPartner(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ShipmentResponse shipment = shipmentService.getShipmentById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shipment));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ShipmentResponse shipment =
                shipmentService.updateShipmentStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Shipment status updated", shipment));
    }
}
