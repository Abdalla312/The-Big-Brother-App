package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.auth.dto.LoginRequest;
import com.expensetracker.big_brother.auth.dto.RefreshRequest;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import com.expensetracker.big_brother.mail.EmailService;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.verification.EmailVerificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
