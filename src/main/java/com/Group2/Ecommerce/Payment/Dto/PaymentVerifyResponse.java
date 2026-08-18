package com.Group2.Ecommerce.Payment.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyResponse {

    private String status;
    private Long orderId;
}
