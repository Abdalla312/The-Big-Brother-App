package com.expensetracker.big_brother.verification;

import com.expensetracker.big_brother.mail.EmailService;
import com.expensetracker.big_brother.mail.EmailTemplateService;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceTest {
    private final UUID userId = UUID.randomUUID();

    @Mock
    private EmailVerificationRepository verificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailTemplateService emailTemplateService;
    @InjectMocks
    private EmailVerificationService verificationService;

    private User aUser() {
        User u = new User();
        u.setId(userId);
        u.setName("John Doe");
        u.setEmail("john@example.com");
        u.setUserVerified(false);
        return u;
    }

    private String computeHash(String token, UUID uId) {
        String value = uId + ":" + token;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(verificationService, "verificationExpiryHours", 1);
        ReflectionTestUtils.setField(verificationService, "resetExpiryMinutes", 30);
    }

    @Test
    void sendVerificationEmail_Success() {
        User user = aUser();
        when(verificationRepository.existsByUserAndExpiresAtAfter(eq(user), any())).thenReturn(false);
        when(emailTemplateService.renderVerificationEmail(any(), any(), anyInt())).thenReturn("<html>Verify</html>");

        verificationService.sendVerificationEmail(user);

        verify(verificationRepository).deleteByUser(user);
        verify(verificationRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendHtmlAsync(eq("john@example.com"), eq("Verify your email"), eq("<html>Verify</html>"));
    }

    @Test
    void sendVerificationEmail_ActiveTokenExists_DoesNothing() {
        User user = aUser();
        when(verificationRepository.existsByUserAndExpiresAtAfter(eq(user), any())).thenReturn(true);
        verificationService.sendVerificationEmail(user);

        verify(verificationRepository, never()).save(any());
        verify(emailService, never()).sendHtmlAsync(any(), any(), any());
    }

    @Test
    void sendEmailChangeVerification_Success() {
        User user = aUser();
        String newEmail = "new@example.com";
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(verificationRepository.existsByUserAndExpiresAtAfter(eq(user), any())).thenReturn(false);
        when(emailTemplateService.renderEmailChangeVerification(any(), any(), any(), anyInt())).thenReturn("<html>Confirm</html>");
        when(emailTemplateService.renderEmailChangeNotification(any(), any(), anyInt())).thenReturn("<html>Notif</html>");

        verificationService.sendEmailChangeVerification(user, newEmail);

        verify(emailService).sendHtmlAsync(eq(newEmail), eq("Confirm your new email"), eq("<html>Confirm</html>"));
        verify(emailService).sendHtmlAsync(eq("john@example.com"), eq("Email change requested"), eq("<html>Notif</html>"));
    }

    @Test
    void sendEmailChangeVerification_EmailTaken_ThrowsException() {
        User user = aUser();
        String newEmail = "existing@example.com";
        when(userRepository.existsByEmail(newEmail)).thenReturn(true);

        assertThatThrownBy(() -> verificationService.sendEmailChangeVerification(user, newEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(emailService, never()).sendHtmlAsync(any(), any(), any());
    }

    @Test
    void sendEmailChangeVerification_ActiveTokenExists_DoesNothing() {
        User user = aUser();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(verificationRepository.existsByUserAndExpiresAtAfter(eq(user), any())).thenReturn(true);

        verificationService.sendEmailChangeVerification(user, "new@example.com");

        verify(verificationRepository, never()).save(any());
        verify(emailService, never()).sendHtmlAsync(any(), any(), any());
    }

    @Test
    void sendPasswordResetEmail_Success() {
        User user = aUser();
        when(emailTemplateService.renderPasswordReset(any(), any(), anyInt())).thenReturn("<html>Reset</html>");

        verificationService.sendPasswordResetEmail(user);

        verify(verificationRepository).deleteByUserAndTokenType(user, TokenType.PASSWORD_RESET);
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationRepository).save(captor.capture());

        assertThat(captor.getValue().getTokenType()).isEqualTo(TokenType.PASSWORD_RESET);
        verify(emailService).sendHtmlAsync(eq("john@example.com"), eq("Reset your password"), eq("<html>Reset</html>"));
    }

    @Test
    void verifyEmail_Success() {
        User user = aUser();
        String rawToken = "rawToken123";
        String hash = computeHash(rawToken, userId);

        EmailVerificationToken token = new EmailVerificationToken(hash, user, null, LocalDateTime.now().plusHours(1));

        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        verificationService.verifyEmail(rawToken, userId);

        assertThat(user.isUserVerified()).isTrue();
        verify(userRepository).save(user);
        verify(verificationRepository).delete(token);
    }

    @Test
    void verifyEmail_EmailChangeToken_UpdatesUserEmail() {
        User user = aUser();
        String rawToken = "rawToken123";
        String hash = computeHash(rawToken, userId);

        EmailVerificationToken token = new EmailVerificationToken(hash, user, "new@example.com", LocalDateTime.now().plusHours(1));

        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        verificationService.verifyEmail(rawToken, userId);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(user);
        verify(verificationRepository).delete(token);
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsException() {
        User user = aUser();
        String rawToken = "rawToken";
        String hash = computeHash(rawToken, userId);

        EmailVerificationToken expiredToken = new EmailVerificationToken(hash, user, null, LocalDateTime.now().minusHours(1));

        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> verificationService.verifyEmail(rawToken, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification token expired");
        verify(verificationRepository).delete(expiredToken);
    }

    @Test
    void verifyEmail_PasswordResetToken_ThrowsException() {
        String rawToken = "reset-token";
        String hash = computeHash(rawToken, userId);
        User user = aUser();
        EmailVerificationToken token = new EmailVerificationToken(
                hash, user, null, LocalDateTime.now().plusHours(1), TokenType.PASSWORD_RESET);
        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> verificationService.verifyEmail(rawToken, userId));

        assertThat(exception.getMessage()).isEqualTo("Invalid verification token");
        verify(verificationRepository, never()).delete(token);
        assertThat(user.isUserVerified()).isFalse();
    }

    @Test
    void verifyPasswordResetToken_Success() {
        User user = aUser();
        String rawToken = "resetToken123";
        String hash = computeHash(rawToken, userId);

        EmailVerificationToken token = new EmailVerificationToken(hash, user, null, LocalDateTime.now().plusMinutes(15), TokenType.PASSWORD_RESET);

        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        User result = verificationService.verifyPasswordResetToken(rawToken, userId);

        assertThat(result).isEqualTo(user);
        verify(verificationRepository).delete(token);
    }

    @Test
    void verifyPasswordResetToken_WrongTokenType_ThrowsException() {
        User user = aUser();
        String rawToken = "resetToken123";
        String hash = computeHash(rawToken, userId);

        EmailVerificationToken token = new EmailVerificationToken(hash, user, null, LocalDateTime.now().plusMinutes(15), TokenType.EMAIL_VERIFICATION);

        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> verificationService.verifyPasswordResetToken(rawToken, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    void verifyEmail_UserIdMismatch_ThrowsException() {
        User userB = new User();
        userB.setId(UUID.randomUUID());
        String rawToken = "rawToken";
        String hash = computeHash(rawToken, userId);
        EmailVerificationToken token = new EmailVerificationToken(hash, userB, null, LocalDateTime.now().plusHours(1));
        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail(rawToken, userId));

        assertThat(ex.getMessage()).isEqualTo("Invalid verification token");
    }

    @Test
    void verifyPasswordResetToken_UserIdMismatch_ThrowsException() {
        User userB = new User();
        userB.setId(UUID.randomUUID());
        String rawToken = "rawToken";
        String hash = computeHash(rawToken, userId);
        EmailVerificationToken token = new EmailVerificationToken(hash, userB, null, LocalDateTime.now().plusMinutes(15), TokenType.PASSWORD_RESET);
        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> verificationService.verifyEmail(rawToken, userId));

        assertThat(ex.getMessage()).isEqualTo("Invalid verification token");
    }

    @Test
    void verifyPasswordResetToken_ExpiredToken_DeletesAndThrows() {
        User user = aUser();
        String hash = computeHash("rawToken", userId);
        EmailVerificationToken token = new EmailVerificationToken(hash, user, null, LocalDateTime.now().minusMinutes(1), TokenType.PASSWORD_RESET);
        when(verificationRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> verificationService.verifyPasswordResetToken("rawToken", userId));

        assertThat(ex.getMessage()).isEqualTo("Reset token expired");
        verify(verificationRepository).delete(token);
    }
}
