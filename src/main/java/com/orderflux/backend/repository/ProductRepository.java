package com.orderflux.backend.repository;

import com.orderflux.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Session 02 methods — keep these
    List<Product> findByIsActiveTrue();
    boolean existsByName(String name);
    Optional<Product> findByIdAndIsActiveTrue(Long id);

    /**
     * Paginated version of findByIsActiveTrue.
     *
     * Spring Data detects the Pageable parameter automatically.
     * Generates two SQL queries:
     *   1. SELECT * FROM products WHERE is_active=1 LIMIT ? OFFSET ?
     *   2. SELECT COUNT(*) FROM products WHERE is_active=1
     * The count query populates totalElements and totalPages.
     */
    Page<Product> findByIsActiveTrue(Pageable pageable);

    /**
     * Search by name containing keyword (case-insensitive).
     *
     * ContainingIgnoreCase generates:
     *   WHERE is_active=1 AND LOWER(name) LIKE LOWER('%keyword%')
     *
     * Also paginated — works the same way as above.
     */
    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(
            String name, Pageable pageable);

    /**
     * Filter by category (case-insensitive) with pagination.
     */
    Page<Product> findByCategoryIgnoreCaseAndIsActiveTrue(
            String category, Pageable pageable);

    /**
     * Combined search — name AND category filter together.
     *
     * Using @Query for clarity when derived query gets too long.
     * JPQL LOWER() = case-insensitive comparison.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
            AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
            """)
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("category") String category,
            Pageable pageable);

    /**
     * Filter by price range with pagination.
     * Between generates: WHERE price >= :min AND price <= :max
     */
    Page<Product> findByPriceBetweenAndIsActiveTrue(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);
}