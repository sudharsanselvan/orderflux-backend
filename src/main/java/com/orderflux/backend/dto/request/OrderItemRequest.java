package com.orderflux.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OrderItemRequest — One product line in an order.
 *
 * Customer sends:
 *   productId → which product to order
 *   quantity  → how many units
 *
 * We look up product details (name, price) from DB.
 * Customer cannot send price — we never trust client-sent prices.
 * Always fetch price from DB at order time.
 *
 * @Min(1): quantity must be at least 1.
 *   0 or negative quantity makes no business sense.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}