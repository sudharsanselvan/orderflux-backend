package com.orderflux.backend.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ProductRequest — Used for both CREATE and UPDATE operations.
 * One DTO for both keeps it simple at this stage.
 * Later we can split into CreateProductRequest / UpdateProductRequest
 * if they diverge significantly.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
	
	@NotBlank(message = "Product name is required")
	@Size(max=100, message="Name must not exceed 100 characters")
	private String name;
	
	@NotBlank(message="Category is required")
	@Size(max=50,message="Category must not exceed 50 characters")
	private String category;
	
	@Size(max=1000,message="Description must not exceed 1000 characters")
	private String description;
	
	@NotNull(message="Price is required")
	@DecimalMin(value="0.0",inclusive = false, message="Price must greater than zero (0)")
	@Digits(integer = 8,fraction = 2,message="Price must have at most 8 digits and 2 decimal places")
	private BigDecimal price;
	
	@NotNull(message="Stock quantity is required")
	@Min(value=0,message="Stock quantity cannot be negative")
	private Integer stockQuantity;
	
	@Size(max=500,message="Image URL must not exceed 500 characters")
	private String imageurl;
}
