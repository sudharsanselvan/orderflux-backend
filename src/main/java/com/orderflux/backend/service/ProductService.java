package com.orderflux.backend.service;

import com.orderflux.backend.dto.request.ProductRequest;
import com.orderflux.backend.dto.response.PageResponse;
import com.orderflux.backend.dto.response.ProductResponse;
import com.orderflux.backend.exception.DuplicateResourceException;
import com.orderflux.backend.exception.ResourceNotFoundException;
import com.orderflux.backend.model.Product;
import com.orderflux.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // ─── READ Operations ──────────────────────────────────────

    /**
     * Get all active products with pagination and sorting.
     *
     * @param page     page number (0-indexed, default 0)
     * @param size     items per page (default 10)
     * @param sortBy   field to sort by (default "createdAt")
     * @param sortDir  "asc" or "desc" (default "desc")
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllActiveProducts(
            int page, int size, String sortBy, String sortDir) {

        // Validate sortDir to prevent injection
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Build Sort object
        Sort sort = Sort.by(direction, sortBy);

        // Build Pageable — combines page, size, and sort
        Pageable pageable = PageRequest.of(page, size, sort);

        // Repository returns Page<Product>
        Page<Product> productPage =
                productRepository.findByIsActiveTrue(pageable);

        // Map Page<Product> → Page<ProductResponse>
        // Then wrap in our PageResponse
        return PageResponse.from(
                productPage.map(ProductResponse::from)
        );
    }

    // ─── Add search method ────────────────────────────────────

    /**
     * Search products by name and/or category with pagination.
     *
     * Both parameters are optional:
     *   name=null, category=null     → returns all active products
     *   name="phone", category=null  → filters by name only
     *   name=null, category="Mobile" → filters by category only
     *   name="apple", category="Mobile" → filters by both
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            String name,
            String category,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(direction, sortBy));

        // Pass null if empty string — @Query handles null as "ignore filter"
        String nameFilter = (name == null || name.isBlank()) ? null : name;
        String categoryFilter = (category == null || category.isBlank())
                ? null : category;

        Page<Product> productPage = productRepository.searchProducts(
                nameFilter, categoryFilter, pageable);

        log.info("Search results: {} products found", productPage.getTotalElements());

        return PageResponse.from(productPage.map(ProductResponse::from));
    }

    // ─── Add price range filter ───────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "price"));

        Page<Product> productPage =
                productRepository.findByPriceBetweenAndIsActiveTrue(
                        minPrice, maxPrice, pageable);

        return PageResponse.from(productPage.map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = findProductOrThrow(id);
        return ProductResponse.from(product);
    }

    // ─── WRITE Operations ─────────────────────────────────────

    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        // Check duplicate name
        if (productRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                "Product", "name", request.getName()
            );
        }

        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageurl())
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return ProductResponse.from(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = findProductOrThrow(id);

        // Check if new name conflicts with ANOTHER product
        // (allow same name if it's the same product being updated)
        if (!product.getName().equals(request.getName()) &&
             productRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                "Product", "name", request.getName()
            );
        }

        // Update fields explicitly — never use a mapper blindly
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageurl());

        // No need to call save() — entity is managed by Hibernate.
        // Any changes to a managed entity are auto-persisted
        // when the transaction commits. This is "dirty checking".
        return ProductResponse.from(product);
    }

    public void deleteProduct(Long id) {
        log.info("Soft deleting product with id: {}", id);

        Product product = findProductOrThrow(id);

        /**
         * SOFT DELETE — set isActive = false instead of DELETE FROM DB
         *
         * Why soft delete?
         *   - Orders reference products. Hard delete breaks order history.
         *   - You can restore products if deleted by mistake.
         *   - Audit trail is preserved.
         *
         * Real DELETE (hard delete) is almost never done in ecommerce.
         */
        product.setIsActive(false);
        // Again — no save() needed. Dirty checking handles it.

        log.info("Product id: {} soft deleted", id);
    }

    // ─── Private Helpers ──────────────────────────────────────

    /**
     * DRY helper — used by getById, update, delete.
     * Single place where "product not found" logic lives.
     */
    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Product", "id", id)
                );
    }
}