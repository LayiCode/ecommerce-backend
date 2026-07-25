package com.Group2.Ecommerce.Order;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.Order.Dto.CheckoutRequest;
import com.Group2.Ecommerce.Order.Dto.OrderRequest;
import com.Group2.Ecommerce.Order.Dto.OrderResponse;
import com.Group2.Ecommerce.Order.Dto.OrderStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.success("Order created", orderService.createOrder(request));
    }

    // Places an order directly from the logged-in user's current cart.
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success("Order placed from cart", orderService.checkoutFromCart(request.getAddressId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(orderService.getMyOrders(pageable));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success("Order status updated", orderService.updateStatus(id, request.getStatus()));
    }
}