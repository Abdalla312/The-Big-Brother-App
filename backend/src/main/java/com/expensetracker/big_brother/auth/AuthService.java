package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.AuthResponse;
import com.expensetracker.big_brother.auth.dto.LoginRequest;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import com.expensetracker.big_brother.auth.dto.ResendVerificationRequest;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import com.expensetracker.big_brother.verification.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    AuthService(JwtService jwtService,
                UserRepository userRepository,
                PasswordEncoder passwordEncoder,
                EmailVerificationService emailVerificationService,
                AuthenticationManager authManager) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.authManager = authManager;
    }

    // Register
    public AuthResponse register(@Valid RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setUserVerified(false);

        User savedUser = userRepository.save(user);

        emailVerificationService.sendVerificationEmail(savedUser);

        return AuthResponse
                .builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .verified(savedUser.isUserVerified())
                .message("Registration successful. Please verify your email before logging in.")
                .build();
    }

    // Login
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return AuthResponse.builder()
                .accessToken(token)
                .name(user.getName())
                .email(user.getEmail())
                .verified(user.isUserVerified())
                .message("Login Successful.")
                .build();
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
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(u -> !u.isUserVerified())
                .ifPresent(emailVerificationService::sendVerificationEmail);
    }
}

