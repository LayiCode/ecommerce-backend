package com.Group2.Ecommerce.Order.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.Group2.Ecommerce.Order.OrderStatus;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}