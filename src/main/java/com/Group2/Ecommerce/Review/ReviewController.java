package com.Group2.Ecommerce.Review;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.Review.Dto.ReviewRequest;
import com.Group2.Ecommerce.Review.Dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<Page<ReviewResponse>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(reviewService.getByProduct(productId, pageable));
    }

    @GetMapping("/reviews/mine")
    public ApiResponse<ReviewResponse> getMine(@RequestParam Long productId) {
        return ApiResponse.success(reviewService.getMine(productId));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success("Review created", reviewService.create(request));
    }

    @PutMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> update(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success("Review updated", reviewService.update(id, request));
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ApiResponse.<Void>success("Review deleted", null);
    }
}
