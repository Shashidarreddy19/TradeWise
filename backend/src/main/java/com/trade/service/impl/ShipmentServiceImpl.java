package com.trade.service.impl;

import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentStatusRequest;
import com.trade.entity.*;
import com.trade.exception.ResourceNotFoundException;
import com.trade.exception.UnauthorizedException;
import com.trade.repository.OrderRepository;
import com.trade.repository.ShipmentRepository;
import com.trade.repository.ShipmentTrackingRepository;
import com.trade.repository.UserRepository;
import com.trade.service.ShipmentService;
import com.trade.util.MappingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipmentsForPartner(String partnerEmail) {
        User partner = findUserByEmail(partnerEmail);
        return shipmentRepository.findByLogisticsPartner(partner)
                .stream()
                .map(MappingUtil::toShipmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipmentsForExporter(String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        return shipmentRepository.findByOrderExporter(exporter)
                .stream()
                .map(MappingUtil::toShipmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id, String userEmail) {
        Shipment shipment = findShipmentById(id);
        verifyShipmentAccess(shipment, userEmail);
        return MappingUtil.toShipmentResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(Long orderId, String userEmail) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment for order", "orderId", orderId));
        verifyShipmentAccess(shipment, userEmail);
        return MappingUtil.toShipmentResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long id, ShipmentStatusRequest request, String partnerEmail) {
        Shipment shipment = findShipmentById(id);

        if (!shipment.getLogisticsPartner().getEmail().equals(partnerEmail)) {
            throw new UnauthorizedException("You do not have permission to manage this shipment");
        }

        shipment.setShipmentStatus(request.getShipmentStatus());
        shipment.setStatusUpdatedAt(LocalDateTime.now());

        // When DELIVERED — mirror status on the linked order
        if (request.getShipmentStatus() == ShipmentStatus.DELIVERED) {
            Order order = shipment.getOrder();
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }

        // When IN_TRANSIT — mirror on order
        if (request.getShipmentStatus() == ShipmentStatus.IN_TRANSIT) {
            Order order = shipment.getOrder();
            order.setStatus(OrderStatus.IN_TRANSIT);
            orderRepository.save(order);
        }

        shipment = shipmentRepository.save(shipment);

        // Record tracking event history
        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Status updated to " + request.getShipmentStatus().name();
        }

        ShipmentTracking tracking = ShipmentTracking.builder()
                .shipment(shipment)
                .status(request.getShipmentStatus())
                .location(request.getLocation())
                .description(desc)
                .build();
        trackingRepository.save(tracking);

        log.info("Shipment [id={}] status updated to [{}] at location [{}] by partner [{}]",
                id, request.getShipmentStatus(), request.getLocation(), partnerEmail);

        return MappingUtil.toShipmentResponse(shipment);
    }

    private Shipment findShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void verifyShipmentAccess(Shipment shipment, String userEmail) {
        User user = findUserByEmail(userEmail);
        boolean isPartner = shipment.getLogisticsPartner().getId().equals(user.getId());
        boolean isExporter = shipment.getOrder().getExporter().getId().equals(user.getId());
        if (!isPartner && !isExporter) {
            throw new UnauthorizedException("You do not have permission to access this shipment");
        }
    }
}
