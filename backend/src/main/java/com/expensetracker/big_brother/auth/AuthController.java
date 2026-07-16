package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.*;
import com.expensetracker.big_brother.common.ApiResponse;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/auth/register")).body(ApiResponse.ok(response, "A verification link has been sent"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "success"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenService.RefreshResult>> refresh(@RequestBody @Valid RefreshRequest request) {
        RefreshTokenService.RefreshResult result = refreshTokenService.rotateRefreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(result, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid RefreshRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam UUID userId,
            @RequestParam String token) {
        authService.verifyEmail(userId,token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification (@RequestBody @Valid ResendVerificationRequest request){
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse
                .ok(null,  "If the email is registered and unverified, a verification link has been sent."));
    }

}
