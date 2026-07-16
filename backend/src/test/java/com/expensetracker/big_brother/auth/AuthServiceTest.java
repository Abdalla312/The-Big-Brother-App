package com.expensetracker.big_brother.auth;

import com.expensetracker.big_brother.auth.dto.AuthResponse;
import com.expensetracker.big_brother.auth.dto.LoginRequest;
import com.expensetracker.big_brother.auth.dto.RegisterRequest;
import com.expensetracker.big_brother.auth.dto.ResendVerificationRequest;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import com.expensetracker.big_brother.user.Role;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import com.expensetracker.big_brother.verification.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private RegisterRequest aRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setName("test_user");
        r.setEmail("test@example.com");
        r.setPassword("Password1");
        return r;
    }

    private LoginRequest aLoginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("test@example.com");
        r.setPassword("Password1");
        return r;
    }

    private ResendVerificationRequest aResendRequest() {
        ResendVerificationRequest r = new ResendVerificationRequest();
        r.setEmail("test@example.com");
        return r;
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
    void register_successfully() {
        RegisterRequest request = aRegisterRequest();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(anUnVerifiedUser());

        AuthResponse response = authService.register(aRegisterRequest());

        assertThat(response.getName()).isEqualTo("test_user");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.isVerified()).isFalse();
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getMessage()).contains("verify");
        verify(emailVerificationService).sendVerificationEmail(any());
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        // given
        RegisterRequest request = aRegisterRequest();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void register_normalizesEmail() {
        RegisterRequest request = aRegisterRequest();
        request.setEmail("Test@Example.com");

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
    void login_success() {
        LoginRequest request = aLoginRequest();
        User user = aVerifiedUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.isVerified()).isTrue();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_unverifiedUser_throwsForbidden() {
        LoginRequest request = aLoginRequest();
        doThrow(new DisabledException("")).when(authenticationManager).authenticate(any());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_badCredentials_throwsUnauthorized() {
        LoginRequest request = aLoginRequest();
        doThrow(new BadCredentialsException("")).when(authenticationManager).authenticate(any());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_userNotFoundAfterAuth_throwsUnauthorized() {
        LoginRequest request = aLoginRequest();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void verifyEmail_success() {
        UUID userId = UUID.randomUUID();
        String token = "raw-token";

        assertDoesNotThrow(() -> authService.verifyEmail(userId, token));
        verify(emailVerificationService).verifyEmail(token, userId);
    }

    @Test
    void verifyEmail_invalidToken_throwsBadRequest() {
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
    void resendVerification_success() {
        ResendVerificationRequest request = aResendRequest();
        User user = anUnVerifiedUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(request);
        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    void resendVerification_alreadyVerified_doesNothing() {
        ResendVerificationRequest request = aResendRequest();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(aVerifiedUser()));
        authService.resendVerification(request);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void resendVerification_emailNotFound_doesNothing() {
        ResendVerificationRequest request = aResendRequest();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        authService.resendVerification(request);
    }

}
