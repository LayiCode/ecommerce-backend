package com.Group2.Ecommerce.Wishlist.Dto;

import com.Group2.Ecommerce.Wishlist.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private Integer stockQuantity;
    private LocalDateTime createdAt;

    public static WishlistItemResponse fromEntity(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getProduct().getImageUrl(),
                item.getProduct().getStockQuantity(),
                item.getCreatedAt()
        );
    }
}
