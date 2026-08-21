package com.trade.entity;

/**
 * Lifecycle states for a shipment.
 */
public enum ShipmentStatus {
    ASSIGNED,
    PICKED_UP,
    AT_EXPORT_CUSTOMS,
    IN_TRANSIT,
    AT_IMPORT_CUSTOMS,
    OUT_FOR_DELIVERY,
    DELIVERED
}
