package com.Group2.Ecommerce.Cart;

import com.Group2.Ecommerce.Cart.Dto.CartItemResponse;
import com.Group2.Ecommerce.Cart.Dto.CartResponse;
import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.Product.Product;
import com.Group2.Ecommerce.Product.ProductService;
import com.Group2.Ecommerce.User.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public CartResponse getMyCart() {
        User currentUser = getCurrentUser();
        List<CartItem> items = cartItemRepository.findByUserId(currentUser.getId());
        return buildResponse(items);
    }

    @Transactional
    public CartResponse addItem(Long productId, int quantity) {
        User currentUser = getCurrentUser();
        Product product = productService.findEntityById(productId);

        // If the product's already in the cart, bump the quantity instead
        // of creating a duplicate row (enforced by the unique constraint
        // on user_id + product_id at the DB level too).
        CartItem item = cartItemRepository.findByUserIdAndProductId(currentUser.getId(), productId)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(currentUser);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + quantity);
        cartItemRepository.save(item);

        return getMyCart();
    }

    @Transactional
    public CartResponse updateItem(Long itemId, int quantity) {
        CartItem item = findOwnedItem(itemId);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return getMyCart();
    }

    @Transactional
    public void removeItem(Long itemId) {
        CartItem item = findOwnedItem(itemId);
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart() {
        User currentUser = getCurrentUser();
        List<CartItem> items = cartItemRepository.findByUserId(currentUser.getId());
        cartItemRepository.deleteAll(items);
    }

    // Same pattern as address ownership — return 404, not 403, so a user
    // can't tell whether an item ID belongs to someone else vs. not existing.
    private CartItem findOwnedItem(Long itemId) {
        User currentUser = getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        if (!item.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }
        return item;
    }

    private CartResponse buildResponse(List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(itemResponses, total);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}