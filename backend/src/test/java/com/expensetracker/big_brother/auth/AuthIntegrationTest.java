package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.auth.dto.*;
import com.expensetracker.big_brother.mail.EmailService;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.verification.EmailVerificationRepository;
import com.expensetracker.big_brother.verification.EmailVerificationToken;
import com.expensetracker.big_brother.verification.TokenType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        clearDatabase();
        verificationRepository.deleteAll();
    }

    private String[] loginUser(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, "hashed");

        String response = performPost("/api/v1/auth/login", null, loginRequest)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.path("data").path("accessToken").asText();
        String refreshToken = json.path("data").path("refreshToken").asText();
        return new String[]{accessToken, refreshToken};
    }

    private User seedUnverifiedUser(String email, String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("hashed"));
        u.setRole(Role.USER);
        u.setUserVerified(false);
        return userRepository.save(u);
    }

    private void seedVerificationToken(User user, String rawToken) {
        String hash = computeHash(rawToken, user.getId());
        verificationRepository.save(new EmailVerificationToken(
                hash, user, null, LocalDateTime.now().plusHours(24)));
    }

    private void seedPasswordResetToken(User user, String rawToken) {
        String hash = computeHash(rawToken, user.getId());
        verificationRepository.save(new EmailVerificationToken(
                hash, user, null, LocalDateTime.now().plusMinutes(30), TokenType.PASSWORD_RESET));
    }

    private String computeHash(String token, UUID userId) {
        String value = userId + ":" + token;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void register_ValidRequest_Returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "Password123#");

        performPost("/api/v1/auth/register", null, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("A verification link has been sent"));
    }

    @Test
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "Password123#");

        performPost("/api/v1/auth/register", null, request)
                .andExpect(status().isCreated());

        performPost("/api/v1/auth/register", null, request)
                .andExpect(status().isConflict());
    }

    @Test
    void login_BeforeVerification_Returns403() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "Password123#");

        performPost("/api/v1/auth/register", null, request)
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("john@example.com", "password123");

        performPost("/api/v1/auth/login", null, loginRequest)
                .andExpect(status().isForbidden()); 
    }

    @Test
    void login_ValidRequest_Return200() throws Exception {
        User user = seedUser("auth-refresh@example.com", "Refresh User");
        String[] tokens = loginUser(user.getEmail());

        assertThat(tokens[1]).isNotNull();
        assertThat(tokens[1]).isNotEmpty();
    }

    @Test
    void refresh_ValidToken_Returns200() throws Exception {
        User user = seedUser("auth-refresh@example.com", "Refresh User");
        String[] tokens = loginUser(user.getEmail());

        RefreshRequest refreshRequest = new RefreshRequest(tokens[1]);

        performPost("/api/v1/auth/refresh", null, refreshRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_InvalidToken_Returns400() throws Exception {
        RefreshRequest request = new RefreshRequest("garbage-token-value");

        performPost("/api/v1/auth/refresh", null, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_ValidToken_Returns200() throws Exception {
        User user = seedUser("auth-logout@example.com", "Logout User");
        String[] tokens = loginUser(user.getEmail());

        RefreshRequest logoutRequest = new RefreshRequest(tokens[1]);

        performPost("/api/v1/auth/logout", null, logoutRequest)
                .andExpect(status().isOk());
    }

    @Test
    void logout_ThenRefreshFails_Returns400() throws Exception {
        User user = seedUser("auth-logout@example.com", "Logout User");
        String[] tokens = loginUser(user.getEmail());

        RefreshRequest logoutRequest = new RefreshRequest(tokens[1]);

        performPost("/api/v1/auth/logout", null, logoutRequest)
                .andExpect(status().isOk());
        RefreshRequest request = new RefreshRequest(tokens[1]);
        performPost("/api/v1/auth/refresh", null, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ValidToken_Returns200() throws Exception {
        User user = seedUnverifiedUser("test@example.com", "userA");
        String rawToken = "raw-token-abc";
        seedVerificationToken(user, rawToken);

        performGet("/api/v1/auth/verify-email?userId=" + user.getId() + "&token=" + rawToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
        assertThat(userRepository.findById(user.getId()).orElseThrow().isUserVerified()).isTrue();
    }

    @Test
    void verifyEmail_InvalidToken_Returns400() throws Exception {
        User user = seedUser("test@example.com", "userA");

        performGet("/api/v1/auth/verify-email?userId=" + user.getId() + "&token=garbage-token", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    void resendVerification_UnverifiedUser_Returns200() throws Exception {
        User user = seedUnverifiedUser("resend@example.com", "Resend User");
        ResendVerificationRequest request = new ResendVerificationRequest(user.getEmail());
        performPost("/api/v1/auth/resend-verification", null, request)
                .andExpect(status().isOk());
        verify(emailService).sendHtmlAsync(eq(user.getEmail()), eq("Verify your email"), anyString());
    }

    @Test
    void resendVerification_AlreadyVerified_Returns200() throws Exception {
        User user = seedUser("resend@example.com", "Resend User");
        ResendVerificationRequest request = new ResendVerificationRequest(user.getEmail());
        performPost("/api/v1/auth/resend-verification", null, request)
                .andExpect(status().isOk());
        verifyNoInteractions(emailService);
    }

    @Test
    void forgotPassword_ValidEmail_Returns200() throws Exception {
        User user = seedUser("forgot@example.com", "Forgot User");
        ForgotPasswordRequest request = new ForgotPasswordRequest(user.getEmail());
        performPost("/api/v1/auth/forgot-password", null, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If the email is registered, a reset link has been sent."));
        verify(emailService).sendHtmlAsync(eq(user.getEmail()), eq("Reset your password"), anyString());
    }

    @Test
    void forgotPassword_NonExistentEmail_Returns200() throws Exception {
        performPost("/api/v1/auth/forgot-password", null,
                new ForgotPasswordRequest("ghost@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If the email is registered, a reset link has been sent."));
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPassword_ValidToken_Returns200() throws Exception {
        User user = seedUser("reset@example.com", "Reset User");
        String rawToken = "reset-token-abc";
        seedPasswordResetToken(user, rawToken);

        performPost("/api/v1/auth/reset-password", null,
                new ResetPasswordRequest(rawToken, user.getId(), "Password123#"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));
        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("Password123#", refreshed.getPasswordHash())).isTrue();
        assertThat(refreshed.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void resetPassword_InvalidToken_Returns400() throws Exception {
        User user = seedUser("reset@example.com", "Reset User");
        performPost("/api/v1/auth/reset-password", null,
                new ResetPasswordRequest("garbage-token", user.getId(), "Password123#"))
                .andExpect(status().isBadRequest());
    }

}
