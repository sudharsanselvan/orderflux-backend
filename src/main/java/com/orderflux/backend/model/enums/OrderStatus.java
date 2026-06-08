package com.orderflux.backend.model.enums;

/**
 * OrderStatus — Lifecycle states of an order.
 *
 * State transition rules:
 *   PENDING    → CONFIRMED or CANCELLED
 *   CONFIRMED  → PROCESSING or CANCELLED
 *   PROCESSING → SHIPPED
 *   SHIPPED    → DELIVERED
 *   DELIVERED  → REFUNDED (if return requested)
 *   CANCELLED  → REFUNDED (if payment was made)
 *
 * Terminal states (no further transitions):
 **/
public enum OrderStatus {
	PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
