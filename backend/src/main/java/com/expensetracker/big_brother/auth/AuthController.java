package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.AuthResponse;
import com.expensetracker.big_brother.auth.dto.LoginRequest;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import com.expensetracker.big_brother.auth.dto.ResendVerificationRequest;
import com.expensetracker.big_brother.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    //    POST /api/v1/auth/
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register (@RequestBody @Valid RegisterRequest request){
        AuthResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/auth/register")).body(response);
    }
    //    POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login (@RequestBody @Valid LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }
    //    GET  /api/v1/auth/verify-email
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam UUID userId,
            @RequestParam String token
    ) {
        authService.verifyEmail(userId,token);
        return  ResponseEntity.ok(Map.of("message", "Email verified successfully"));

    }
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification (@RequestBody @Valid ResendVerificationRequest request){
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse
                .ok(null,  "If the email is registered and unverified, a verification link has been sent."));
    }

}
