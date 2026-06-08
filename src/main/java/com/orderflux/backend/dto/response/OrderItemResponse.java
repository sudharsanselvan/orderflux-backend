package com.orderflux.backend.dto.response;

import com.orderflux.backend.model.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productCategory;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productCategory(item.getProduct().getCategory())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}