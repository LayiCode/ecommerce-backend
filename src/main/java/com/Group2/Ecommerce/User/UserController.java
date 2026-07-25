package com.Group2.Ecommerce.User;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.User.Dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ApiResponse.success(UserResponse.fromEntity(currentUser));
    }
}