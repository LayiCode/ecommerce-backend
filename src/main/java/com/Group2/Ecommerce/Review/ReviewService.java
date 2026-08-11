package com.Group2.Ecommerce.Review;

import com.Group2.Ecommerce.Common.Exception.ForbiddenException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Order.OrderRepository;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.Review.Dto.ReviewRequest;
import com.Group2.Ecommerce.Review.Dto.ReviewResponse;
import com.Group2.Ecommerce.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;

    @Transactional
    public ReviewResponse create(ReviewRequest request) {
        User currentUser = getCurrentUser();
        Product product = productService.findEntityById(request.getProductId());

        if (!orderRepository.hasDeliveredOrderForProduct(currentUser.getId(), product.getId())) {
            throw new ForbiddenException("You can only review products you have purchased and received");
        }

        if (reviewRepository.findByProductIdAndUserId(product.getId(), currentUser.getId()).isPresent()) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(currentUser);
        applyRequest(review, request);

        return ReviewResponse.fromEntity(reviewRepository.save(review));
    }

    public Page<ReviewResponse> getByProduct(Long productId, Pageable pageable) {
        productService.findEntityById(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    public ReviewResponse getMine(Long productId) {
        User currentUser = getCurrentUser();
        Optional<Review> review = reviewRepository.findByProductIdAndUserId(productId, currentUser.getId());
        return review.map(ReviewResponse::fromEntity).orElse(null);
    }

    @Transactional
    public ReviewResponse update(Long id, ReviewRequest request) {
        Review review = findOwned(id);
        applyRequest(review, request);
        return ReviewResponse.fromEntity(reviewRepository.save(review));
    }

    @Transactional
    public void delete(Long id) {
        Review review = findOwned(id);
        reviewRepository.delete(review);
    }

    private Review findOwned(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        if (!review.getUser().getId().equals(getCurrentUser().getId())) {
            throw new ForbiddenException("You can only modify your own reviews");
        }
        return review;
    }

    private void applyRequest(Review review, ReviewRequest request) {
        review.setRating(request.getRating());
        review.setComment(request.getComment());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
