package com.Group2.Ecommerce.Product;

import com.Group2.Ecommerce.Category.Category;
import com.Group2.Ecommerce.Category.CategoryService;
import com.Group2.Ecommerce.Common.Exception.OutOfStockException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Product.Dto.ProductRequest;
import com.Group2.Ecommerce.Product.Dto.ProductResponse;
import com.Group2.Ecommerce.Review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ReviewRepository reviewRepository;

    public Page<ProductResponse> search(String name, Long categoryId, Pageable pageable) {
        String query = (name == null) ? "" : name;
        Page<Product> page;

        if (categoryId != null) {
            page = productRepository.findByNameContainingIgnoreCaseAndCategoryId(query, categoryId, pageable);
        } else {
            page = productRepository.findByNameContainingIgnoreCase(query, pageable);
        }

        if (page.isEmpty()) {
            return page.map(ProductResponse::fromEntity);
        }

        List<Long> ids = page.getContent().stream().map(Product::getId).toList();
        Map<Long, Object[]> aggregates = aggregateRatings(ids);
        return page.map(product -> {
            ProductResponse response = ProductResponse.fromEntity(product);
            applyRating(response, aggregates.get(product.getId()));
            return response;
        });
    }

    public ProductResponse getById(Long id) {
        ProductResponse response = ProductResponse.fromEntity(findEntityById(id));
        Map<Long, Object[]> aggregates = aggregateRatings(Collections.singletonList(id));
        applyRating(response, aggregates.get(id));
        return response;
    }

    private Map<Long, Object[]> aggregateRatings(List<Long> productIds) {
        return reviewRepository.aggregateByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row,
                        (a, b) -> a
                ));
    }

    private void applyRating(ProductResponse response, Object[] aggregate) {
        if (aggregate == null) {
            response.setRating(null);
            response.setReviewCount(0);
            return;
        }
        long count = ((Number) aggregate[1]).longValue();
        double avg = ((Number) aggregate[2]).doubleValue();
        response.setReviewCount(count);
        response.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
    }

   public Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryService.getById(request.getCategoryId());

        Product product = new Product();
        applyRequest(product, request, category);

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntityById(id);
        Category category = categoryService.getById(request.getCategoryId());
        applyRequest(product, request, category);

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Atomically decrements stock, guarding against overselling under concurrent requests.
     * Call this inside the caller's own @Transactional boundary (e.g. order creation).
     */
    @Transactional
    public void decrementStock(Long productId, int quantity) {
        int updatedRows = productRepository.decrementStockIfAvailable(productId, quantity);
        if (updatedRows == 0) {
            throw new OutOfStockException("Insufficient stock for product: " + productId);
        }
    }

    private void applyRequest(Product product, ProductRequest request, Category category) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
    }
}