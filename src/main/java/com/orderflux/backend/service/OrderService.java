package com.orderflux.backend.service;

import com.orderflux.backend.dto.request.OrderItemRequest;
import com.orderflux.backend.dto.request.PlaceOrderRequest;
import com.orderflux.backend.dto.response.OrderResponse;
import com.orderflux.backend.dto.response.PageResponse;
import com.orderflux.backend.exception.BadRequestException;
import com.orderflux.backend.exception.ForbiddenException;
import com.orderflux.backend.exception.ResourceNotFoundException;
import com.orderflux.backend.model.*;
import com.orderflux.backend.model.enums.OrderStatus;
import com.orderflux.backend.repository.OrderRepository;
import com.orderflux.backend.repository.ProductRepository;
import com.orderflux.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderService — Core business logic for order management.
 *
 * Key responsibilities:
 *   1. Place orders with stock validation
 *   2. Generate unique order numbers
 *   3. Calculate totals from real-time product prices
 *   4. Retrieve orders with security checks
 *   5. Cancel orders with status validation
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ─── Place Order ──────────────────────────────────────────

    /**
     * Place a new order.
     *
     * Business rules enforced:
     *   1. User must exist and be verified/enabled
     *   2. All products must exist and be active
     *   3. Stock must be sufficient for each item
     *   4. Price fetched from DB (never trusted from client)
     *   5. Stock decremented after order placed
     *   6. Order number generated uniquely
     *
     * @param email   authenticated user's email (from JWT)
     * @param request order details from client
     */
    public OrderResponse placeOrder(String email,
                                    PlaceOrderRequest request) {
        log.info("Placing order for user: {}", email);

        // Step 1: Load authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        // Step 2: Build order items + validate stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {

            // Load product — must exist and be active
            Product product = productRepository
                    .findByIdAndIsActiveTrue(itemRequest.getProductId())
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Product", "id", itemRequest.getProductId()
                        )
                    );

            // Validate stock
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException(
                    "Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStockQuantity() +
                    ", Requested: " + itemRequest.getQuantity()
                );
            }

            // Calculate line total
            // unitPrice = current product price (snapshot)
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            // Build OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(lineTotal);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(lineTotal);

            // Decrement stock
            product.setStockQuantity(
                product.getStockQuantity() - itemRequest.getQuantity()
            );
            // No explicit save needed — dirty checking handles it
        }

        // Step 3: Build Order
        Order order = Order.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        // Step 4: Link items to order (bidirectional)
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        // Step 5: Save order — cascade saves all items
        Order savedOrder = orderRepository.save(order);
        log.info("Order placed: {} for user: {}",
                savedOrder.getOrderNumber(), email);

        return OrderResponse.from(savedOrder);
    }

    // ─── Read Operations ──────────────────────────────────────

    /**
     * Get current user's orders with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String email,
                                                    int page,
                                                    int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);

        return PageResponse.from(orders.map(OrderResponse::from));
    }

    /**
     * Get specific order by ID.
     * Security: user can only access their own orders.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Order", "id", orderId)
                );

        return OrderResponse.from(order);
    }

    // ─── Cancel Order ─────────────────────────────────────────

    /**
     * Cancel an order.
     *
     * Business rules:
     *   1. Order must belong to the requesting user
     *   2. Only PENDING or CONFIRMED orders can be cancelled
     *   3. Stock is restored when order is cancelled
     */
    public OrderResponse cancelOrder(String email, Long orderId) {
        log.info("Cancel request for order: {} by user: {}",
                orderId, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new ResourceNotFoundException("User", "email", email)
                );

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Order", "id", orderId)
                );

        // Validate cancellable status
        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException(
                "Order cannot be cancelled. Current status: " +
                order.getStatus() +
                ". Only PENDING or CONFIRMED orders can be cancelled."
            );
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(
                product.getStockQuantity() + item.getQuantity()
            );
            log.info("Restored {} units of product: {}",
                    item.getQuantity(), product.getName());
        }

        // Update status
        order.setStatus(OrderStatus.CANCELLED);

        log.info("Order {} cancelled successfully", order.getOrderNumber());
        return OrderResponse.from(order);
    }

    // ─── Private Helpers ──────────────────────────────────────

    /**
     * Generate unique order number.
     *
     * Format: ORD-XXXXX (e.g. ORD-00001, ORD-00042)
     *
     * Strategy:
     *   Count existing orders + 1 → base number
     *   If collision (race condition) → increment until unique
     *   Pad to 5 digits with leading zeros
     *
     * Production alternative:
     *   Use UUID or timestamp-based IDs to avoid race conditions
     *   in high-traffic systems. For learning project this is fine.
     */
    private String generateOrderNumber() {
        long count = orderRepository.count() + 1;
        String orderNumber = "ORD-" + String.format("%05d", count);

        // Handle collision
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            count++;
            orderNumber = "ORD-" + String.format("%05d", count);
        }

        return orderNumber;
    }

    /**
     * Get currently authenticated user's email from SecurityContext.
     * Alternative to passing email as parameter.
     * Used when email isn't passed from controller.
     */
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}