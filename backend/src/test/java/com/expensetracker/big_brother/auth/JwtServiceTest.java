package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JwtServiceTest {
    private static final String SECRET_KEY = Base64.getEncoder()
            .encodeToString("super-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha!".getBytes());
    private static final long EXPIRY_MS = 900_000L;

    private JwtService jwtService;
    private CustomUserDetails customUserDetails;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", EXPIRY_MS);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("hashedPassword");
        user.setRole(Role.USER);
        user.setTokenVersion(1);

        customUserDetails = new CustomUserDetails(user);
    }

    @Test
    void generateToken_Success() {
        String token = jwtService.generateToken(customUserDetails);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUserName(token)).isEqualTo("john@example.com");
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(customUserDetails);

        boolean isValid = jwtService.isTokenValid(token, customUserDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsTrue() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", -1000L);
        String token = jwtService.generateToken(customUserDetails);

        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", -1000L);
        String token = jwtService.generateToken(customUserDetails);

        assertThat(jwtService.isTokenValid(token, customUserDetails)).isFalse();
    }

    @Test
    void isTokenValid_TokenVersionMismatch_ReturnsFalse() {
        String token = jwtService.generateToken(customUserDetails);

        user.setTokenVersion(2);
        CustomUserDetails updatedUserDetails = new CustomUserDetails(user);

        boolean isValid = jwtService.isTokenValid(token, updatedUserDetails);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_WrongUsername_ReturnsFalse() {
        String token = jwtService.generateToken(customUserDetails);

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");
        otherUser.setRole(Role.USER);
        otherUser.setTokenVersion(1);

        boolean isValid = jwtService.isTokenValid(token, new CustomUserDetails(otherUser));

        assertThat(isValid).isFalse();
    }

    @Test
    void extractClaims_ReturnsCorrectCustomClaims() {
        String token = jwtService.generateToken(customUserDetails);

        Integer tokenVersion = jwtService.extractClaims(token, claims -> claims.get("tokenVersion", Integer.class));
        String role = jwtService.extractClaims(token, claims -> claims.get("role", String.class));

        assertThat(role).isEqualTo("ROLE_USER");
        assertThat(tokenVersion).isEqualTo(1);
    }

    @Test
    void isTokenValid_TamperedToken_ThrowsException() {
        String token = jwtService.generateToken(customUserDetails);
        String tamperedToken = token + "tampered";

        assertThat(jwtService.isTokenValid(tamperedToken, customUserDetails)).isFalse();
    }
}
