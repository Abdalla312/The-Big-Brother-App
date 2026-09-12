package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.*;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import com.expensetracker.big_brother.verification.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks
    private AuthService authService;

    private ResendVerificationRequest aResendRequest() {
        return new ResendVerificationRequest("test@example.com");
    }

    private User anUnVerifiedUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setName("test_user");
        u.setEmail("test@example.com");
        u.setRole(Role.USER);
        u.setUserVerified(false);
        return u;
    }

    private User aVerifiedUser() {
        User u = anUnVerifiedUser();
        u.setUserVerified(true);
        return u;
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("test_user", "test@example.com", "Password1");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(anUnVerifiedUser());

        AuthResponse response = authService.register(request);

        assertThat(response.name()).isEqualTo("test_user");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.verified()).isFalse();
        assertThat(response.accessToken()).isNull();
        verify(emailVerificationService).sendVerificationEmail(any());
    }

    @Test
    void Register_DuplicateEmail_ThrowsException() {
        // given
        RegisterRequest request = new RegisterRequest("test_user", "test@example.com", "Password1");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void register_NormalizesEmail_Success() {
        RegisterRequest request = new RegisterRequest("Test@Example.com", "test@example.com", "Password1");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertThat(saved.getEmail()).isEqualTo("test@example.com");
            return saved;
        });
        authService.register(request);
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");
        User user = aVerifiedUser();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.verified()).isTrue();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_UnverifiedUser_ThrowsForbidden() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");
        doThrow(new DisabledException("")).when(authenticationManager).authenticate(any());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_BadCredentials_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");
        doThrow(new BadCredentialsException("")).when(authenticationManager).authenticate(any());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_UserNotFoundAfterAuth_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void verifyEmail_Success() {
        UUID userId = UUID.randomUUID();
        String token = "raw-token";

        assertDoesNotThrow(() -> authService.verifyEmail(userId, token));
        verify(emailVerificationService).verifyEmail(token, userId);
    }

    @Test
    void verifyEmail_InvalidToken_ThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        String token = "bad-token";

        doThrow(new IllegalArgumentException("Invalid verification token"))
                .when(emailVerificationService).verifyEmail(token, userId);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.verifyEmail(userId, token));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("Invalid verification token");
    }

    @Test
    void resendVerification_Success() {
        ResendVerificationRequest request = aResendRequest();
        User user = anUnVerifiedUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(request);
        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    void resendVerification_AlreadyVerified_DoesNothing() {
        ResendVerificationRequest request = aResendRequest();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(aVerifiedUser()));
        authService.resendVerification(request);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void resendVerification_EmailNotFound_DoesNothing() {
        ResendVerificationRequest request = aResendRequest();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        authService.resendVerification(request);
    }

    @Test
    void forgotPassword_UserExists_SendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        User user = aVerifiedUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(emailVerificationService).sendPasswordResetEmail(user);
    }

    @Test
    void forgotPassword_UserNotFound_DoesNothing() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(emailVerificationService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_EmailNormalized() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("Test@EXAMPLE.com"));
        verify(userRepository).findByEmail("test@example.com");
        verify(emailVerificationService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void resetPassword_Success() {
        User user = aVerifiedUser();
        user.setTokenVersion(3);
        UUID userId = user.getId();
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", userId, "NewPassword1");

        when(emailVerificationService.verifyPasswordResetToken("reset-token", userId)).thenReturn(user);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("hashed-new-password");

        authService.resetPassword(request);

        assertThat(user.getTokenVersion()).isEqualTo(4);
        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-password");
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(userId);
    }

    @Test
    void resetPassword_UserNotFoundAfterVerification_ThrowsIllegalArgument() {
        User user = aVerifiedUser();
        user.setTokenVersion(5);
        UUID userId = user.getId();
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", userId, "NewPassword1");
        when(emailVerificationService.verifyPasswordResetToken("reset-token", userId)).thenReturn(user);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword(request));

        assertThat(exception.getMessage()).isEqualTo("User not found");
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    void resetPassword_TokenVersionIncremented() {
        User user = aVerifiedUser();
        user.setTokenVersion(5);
        UUID userId = user.getId();
        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token", userId, "NewPassword1");
        when(emailVerificationService.
                verifyPasswordResetToken("reset-token", userId)).thenReturn(user);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("hashed");

        authService.resetPassword(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getTokenVersion()).isEqualTo(6);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        verify(refreshTokenService).revokeAllUserTokens(userId);

    }
}
