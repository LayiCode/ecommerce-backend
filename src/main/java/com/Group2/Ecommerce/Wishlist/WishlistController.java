package com.Group2.Ecommerce.Wishlist;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.Wishlist.Dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<WishlistItemResponse>> getAll() {
        return ApiResponse.success(wishlistService.getAll());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> add(@PathVariable Long productId) {
        wishlistService.add(productId);
        return ApiResponse.success("Added to wishlist", null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        wishlistService.remove(productId);
        return ApiResponse.success("Removed from wishlist", null);
    }

    @GetMapping("/{productId}/check")
    public ApiResponse<Boolean> check(@PathVariable Long productId) {
        return ApiResponse.success(wishlistService.check(productId));
    }
}
