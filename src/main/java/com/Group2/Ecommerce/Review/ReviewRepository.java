package com.Group2.Ecommerce.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT r.product.id AS productId, COUNT(r) AS cnt, AVG(r.rating) AS avgRating " +
            "FROM Review r WHERE r.product.id IN :ids GROUP BY r.product.id")
    List<Object[]> aggregateByProductIds(@Param("ids") Collection<Long> productIds);
}
