package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.dto.ChangePasswordRequest;
import com.expensetracker.big_brother.user.dto.DeleteAccountRequest;
import com.expensetracker.big_brother.user.dto.UpdateProfileRequest;
import com.expensetracker.big_brother.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfiles(
            @AuthenticationPrincipal CustomUserDetails user) {
        UserResponse response = userService.getProfile(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile retrieved"));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody @Valid UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        UserResponse response = userService.updateProfile(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile updated"));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        userService.changePassword(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password updated"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestBody @Valid DeleteAccountRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        userService.deleteAccount(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Account Deleted"));
    }

}
