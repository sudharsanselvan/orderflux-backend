package com.orderflux.backend.model;

import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.orderflux.backend.model.base.BaseEntity;
import com.orderflux.backend.model.enums.OrderStatus;

import jakarta.persistence.*;
import lombok.*;

/**
 * Order — Represents a customer's purchase.
 *
 * Key design decisions:
 *
 * 1. orderNumber:
 *    Human-readable identifier shown to customers.
 *    "ORD-00001" is more user-friendly than id=1.
 *    Generated in OrderService, not here.
 *
 * 2. totalAmount:
 *    Denormalized sum of all OrderItem.totalPrice values.
 *    Could be computed dynamically, but storing it avoids
 *    recalculating on every fetch.
 *    Updated when items change.
 *
 * 3. CascadeType.ALL on items:
 *    OrderItems cannot exist without an Order.
 *    Saving Order automatically saves all its items.
 *    Deleting Order automatically deletes all its items.
 *
 * 4. orphanRemoval = true:
 *    If an item is removed from the items list,
 *    it's automatically deleted from DB.
 *    Without this, removed items become orphaned rows.
 *
 * 5. @Builder.Default on items:
 *    Without this, items = null when built via builder.
 *    new ArrayList<>() ensures we can call .add() safely.
 */
@Entity(name = "CustomerOrder")    // ← JPQL name, avoids ORDER conflict
@Table(name = "orders")            // ← DB table name, unchanged
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
     * @ManyToOne LAZY:
     *   Don't load User when loading Order.
     *   We only need user_id for most operations.
     *   Access user data explicitly when needed.
     */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false)
	private User user;
	
	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;
	
	@Enumerated(EnumType.STRING)
	@Column(name="status",nullable=false,length=20)
	private OrderStatus status;
	
	@Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;
	
	@Column(name = "shipping_address", nullable = false, length = 500)
	private String shippingAddress;
	
	@Column(name = "notes", length = 500)
	private String notes;
	
	/**
     * @OneToMany:
     *   mappedBy = "order" → OrderItem.order field owns the relationship
     *   cascade = ALL      → save/delete propagates to items
     *   orphanRemoval      → removed items deleted from DB
     *   fetch = LAZY       → items NOT loaded unless accessed
     *
     * @Builder.Default:
     *   Initializes to empty list when using builder.
     *   Prevents NullPointerException on order.getItems().add()
     */
	
	@OneToMany(
	        mappedBy = "order",
	        cascade = CascadeType.ALL,
	        orphanRemoval = true,
	        fetch = FetchType.LAZY
	    )
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();
}
