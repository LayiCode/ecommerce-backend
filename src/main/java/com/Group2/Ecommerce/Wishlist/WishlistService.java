package com.Group2.Ecommerce.Wishlist;

import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.User.User;
import com.Group2.Ecommerce.Wishlist.Dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;

    public List<WishlistItemResponse> getAll() {
        User currentUser = getCurrentUser();
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(WishlistItemResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void add(Long productId) {
        User currentUser = getCurrentUser();
        Product product = productService.findEntityById(productId);

        if (wishlistRepository.existsByUserIdAndProductId(currentUser.getId(), productId)) {
            return; // already wishlisted — idempotent
        }

        WishlistItem item = new WishlistItem();
        item.setUser(currentUser);
        item.setProduct(product);
        wishlistRepository.save(item);
    }

    @Transactional
    public void remove(Long productId) {
        User currentUser = getCurrentUser();
        wishlistRepository.deleteByUserIdAndProductId(currentUser.getId(), productId);
    }

    public boolean check(Long productId) {
        User currentUser = getCurrentUser();
        return wishlistRepository.existsByUserIdAndProductId(currentUser.getId(), productId);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
