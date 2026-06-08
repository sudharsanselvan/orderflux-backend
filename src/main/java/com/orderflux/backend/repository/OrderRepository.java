package com.orderflux.backend.repository;

import com.orderflux.backend.model.Order;
import com.orderflux.backend.model.User;
import com.orderflux.backend.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(
        value = """
                SELECT DISTINCT o FROM CustomerOrder o
                LEFT JOIN FETCH o.items i
                LEFT JOIN FETCH i.product
                WHERE o.user = :user
                """,
        countQuery = """
                SELECT COUNT(o) FROM CustomerOrder o
                WHERE o.user = :user
                """
    )
    Page<Order> findByUserOrderByCreatedAtDesc(
            @Param("user") User user,
            Pageable pageable
    );

    Optional<Order> findByIdAndUser(Long id, User user);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("""
            SELECT o FROM CustomerOrder o
            WHERE o.status = :status
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findByStatusOrderByCreatedAtDesc(
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    boolean existsByOrderNumber(String orderNumber);
}