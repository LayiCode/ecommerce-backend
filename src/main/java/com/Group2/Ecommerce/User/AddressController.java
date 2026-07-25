package com.Group2.Ecommerce.User;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.User.Dto.AddressRequest;
import com.Group2.Ecommerce.User.Dto.AddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressResponse>> getMyAddresses() {
        return ApiResponse.success(addressService.getMyAddresses());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Address created", addressService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Address updated", addressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ApiResponse.success("Address deleted", null);
    }
}