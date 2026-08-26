package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.BaseIntegrationTest;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.dto.ChangePasswordRequest;
import com.expensetracker.big_brother.user.dto.DeleteAccountRequest;
import com.expensetracker.big_brother.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserIntegrationTest extends BaseIntegrationTest {

    private CustomUserDetails userAPrincipal;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        clearDatabase();
        userA = seedUser("userA@example.com", "User A");
        userB = seedUser("userB@example.com", "User B");
        userAPrincipal = new CustomUserDetails(userA);
    }

//    getProfile_returnsProfile│GET /me│200 + user data
    @Test
    void getProfile_Success_Returns200() throws Exception {
        performGet("/api/v1/users/me", userAPrincipal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile retrieved"))
                .andExpect(jsonPath("$.data.name").value("User A"));
    }

    @Test
    void getProfile_UnAuthorized_Returns401() throws Exception {
        performGet("/api/v1/users/me", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
//    updateProfile_ownProfile│PATCH /me│200
    @Test
    void updateProfile_OwnProfile_Returns200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("New User A", null);
        performPatch("/api/v1/users/me", userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated"))
                .andExpect(jsonPath("$.data.name").value("New User A"));
    }
//    up dateProfile_emailConflict│PATCH /me (existing email)│409
    @Test
    void updateProfile_EmailConflict_Returns409() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(null, "userB@example.com");
        performPatch("/api/v1/users/me", userAPrincipal, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
//    changePassword_success│PATCH /me/password│200
    @Test
    void changePassword_Success_Returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("hashed", "Password123#");
        performPatch("/api/v1/users/me/password", userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));
    }
//    changePassword_wrongCurrent│PATCH /me/password│400
    @Test
    void changePassword_WrongCurrent_Returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wr0ngPass", "Password123#");
        performPatch("/api/v1/users/me/password", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
//    deleteAccount_success│DELETE /me│200

    @Test
    void deleteAccount_Success_Returns200() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("hashed");
        performDelete("/api/v1/users/me", userAPrincipal, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account Deleted"));
    }
//    deleteAccount_wrongPassword│DELETE /me│400

    @Test
    void deleteAccount_WrongPassword_Returns400() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("WrongPassword");
        performDelete("/api/v1/users/me", userAPrincipal, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }
//    deleteAccount_unauthorized│DELETE /me (no token)│401
    @Test
    void deleteAccount_UnAuthorized_Returns401() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("hashed");
        performDelete("/api/v1/users/me", null, request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

}
