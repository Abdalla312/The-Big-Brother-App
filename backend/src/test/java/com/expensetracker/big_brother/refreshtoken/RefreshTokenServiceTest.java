package com.expensetracker.big_brother.refreshtoken;

import com.expensetracker.big_brother.auth.JwtService;
import com.expensetracker.big_brother.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {
    private final UUID userId = UUID.randomUUID();
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User aUser() {
        User u = new User();
        u.setId(userId);
        u.setName("test_user");
        u.setEmail("test@example.com");
        return u;
    }

    private RefreshToken aRefreshToken(boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(aUser());
        token.setTokenHash("hashed-token");
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiryDays", 30L);
    }

    @Test
    void generateRefreshToken_Success() {
        User user = aUser();
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime now = LocalDateTime.now();
        String rawToken = refreshTokenService.generateRefreshToken(user);

        assertThat(rawToken).isNotNull();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(now);
    }

    @Test
    void rotateRefreshToken_Success() {
        User user = aUser();
        RefreshToken oldToken = aRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("new-access-token");

        RefreshTokenService.RefreshResult result = refreshTokenService.rotateRefreshToken("raw-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotNull();
        assertThat(oldToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void rotateRefreshToken_TokenNotFound_ThrowsException() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void rotateRefreshToken_RevokedToken_ThrowsException() {
        RefreshToken revoked = aRefreshToken(true, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("raw-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token revoked");
    }

    @Test
    void rotateRefreshToken_ExpiredToken_ThrowsException() {
        RefreshToken expired = aRefreshToken(false, LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("raw-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token expired");
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void revokeRefreshToken_Success() {
        RefreshToken token = aRefreshToken(false, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        refreshTokenService.revokeRefreshToken("raw-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeRefreshToken_NonExistentToken_DoesNothing() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllUserToken_Success() {
        refreshTokenService.revokeAllUserTokens(userId);
        verify(refreshTokenRepository).revokeByAllUserId(userId);
    }

    @Test
    void cleanupExpiredTokens_Success() {
        refreshTokenService.cleanupExpiredTokens();
        verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
