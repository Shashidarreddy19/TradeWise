package com.trade.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for creating an Export Shipment Request.
 * Only Exporters can submit this.
 */
@Data
public class OrderRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Destination country ID is required")
    private Long destinationCountryId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    /**
     * Preferred shipping mode: Sea Freight, Air Freight, Road, Rail
     */
    private String shippingRequirements;

    /**
     * Any additional notes or special handling instructions.
     */
    private String specialInstructions;
}
