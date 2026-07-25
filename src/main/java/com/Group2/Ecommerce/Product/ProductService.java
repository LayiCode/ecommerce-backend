package com.Group2.Ecommerce.Product;

import com.Group2.Ecommerce.Category.Category;
import com.Group2.Ecommerce.Category.CategoryService;
import com.Group2.Ecommerce.Common.Exception.OutOfStockException;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Product.Dto.ProductRequest;
import com.Group2.Ecommerce.Product.Dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public Page<ProductResponse> search(String name, Long categoryId, Pageable pageable) {
        String query = (name == null) ? "" : name;
        Page<Product> page;

        if (categoryId != null) {
            page = productRepository.findByNameContainingIgnoreCaseAndCategoryId(query, categoryId, pageable);
        } else {
            page = productRepository.findByNameContainingIgnoreCase(query, pageable);
        }

        return page.map(ProductResponse::fromEntity);
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.fromEntity(findEntityById(id));
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