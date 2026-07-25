package com.Group2.Ecommerce.Cart;

import com.Group2.Ecommerce.Cart.Dto.AddCartItemRequest;
import com.Group2.Ecommerce.Cart.Dto.CartResponse;
import com.Group2.Ecommerce.Cart.Dto.UpdateCartItemRequest;
import com.Group2.Ecommerce.Common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getMyCart() {
        return ApiResponse.success(cartService.getMyCart());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success("Item added to cart", cartService.addItem(request.getProductId(), request.getQuantity()));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success("Cart item updated", cartService.updateItem(itemId, request.getQuantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(itemId);
        return ApiResponse.success("Item removed from cart", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.success("Cart cleared", null);
    }
}