package com.Group2.Ecommerce.Payment.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitializePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;
}