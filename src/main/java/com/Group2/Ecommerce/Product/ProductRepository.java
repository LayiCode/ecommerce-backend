package com.Group2.Ecommerce.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search by name (case-insensitive) and optionally filter by category, paginated.
    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(
            String name, Long categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Atomic conditional decrement: only succeeds if enough stock exists.
     * Returns the number of rows updated (0 = insufficient stock / not found).
     * This single statement is what prevents the classic "two customers buy
     * the last unit at once" race condition.
     */
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decrementStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);
}