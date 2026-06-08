package com.orderflux.backend.controller;

import com.orderflux.backend.dto.request.ProductRequest;
import com.orderflux.backend.dto.response.PageResponse;
import com.orderflux.backend.dto.response.ProductResponse;
import com.orderflux.backend.service.ProductService;
import com.orderflux.backend.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name="Products",description = "Product catelog management and search")
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products
     *
     * Query parameters (all optional — defaults apply):
     *   page    = 0       (which page, 0-indexed)
     *   size    = 10      (items per page)
     *   sortBy  = createdAt (field to sort by)
     *   sortDir = desc    (asc or desc)
     *
     * Examples:
     *   /api/products                          → page 0, 10 items, newest first
     *   /api/products?page=1&size=5            → page 2, 5 items
     *   /api/products?sortBy=price&sortDir=asc → cheapest first
     *
     * @RequestParam(defaultValue = "..."):
     *   If parameter not provided in URL, use this default.
     *   Never returns null for these params.
     */
    @Operation(
    		summary="Get all active products",
    		description = "Return paginated list of active products. " + "Supports sorting by name, price, createdAt."
    		)
    @ApiResponses({
    		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",description = "Product fetched successfully")
    })
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)",example="0")
    		@RequestParam(defaultValue = "0")   int page,
    		@Parameter(description = "Item per page",example="10")
            @RequestParam(defaultValue = "10")  int size,
            @Parameter(description = "Sort field: name, price, createdAt",example="createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc",example="desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(
            ApiResponse.success("Products fetched successfully",
                productService.getAllActiveProducts(page, size, sortBy, sortDir))
        );
    }

    /**
     * GET /api/products/search
     *
     * Query parameters:
     *   name     = (optional) search by name keyword
     *   category = (optional) filter by category
     *   page, size, sortBy, sortDir — same as above
     *
     * Examples:
     *   /api/products/search?name=iphone
     *   /api/products/search?category=Mobile Phone
     *   /api/products/search?name=apple&category=Mobile&sortBy=price&sortDir=asc
     */
    @Operation(summary = "Search products by name and/or category")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
    		@Parameter(description = "Search keyword in product name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir) {

        return ResponseEntity.ok(
            ApiResponse.success("Search results fetched successfully",
                productService.searchProducts(
                    name, category, page, size, sortBy, sortDir))
        );
    }

    /**
     * GET /api/products/price-range
     *
     * Examples:
     *   /api/products/price-range?min=1000&max=50000
     */
    @Operation(summary = "Get products within price range")
    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getByPriceRange(
    		@Parameter(description = "Minimum price",example="1000")
            @RequestParam BigDecimal min,
            @Parameter(description = "Maximum price",example="50000")
            @RequestParam BigDecimal max,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            ApiResponse.success("Products fetched by price range",
                productService.getProductsByPriceRange(min, max, page, size))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success("Product fetched successfully",
                productService.getProductById(id))
        );
    }

    @Operation(summary = "Create a new product")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product name already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Token required")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully",
                    productService.createProduct(request)));
    }

    @Operation(summary = "Update an existing product")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success("Product updated successfully",
                productService.updateProduct(id, request))
        );
    }

    /**
     * DELETE returns 204 No Content — success with no body.
     * Why no body? The resource no longer exists.
     * Sending a body about a deleted thing is semantically odd.
     */
    @Operation(summary = "Soft delete a product")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Product deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}