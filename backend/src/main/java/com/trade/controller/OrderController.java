package com.trade.controller;

import com.trade.dto.ApiResponse;
import com.trade.dto.order.OrderRequest;
import com.trade.dto.order.OrderResponse;
import com.trade.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages Export Shipment Requests.
 *
 * EXPORTER endpoints:
 *   GET    /api/orders          – list own requests
 *   GET    /api/orders/{id}     – get one request
 *   POST   /api/orders          – create a shipment request (status = PENDING_LOGISTICS)
 *   PUT    /api/orders/{id}     – update request (PENDING_LOGISTICS only)
 *
 * LOGISTICS endpoints:
 *   GET    /api/orders/pending           – all PENDING_LOGISTICS requests (filtered to exclude own rejections)
 *   PATCH  /api/orders/{id}/accept       – accept → status = LOGISTICS_ACCEPTED + shipment auto-created
 *   PATCH  /api/orders/{id}/reject       – reject this request (stays visible to other partners)
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── EXPORTER endpoints ───────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<OrderResponse> orders = orderService.getAllOrdersForExporter(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponse order = orderService.getOrderById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponse order = orderService.createOrder(request, userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment request created successfully", order));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EXPORTER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponse order = orderService.updateOrder(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Shipment request updated successfully", order));
    }

    // ── LOGISTICS endpoints ──────────────────────────────────────────────────

    /**
     * Returns all PENDING_LOGISTICS requests excluding those already rejected by this partner.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<OrderResponse> orders = orderService.getPendingRequests(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Accept a shipment request. Auto-creates a shipment with status ASSIGNED.
     */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponse order = orderService.acceptRequest(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request accepted. Shipment created successfully.", order));
    }

    /**
     * Reject a shipment request. Request stays available to other logistics partners.
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('LOGISTICS')")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        OrderResponse order = orderService.rejectRequest(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Request rejected.", order));
    }
}
