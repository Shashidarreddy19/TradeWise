package com.trade.service.impl;

import com.trade.dto.proposal.ProposalRequest;
import com.trade.dto.proposal.ProposalResponse;
import com.trade.entity.*;
import com.trade.exception.BadRequestException;
import com.trade.exception.ResourceNotFoundException;
import com.trade.exception.UnauthorizedException;
import com.trade.repository.*;
import com.trade.service.LogisticsProposalService;
import com.trade.util.MappingUtil;
import com.trade.util.TrackingNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsProposalServiceImpl implements LogisticsProposalService {

    private final LogisticsProposalRepository proposalRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;

    @Override
    @Transactional
    public ProposalResponse submitProposal(ProposalRequest request, String partnerEmail) {
        User partner = findUserByEmail(partnerEmail);
        if (partner.getRole() != Role.LOGISTICS) {
            throw new UnauthorizedException("Only registered logistics partners can submit proposals");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING_LOGISTICS) {
            throw new BadRequestException("Order #" + order.getId() + " is no longer available for quoting. Current status: " + order.getStatus());
        }

        // Check if this partner already submitted a proposal for this order
        Optional<LogisticsProposal> existingOpt = proposalRepository.findByOrderAndLogisticsPartner(order, partner);
        LogisticsProposal proposal;
        if (existingOpt.isPresent()) {
            proposal = existingOpt.get();
            if (proposal.getStatus() == ProposalStatus.ACCEPTED) {
                throw new BadRequestException("Your proposal for this order has already been accepted.");
            }
            // Update existing proposal
            proposal.setServices(String.join(", ", request.getServices()));
            proposal.setEstimatedCost(request.getEstimatedCost());
            proposal.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
            proposal.setEstimatedTransitDays(request.getEstimatedTransitDays());
            proposal.setPickupDate(request.getPickupDate());
            proposal.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
            proposal.setAdditionalCharges(request.getAdditionalCharges() != null ? request.getAdditionalCharges() : BigDecimal.ZERO);
            proposal.setNotes(request.getNotes());
            proposal.setStatus(ProposalStatus.PENDING);
            log.info("Logistics partner [{}] updated proposal [id={}] for order [{}]", partnerEmail, proposal.getId(), order.getId());
        } else {
            proposal = LogisticsProposal.builder()
                    .order(order)
                    .logisticsPartner(partner)
                    .services(String.join(", ", request.getServices()))
                    .estimatedCost(request.getEstimatedCost())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .estimatedTransitDays(request.getEstimatedTransitDays())
                    .pickupDate(request.getPickupDate())
                    .expectedDeliveryDate(request.getExpectedDeliveryDate())
                    .additionalCharges(request.getAdditionalCharges() != null ? request.getAdditionalCharges() : BigDecimal.ZERO)
                    .notes(request.getNotes())
                    .status(ProposalStatus.PENDING)
                    .build();
            log.info("Logistics partner [{}] created proposal for order [{}]", partnerEmail, order.getId());
        }

        proposal = proposalRepository.save(proposal);
        return MappingUtil.toProposalResponse(proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProposalResponse> getProposalsForOrder(Long orderId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // If user is exporter, they must own the order
        if (user.getRole() == Role.EXPORTER && !order.getExporter().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have permission to view proposals for this order");
        }

        List<LogisticsProposal> list;
        // If user is logistics partner, they only see their own proposal for this order
        if (user.getRole() == Role.LOGISTICS) {
            list = proposalRepository.findByOrderAndLogisticsPartner(order, user)
                    .map(List::of)
                    .orElseGet(List::of);
        } else {
            list = proposalRepository.findByOrderOrderByCreatedAtDesc(order);
        }

        return list.stream().map(MappingUtil::toProposalResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProposalResponse> getMyProposals(String partnerEmail) {
        User partner = findUserByEmail(partnerEmail);
        return proposalRepository.findByLogisticsPartnerOrderByCreatedAtDesc(partner)
                .stream()
                .map(MappingUtil::toProposalResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProposalResponse getProposalById(Long proposalId, String userEmail) {
        LogisticsProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));
        User user = findUserByEmail(userEmail);
        boolean isOwner = proposal.getLogisticsPartner().getId().equals(user.getId());
        boolean isExporter = proposal.getOrder().getExporter().getId().equals(user.getId());
        if (!isOwner && !isExporter) {
            throw new UnauthorizedException("You do not have permission to view this proposal");
        }
        return MappingUtil.toProposalResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse acceptProposal(Long proposalId, String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        LogisticsProposal acceptedProposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        Order order = acceptedProposal.getOrder();

        // Security check: caller must be the exporter who created this order
        if (!order.getExporter().getId().equals(exporter.getId())) {
            throw new UnauthorizedException("Only the exporter who created this order can accept proposals");
        }

        // Validate state
        if (order.getStatus() != OrderStatus.PENDING_LOGISTICS) {
            throw new BadRequestException("Order #" + order.getId() + " is already confirmed or processed.");
        }

        if (acceptedProposal.getStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException("Proposal is not in PENDING status. Current status: " + acceptedProposal.getStatus());
        }

        // 1. Mark accepted proposal
        acceptedProposal.setStatus(ProposalStatus.ACCEPTED);
        proposalRepository.save(acceptedProposal);

        // 2. Reject sibling pending proposals for this order
        List<LogisticsProposal> siblings = proposalRepository.findByOrder(order);
        for (LogisticsProposal p : siblings) {
            if (!p.getId().equals(acceptedProposal.getId()) && p.getStatus() == ProposalStatus.PENDING) {
                p.setStatus(ProposalStatus.REJECTED);
                proposalRepository.save(p);
            }
        }

        // 3. Update Order status
        order.setStatus(OrderStatus.LOGISTICS_ACCEPTED);
        order.setAssignedLogisticsPartner(acceptedProposal.getLogisticsPartner());
        order.setAcceptedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 4. Automatically create Shipment
        String trackingNum = generateUniqueTrackingNumber();
        BigDecimal totalCost = acceptedProposal.getEstimatedCost().add(
                acceptedProposal.getAdditionalCharges() != null ? acceptedProposal.getAdditionalCharges() : BigDecimal.ZERO
        );

        Shipment shipment = Shipment.builder()
                .order(order)
                .logisticsPartner(acceptedProposal.getLogisticsPartner())
                .proposal(acceptedProposal)
                .trackingNumber(trackingNum)
                .shipmentStatus(ShipmentStatus.ASSIGNED)
                .origin(order.getPickupLocation())
                .destination(order.getDestinationCountry().getName())
                .services(acceptedProposal.getServices())
                .cost(totalCost)
                .currency(acceptedProposal.getCurrency())
                .pickupDate(acceptedProposal.getPickupDate())
                .estimatedDelivery(acceptedProposal.getExpectedDeliveryDate())
                .statusUpdatedAt(LocalDateTime.now())
                .trackingEvents(new ArrayList<>())
                .build();

        shipment = shipmentRepository.save(shipment);

        // 5. Create initial ShipmentTracking event
        String partnerName = acceptedProposal.getLogisticsPartner().getLogisticsProfile() != null
                && acceptedProposal.getLogisticsPartner().getLogisticsProfile().getCompanyName() != null
                ? acceptedProposal.getLogisticsPartner().getLogisticsProfile().getCompanyName()
                : acceptedProposal.getLogisticsPartner().getName();

        ShipmentTracking initialTracking = ShipmentTracking.builder()
                .shipment(shipment)
                .status(ShipmentStatus.ASSIGNED)
                .location(order.getPickupLocation())
                .description("Shipment booked and assigned to logistics partner " + partnerName)
                .build();

        trackingRepository.save(initialTracking);

        log.info("Order [{}] proposal [{}] accepted by exporter [{}]. Auto-created shipment [{}] with tracking [{}]",
                order.getId(), acceptedProposal.getId(), exporterEmail, shipment.getId(), trackingNum);

        return MappingUtil.toProposalResponse(acceptedProposal);
    }

    @Override
    @Transactional
    public ProposalResponse rejectProposal(Long proposalId, String exporterEmail) {
        User exporter = findUserByEmail(exporterEmail);
        LogisticsProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", "id", proposalId));

        Order order = proposal.getOrder();
        if (!order.getExporter().getId().equals(exporter.getId())) {
            throw new UnauthorizedException("Only the order owner can reject proposals");
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new BadRequestException("Only PENDING proposals can be rejected. Current status: " + proposal.getStatus());
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal = proposalRepository.save(proposal);
        log.info("Proposal [{}] rejected by exporter [{}] for order [{}]", proposal.getId(), exporterEmail, order.getId());

        return MappingUtil.toProposalResponse(proposal);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String generateUniqueTrackingNumber() {
        String num;
        do {
            num = TrackingNumberUtil.generate();
        } while (shipmentRepository.existsByTrackingNumber(num));
        return num;
    }
}
