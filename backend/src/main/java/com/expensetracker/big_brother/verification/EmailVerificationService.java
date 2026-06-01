package com.expensetracker.big_brother.verification;

import com.expensetracker.big_brother.mail.EmailService;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.backend-url}")
    private String backendUrl;

    public EmailVerificationService(
            EmailVerificationRepository verificationRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        verificationRepository.deleteByUser(user);
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken, user.getId());
        EmailVerificationToken token = new EmailVerificationToken(
                tokenHash,
                user,
                LocalDateTime.now().plusHours(1));
        verificationRepository.save(token);
        String verificationLink = backendUrl + "/api/auth/verify-email?userId="
                + user.getId()
                + "&token="
                + rawToken;

        emailService.send(
                user.getEmail(),
                "Verify your email",
                "Click this link to verify your email: " + verificationLink
        );
    }

    @Transactional
    public void verifyEmail(String rawToken, UUID userId) {
        String tokenHash = hashToken(rawToken, userId);

        EmailVerificationToken token = verificationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!token.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Invalid verification token");
        }
        if (token.isExpired()) {
            verificationRepository.delete(token);
            throw new IllegalArgumentException("Verification token expired");
        }
        User user = token.getUser();
        user.setUserVerified(true);

        userRepository.save(user);
        verificationRepository.delete(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token, UUID userId) {
        String value = userId + ":" + token;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not hash verification token", e);
        }
    }
}
