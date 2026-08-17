package com.Group2.Ecommerce.Product.Dto;

import com.Group2.Ecommerce.Product.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private String colorName;
    private int sortOrder;

    public static ProductImageResponse fromEntity(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setColorName(image.getColorName());
        response.setSortOrder(image.getSortOrder());
        return response;
    }
}
