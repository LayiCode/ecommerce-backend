package com.Group2.Ecommerce.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findAll(Pageable pageable);
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Order o JOIN o.items oi " +
            "WHERE o.user.id = :userId AND oi.product.id = :productId " +
            "AND o.status = com.Group2.Ecommerce.Order.OrderStatus.DELIVERED")
    boolean hasDeliveredOrderForProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}