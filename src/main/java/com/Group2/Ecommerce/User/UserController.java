package com.Group2.Ecommerce.User;

import com.Group2.Ecommerce.Common.ApiResponse;
import com.Group2.Ecommerce.User.Dto.ChangePasswordRequest;
import com.Group2.Ecommerce.User.Dto.UpdateProfileRequest;
import com.Group2.Ecommerce.User.Dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ApiResponse.success(UserResponse.fromEntity(currentUser));
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        User updated = userService.updateProfile(currentUser, request);
        return ApiResponse.success("Profile updated", UserResponse.fromEntity(updated));
    }

    @PutMapping("/profile/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        userService.changePassword(currentUser, request);
        return ApiResponse.success("Password changed successfully", null);
    }
}
