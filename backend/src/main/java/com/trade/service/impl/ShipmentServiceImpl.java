package com.trade.service.impl;

import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentStatusRequest;
import com.trade.entity.*;
import com.trade.exception.ResourceNotFoundException;
import com.trade.exception.UnauthorizedException;
import com.trade.repository.OrderRepository;
import com.trade.repository.ShipmentRepository;
import com.trade.repository.UserRepository;
import com.trade.service.ShipmentService;
import com.trade.util.MappingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles shipment queries and status updates for logistics partners.
 * Shipment creation is handled inside OrderServiceImpl when a request is accepted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
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
    public ShipmentResponse getShipmentById(Long id, String partnerEmail) {
        Shipment shipment = findShipmentById(id);
        verifyShipmentOwnership(shipment, partnerEmail);
        return MappingUtil.toShipmentResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long id, ShipmentStatusRequest request, String partnerEmail) {
        Shipment shipment = findShipmentById(id);
        verifyShipmentOwnership(shipment, partnerEmail);

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
        log.info("Shipment [id={}] status updated to [{}] by partner [{}]",
                id, request.getShipmentStatus(), partnerEmail);

        return MappingUtil.toShipmentResponse(shipment);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Shipment findShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void verifyShipmentOwnership(Shipment shipment, String partnerEmail) {
        if (!shipment.getLogisticsPartner().getEmail().equals(partnerEmail)) {
            throw new UnauthorizedException("You do not have permission to access this shipment");
        }
    }
}
