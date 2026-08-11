package com.Group2.Ecommerce.Product;

import com.Group2.Ecommerce.Category.Category;
import com.Group2.Ecommerce.Category.CategoryService;
import com.Group2.Ecommerce.Common.Exception.OutOfStockException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Product.Dto.ProductRequest;
import com.Group2.Ecommerce.Product.Dto.ProductResponse;
import com.Group2.Ecommerce.Review.ReviewRepository;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(1L);
        product.setName("Wireless Headphones");
        product.setPrice(new BigDecimal("89.99"));
        product.setStockQuantity(10);
        product.setCategory(category);
    }

    @Test
    void getById_returnsProduct_whenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Wireless Headphones");
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void getById_includesRatingAggregate_whenReviewsExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reviewRepository.aggregateByProductIds(Collections.singletonList(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L, 4.5d}));

        ProductResponse response = productService.getById(1L);

        assertThat(response.getRating()).isEqualByComparingTo("4.5");
        assertThat(response.getReviewCount()).isEqualTo(2);
    }

    @Test
    void search_includesRatingAggregates_forPageResults() {
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Speaker");
        product2.setPrice(new BigDecimal("49.99"));
        product2.setStockQuantity(5);
        product2.setCategory(category);

        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByNameContainingIgnoreCase("", pageable))
                .thenReturn(new PageImpl<>(List.of(product, product2), pageable, 2));
        when(reviewRepository.aggregateByProductIds(List.of(1L, 2L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L, 5.0d}, new Object[]{2L, 1L, 4.0d}));

        Page<ProductResponse> responses = productService.search(null, null, pageable);

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).getRating()).isEqualByComparingTo("5.0");
        assertThat(responses.getContent().get(0).getReviewCount()).isEqualTo(3);
        assertThat(responses.getContent().get(1).getRating()).isEqualByComparingTo("4.0");
        assertThat(responses.getContent().get(1).getReviewCount()).isEqualTo(1);
    }

    @Test
    void search_skipsAggregation_whenPageEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByNameContainingIgnoreCase("", pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        Page<ProductResponse> responses = productService.search(null, null, pageable);

        assertThat(responses.getContent()).isEmpty();
        verify(reviewRepository, never()).aggregateByProductIds(any());
    }

    @Test
    void getById_throwsResourceNotFound_whenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesProduct_withCategoryResolved() {
        ProductRequest request = new ProductRequest();
        request.setName("Wireless Headphones");
        request.setPrice(new BigDecimal("89.99"));
        request.setStockQuantity(10);
        request.setCategoryId(1L);

        when(categoryService.getById(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response.getName()).isEqualTo("Wireless Headphones");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void decrementStock_succeeds_whenEnoughStockAvailable() {
        // Simulates the atomic UPDATE affecting 1 row — meaning the
        // conditional (stock_quantity >= quantity) succeeded.
        when(productRepository.decrementStockIfAvailable(1L, 5)).thenReturn(1);

        productService.decrementStock(1L, 5);

        verify(productRepository).decrementStockIfAvailable(1L, 5);
    }

    @Test
    void decrementStock_throwsOutOfStock_whenInsufficientStock() {
        // Simulates the atomic UPDATE affecting 0 rows — the WHERE clause
        // (stock_quantity >= quantity) failed, meaning not enough stock.
        // This is the exact scenario that prevents the "two customers buy
        // the last unit" race condition.
        when(productRepository.decrementStockIfAvailable(1L, 999)).thenReturn(0);

        assertThatThrownBy(() -> productService.decrementStock(1L, 999))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("1");
    }

    @Test
    void delete_throwsResourceNotFound_whenProductDoesNotExist() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void delete_removesProduct_whenExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }
}