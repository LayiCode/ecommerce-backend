package com.Group2.Ecommerce.Payment.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitializePaymentResponse {
    private String authorizationUrl; // frontend redirects the user here to pay
    private String reference;
}