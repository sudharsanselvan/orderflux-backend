package com.orderflux.backend.dto.response;

import com.orderflux.backend.model.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private Double ratings;
    private Integer reviewCount;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .ratings(product.getRatings())
                .reviewCount(product.getReviewCount())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}