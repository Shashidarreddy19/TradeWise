package com.trade.util;

import com.trade.dto.category.CategoryResponse;
import com.trade.dto.country.CountryResponse;
import com.trade.dto.order.OrderResponse;
import com.trade.dto.product.ProductResponse;
import com.trade.dto.proposal.ProposalResponse;
import com.trade.dto.shipment.ShipmentResponse;
import com.trade.dto.shipment.ShipmentTrackingResponse;
import com.trade.entity.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static utility methods for mapping JPA entities to response DTOs.
 * Keeps service code clean and avoids entity leakage to the API layer.
 */
public final class MappingUtil {

    private MappingUtil() {}

    public static CountryResponse toCountryResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .code(country.getCode())
                .name(country.getName())
                .currency(country.getCurrency())
                .build();
    }

    public static CategoryResponse toCategoryResponse(ProductCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .build();
    }

    public static ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .exporterId(product.getExporter().getId())
                .exporterName(product.getExporter().getName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getCategoryName())
                .name(product.getName())
                .hsCode(product.getHsCode())
                .description(product.getDescription())
                .material(product.getMaterial())
                .composition(product.getComposition())
                .function(product.getFunction())
                .manufacturingProcess(product.getManufacturingProcess())
                .physicalForm(product.getPhysicalForm())
                .specifications(product.getSpecifications())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .weight(product.getWeight())
                .build();
    }

    public static OrderResponse toOrderResponse(Order order) {
        String exporterCompany = null;
        if (order.getExporter().getExporterProfile() != null) {
            exporterCompany = order.getExporter().getExporterProfile().getCompanyName();
        }

        Long logisticsId = null;
        String logisticsName = null;
        String logisticsCompany = null;
        if (order.getAssignedLogisticsPartner() != null) {
            logisticsId = order.getAssignedLogisticsPartner().getId();
            logisticsName = order.getAssignedLogisticsPartner().getName();
            if (order.getAssignedLogisticsPartner().getLogisticsProfile() != null) {
                logisticsCompany = order.getAssignedLogisticsPartner()
                        .getLogisticsProfile().getCompanyName();
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .productId(order.getProduct().getId())
                .productName(order.getProduct().getName())
                .productHsCode(order.getProduct().getHsCode())
                .productWeightPerUnit(order.getProduct().getWeight())
                .exporterId(order.getExporter().getId())
                .exporterName(order.getExporter().getName())
                .exporterCompany(exporterCompany)
                .destinationCountryId(order.getDestinationCountry().getId())
                .destinationCountryName(order.getDestinationCountry().getName())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .pickupLocation(order.getPickupLocation())
                .shippingRequirements(order.getShippingRequirements())
                .specialInstructions(order.getSpecialInstructions())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .acceptedAt(order.getAcceptedAt())
                .assignedLogisticsPartnerId(logisticsId)
                .assignedLogisticsPartnerName(logisticsName)
                .assignedLogisticsPartnerCompany(logisticsCompany)
                .build();
    }

    public static ShipmentTrackingResponse toShipmentTrackingResponse(ShipmentTracking tracking) {
        return ShipmentTrackingResponse.builder()
                .id(tracking.getId())
                .shipmentId(tracking.getShipment().getId())
                .status(tracking.getStatus())
                .statusLabel(formatStatusLabel(tracking.getStatus()))
                .location(tracking.getLocation())
                .description(tracking.getDescription())
                .createdAt(tracking.getCreatedAt())
                .build();
    }

    public static ShipmentResponse toShipmentResponse(Shipment shipment) {
        String exporterCompany = null;
        if (shipment.getOrder().getExporter().getExporterProfile() != null) {
            exporterCompany = shipment.getOrder().getExporter()
                    .getExporterProfile().getCompanyName();
        }

        String logisticsCompany = null;
        if (shipment.getLogisticsPartner().getLogisticsProfile() != null) {
            logisticsCompany = shipment.getLogisticsPartner().getLogisticsProfile().getCompanyName();
        }

        List<ShipmentTrackingResponse> trackingList = Collections.emptyList();
        if (shipment.getTrackingEvents() != null) {
            trackingList = shipment.getTrackingEvents().stream()
                    .map(MappingUtil::toShipmentTrackingResponse)
                    .toList();
        }

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrder().getId())
                .orderProductName(shipment.getOrder().getProduct().getName())
                .orderDestinationCountry(shipment.getOrder().getDestinationCountry().getName())
                .orderQuantity(shipment.getOrder().getQuantity())
                .exporterId(shipment.getOrder().getExporter().getId())
                .exporterName(shipment.getOrder().getExporter().getName())
                .exporterCompany(exporterCompany)
                .logisticsPartnerId(shipment.getLogisticsPartner().getId())
                .logisticsPartnerName(shipment.getLogisticsPartner().getName())
                .logisticsPartnerCompany(logisticsCompany)
                .trackingNumber(shipment.getTrackingNumber())
                .shipmentStatus(shipment.getShipmentStatus())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .services(shipment.getServices())
                .cost(shipment.getCost())
                .currency(shipment.getCurrency())
                .pickupDate(shipment.getPickupDate())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .statusUpdatedAt(shipment.getStatusUpdatedAt())
                .createdAt(shipment.getCreatedAt())
                .trackingHistory(trackingList)
                .build();
    }

    public static ProposalResponse toProposalResponse(LogisticsProposal proposal) {
        String partnerCompany = null;
        String partnerExperience = null;
        String partnerServiceArea = null;
        Boolean tracking = false;
        Boolean insurance = false;

        LogisticsProfile profile = proposal.getLogisticsPartner().getLogisticsProfile();
        if (profile != null) {
            partnerCompany = profile.getCompanyName();
            partnerExperience = profile.getExperience();
            partnerServiceArea = profile.getServiceArea();
            tracking = profile.getTrackingSupport();
            insurance = profile.getCargoInsurance();
        }

        List<String> servicesList = Collections.emptyList();
        if (proposal.getServices() != null && !proposal.getServices().isBlank()) {
            servicesList = Arrays.stream(proposal.getServices().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        BigDecimal extra = proposal.getAdditionalCharges() != null ? proposal.getAdditionalCharges() : BigDecimal.ZERO;
        BigDecimal total = proposal.getEstimatedCost().add(extra);

        return ProposalResponse.builder()
                .id(proposal.getId())
                .orderId(proposal.getOrder().getId())
                .orderProductName(proposal.getOrder().getProduct().getName())
                .orderDestinationCountry(proposal.getOrder().getDestinationCountry().getName())
                .orderQuantity(proposal.getOrder().getQuantity())
                .logisticsPartnerId(proposal.getLogisticsPartner().getId())
                .logisticsPartnerName(proposal.getLogisticsPartner().getName())
                .logisticsPartnerCompany(partnerCompany)
                .logisticsPartnerPhone(proposal.getLogisticsPartner().getPhone())
                .logisticsPartnerEmail(proposal.getLogisticsPartner().getEmail())
                .logisticsPartnerExperience(partnerExperience)
                .logisticsPartnerServiceArea(partnerServiceArea)
                .trackingSupport(tracking)
                .cargoInsurance(insurance)
                .services(servicesList)
                .estimatedCost(proposal.getEstimatedCost())
                .currency(proposal.getCurrency())
                .estimatedTransitDays(proposal.getEstimatedTransitDays())
                .pickupDate(proposal.getPickupDate())
                .expectedDeliveryDate(proposal.getExpectedDeliveryDate())
                .additionalCharges(extra)
                .totalAmount(total)
                .notes(proposal.getNotes())
                .status(proposal.getStatus())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }

    private static String formatStatusLabel(ShipmentStatus status) {
        if (status == null) return "Unknown";
        switch (status) {
            case ASSIGNED: return "Booking Confirmed";
            case PICKED_UP: return "Cargo Picked Up";
            case AT_EXPORT_CUSTOMS: return "At Export Customs";
            case IN_TRANSIT: return "In Transit";
            case AT_IMPORT_CUSTOMS: return "At Import Customs";
            case OUT_FOR_DELIVERY: return "Out for Delivery";
            case DELIVERED: return "Delivered";
            default: return status.name();
        }
    }
}
