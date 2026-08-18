package com.Group2.Ecommerce.Review.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewSummary {

    private Page<ReviewResponse> content;
    private double averageRating;
    private long reviewCount;
    private boolean canReview;
    private ReviewResponse myReview;
}
