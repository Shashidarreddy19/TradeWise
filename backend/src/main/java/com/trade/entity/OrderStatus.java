package com.trade.entity;

/**
 * Lifecycle states for an Export Shipment Request.
 *
 * PENDING_LOGISTICS  — Exporter created the request; awaiting a logistics partner to accept.
 * LOGISTICS_ACCEPTED — A logistics partner accepted; shipment auto-created.
 * IN_TRANSIT         — Shipment is actively moving.
 * DELIVERED          — Shipment delivered to destination.
 * REJECTED           — Every logistics partner rejected; no one available.
 */
public enum OrderStatus {
    PENDING_LOGISTICS,
    LOGISTICS_ACCEPTED,
    IN_TRANSIT,
    DELIVERED,
    REJECTED
}
