package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.AuthResponse;
import com.expensetracker.big_brother.auth.dto.LoginRequest;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import com.expensetracker.big_brother.auth.dto.ResendVerificationRequest;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import com.expensetracker.big_brother.verification.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Register
    public AuthResponse register(@Valid RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setUserVerified(false);

        User savedUser = userRepository.save(user);

        emailVerificationService.sendVerificationEmail(savedUser);

        return new AuthResponse(
                null, null,
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.isUserVerified());
    }

    // Login
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.isUserVerified());
    }

    // verify email
    public void verifyEmail(UUID userId, String token) {
        try {
            emailVerificationService.verifyEmail(token, userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    public void resendVerification(ResendVerificationRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(u -> !u.isUserVerified())
                .ifPresent(emailVerificationService::sendVerificationEmail);
    }
}

