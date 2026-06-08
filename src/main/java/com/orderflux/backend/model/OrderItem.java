package com.orderflux.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderItem — One line item within an Order.
 *
 * Does NOT extend BaseEntity:
 *   Items don't need audit timestamps independently.
 *   The parent Order's createdAt covers when the item was created.
 *   Keeping it lean — only necessary columns.
 *
 * unitPrice — snapshot of product price at order time:
 *   Product price may change after order is placed.
 *   We store the price the customer actually paid.
 *   This field is IMMUTABLE after creation.
 *
 * totalPrice = quantity × unitPrice:
 *   Stored (not computed) for query efficiency.
 *   Avoids multiplication on every read.
 *   Set once in OrderService, never changed.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @ManyToOne to Order:
     *   Many items belong to one order.
     *   This is the "owning side" of the relationship
     *   (has the foreign key column order_id).
     *   LAZY: don't load the full Order when loading an item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * @ManyToOne to Product:
     *   Many order items can reference one product.
     *   LAZY: don't auto-load Product.
     *   We'll JOIN FETCH when we need product details.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price snapshot — immutable after creation.
     * This is what the customer paid per unit.
     */
    @Column(name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal unitPrice;

    /**
     * Pre-computed total = quantity × unitPrice.
     * Stored for efficient order total calculation.
     */
    @Column(name = "total_price",
            nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal totalPrice;
}