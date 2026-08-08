package com.Group2.Ecommerce.Auth.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyResetCodeRequest {

    @NotBlank(message = "Reset code is required")
    private String code;
}
