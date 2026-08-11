package com.Group2.Ecommerce.Review;

import com.Group2.Ecommerce.Common.Exception.ForbiddenException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Order.OrderRepository;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.Review.Dto.ReviewRequest;
import com.Group2.Ecommerce.Review.Dto.ReviewResponse;
import com.Group2.Ecommerce.User.Role;
import com.Group2.Ecommerce.User.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private User otherUser;
    private Product product;
    private Review review;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setRole(Role.CUSTOMER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other User");
        otherUser.setRole(Role.CUSTOMER);

        product = new Product();
        product.setId(1L);
        product.setName("Wireless Headphones");

        review = new Review();
        review.setId(10L);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(5);
        review.setComment("Excellent");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ReviewRequest request(Integer rating, String comment) {
        ReviewRequest request = new ReviewRequest();
        request.setProductId(1L);
        request.setRating(rating);
        request.setComment(comment);
        return request;
    }

    @Test
    void create_savesReview_whenUserHasDeliveredOrder() {
        when(productService.findEntityById(1L)).thenReturn(product);
        when(orderRepository.hasDeliveredOrderForProduct(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.create(request(5, "Excellent"));

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Wireless Headphones");
        assertThat(response.getUserName()).isEqualTo("Test User");
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excellent");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void create_throwsForbidden_whenUserHasNoDeliveredOrder() {
        when(productService.findEntityById(1L)).thenReturn(product);
        when(orderRepository.hasDeliveredOrderForProduct(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(request(5, "Excellent")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("purchased and received");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_throwsIllegalState_whenUserAlreadyReviewed() {
        when(productService.findEntityById(1L)).thenReturn(product);
        when(orderRepository.hasDeliveredOrderForProduct(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.create(request(4, "Again")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void create_throwsResourceNotFound_whenProductMissing() {
        when(productService.findEntityById(1L))
                .thenThrow(new ResourceNotFoundException("Product not found: 1"));

        assertThatThrownBy(() -> reviewService.create(request(5, "Excellent")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_editsOwnReview() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.update(10L, request(3, "Meh"));

        assertThat(response.getRating()).isEqualTo(3);
        assertThat(response.getComment()).isEqualTo("Meh");
    }

    @Test
    void update_throwsForbidden_forAnotherUsersReview() {
        review.setUser(otherUser);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(10L, request(3, "Meh")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own reviews");
    }

    @Test
    void update_throwsResourceNotFound_whenMissing() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(99L, request(5, "Great")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesOwnReview() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        reviewService.delete(10L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void delete_throwsForbidden_forAnotherUsersReview() {
        review.setUser(otherUser);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(10L))
                .isInstanceOf(ForbiddenException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void getByProduct_returnsPage_whenProductExists() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.findEntityById(1L)).thenReturn(product);
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(review), pageable, 1));

        Page<ReviewResponse> responses = reviewService.getByProduct(1L, pageable);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(responses.getContent().get(0).getRating()).isEqualTo(5);
    }

    @Test
    void getMine_returnsOwnReview_whenExists() {
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getMine(1L);

        assertThat(response).isNotNull();
        assertThat(response.getUserName()).isEqualTo("Test User");
    }

    @Test
    void getMine_returnsNull_whenNoReview() {
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThat(reviewService.getMine(1L)).isNull();
    }
}
