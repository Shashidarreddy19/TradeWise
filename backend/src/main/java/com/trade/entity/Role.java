package com.trade.entity;

/**
 * Enum representing user roles in the Trade platform.
 * All DB values must be represented here to avoid Hibernate deserialization errors.
 */
public enum Role {
    EXPORTER,
    LOGISTICS,
    /** DB legacy value stored as LOGISTICS_PARTNER — treated same as LOGISTICS */
    LOGISTICS_PARTNER,
    IMPORTER,
    ADMIN
}
