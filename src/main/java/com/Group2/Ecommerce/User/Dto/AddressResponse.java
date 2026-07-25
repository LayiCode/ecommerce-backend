package com.Group2.Ecommerce.User.Dto;

import com.Group2.Ecommerce.User.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String label;
    private String fullName;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private boolean isDefault;

    public static AddressResponse fromEntity(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getFullName(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getPhone(),
                address.isDefault()
        );
    }
}