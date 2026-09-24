package com.trade.dto.shipment;

import com.trade.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for updating the status of an existing shipment,
 * recording location and description for the tracking history timeline.
 */
@Data
public class ShipmentStatusRequest {

    @NotNull(message = "Shipment status is required")
    private ShipmentStatus shipmentStatus;

    // Optional location of current update (e.g. "JNPT Port, Navi Mumbai")
    private String location;

    // Optional note / remarks for this status update
    private String description;
}
