package com.orderflux.backend.dto.response;

import com.orderflux.backend.model.Order;
import com.orderflux.backend.model.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String notes;

    /**
     * Include basic user info — not full UserResponse.
     * Order confirmation needs to show who placed it.
     * We don't expose full user object — just what's needed.
     */
    private Long userId;
    private String userEmail;

    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .items(order.getItems()
                        .stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}