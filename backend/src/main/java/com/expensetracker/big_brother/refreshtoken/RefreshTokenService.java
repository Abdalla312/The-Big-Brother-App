package com.expensetracker.big_brother.refreshtoken;

import com.expensetracker.big_brother.auth.JwtService;
import com.expensetracker.big_brother.security.CustomUserDetails;
import com.expensetracker.big_brother.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public RefreshResult rotateRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (oldToken.isRevoked()) throw new IllegalArgumentException("Refresh token revoked");

        if (oldToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(oldToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = oldToken.getUser();
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newAccessToken = jwtService.generateToken(new CustomUserDetails(user));
        String newRefreshToken = generateRefreshToken(user);

        log.info("Refresh token rotated for user: {}", user.getId());
        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    @Transactional
    public String generateRefreshToken(User user) {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked");
                });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeByAllUserId(userId);
        log.info("All refresh tokens revoked for user: {}", userId);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            log.info("Expired refresh tokens cleaned up");
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RefreshResult(String accessToken, String refreshToken) {}
}
