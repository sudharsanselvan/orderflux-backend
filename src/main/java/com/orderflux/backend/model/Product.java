package com.orderflux.backend.model;

import com.orderflux.backend.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")   // plural — consistent with "users" table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {   // ← extends BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // TODO: Replace with @ManyToOne Category entity in Session 04
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", length = 1000)
    private String description;

    // precision=10, scale=2 → up to 99999999.99
    // BigDecimal: exact decimal arithmetic, no floating-point imprecision
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default                           // ← @Builder.Default
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;         // ← camelCase field + default

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default                           // ← @Builder.Default
    @Column(name = "ratings", nullable = false)
    private Double ratings = 0.0;

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;           // ← renamed from no_of_reviews

    @Builder.Default                           // ← @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}