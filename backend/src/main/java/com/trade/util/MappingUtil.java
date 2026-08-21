package com.trade.util;

import com.trade.dto.category.CategoryResponse;
import com.trade.dto.country.CountryResponse;
import com.trade.dto.order.OrderResponse;
import com.trade.dto.product.ProductResponse;
import com.trade.dto.shipment.ShipmentResponse;
import com.trade.entity.*;

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
        // Resolve exporter company name safely
        String exporterCompany = null;
        if (order.getExporter().getExporterProfile() != null) {
            exporterCompany = order.getExporter().getExporterProfile().getCompanyName();
        }

        // Resolve assigned logistics partner details safely
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

    public static ShipmentResponse toShipmentResponse(Shipment shipment) {
        // Resolve exporter company safely
        String exporterCompany = null;
        if (shipment.getOrder().getExporter().getExporterProfile() != null) {
            exporterCompany = shipment.getOrder().getExporter()
                    .getExporterProfile().getCompanyName();
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
                .trackingNumber(shipment.getTrackingNumber())
                .shipmentStatus(shipment.getShipmentStatus())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .statusUpdatedAt(shipment.getStatusUpdatedAt())
                .createdAt(shipment.getCreatedAt())
                .build();
    }
}
