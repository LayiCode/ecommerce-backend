package com.Group2.Ecommerce.User.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    private String label;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Address line 1 is required")
    private String line1;

    private String line2;

    @NotBlank(message = "City is required")
    private String city;

    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country is too long")
    private String country;

    private String phone;

    private boolean isDefault;
}