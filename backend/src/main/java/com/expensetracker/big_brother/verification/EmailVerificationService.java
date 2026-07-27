package com.expensetracker.big_brother.verification;

import com.expensetracker.big_brother.mail.EmailService;
import com.expensetracker.big_brother.mail.EmailTemplateService;
import com.expensetracker.big_brother.user.User;
import com.expensetracker.big_brother.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.backend-url}")
    private String backendUrl;
    @Value("${app.frontend-url}")
    private String frontendUrl;
    @Value("${app.verification.expiry-hours:1}")
    private int verificationExpiryHours;

    public EmailVerificationService(
            EmailVerificationRepository verificationRepository,
            UserRepository userRepository,
            EmailService emailService,
            EmailTemplateService emailTemplateService
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        if (verificationRepository.existsByUserAndExpiresAtAfter(user, LocalDateTime.now())) return;
        verificationRepository.deleteByUser(user);
        verificationRepository.flush();
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken, user.getId());
        EmailVerificationToken token = new EmailVerificationToken(
                tokenHash,
                user,
                null,
                LocalDateTime.now().plusHours(verificationExpiryHours));
        verificationRepository.save(token);
        String verificationLink = frontendUrl + "/verify?userId=" + user.getId() + "&token=" + rawToken;
        String htmlBody = emailTemplateService.renderVerificationEmail(
                user.getName(),
                verificationLink,
                verificationExpiryHours);
        emailService.sendHtml(user.getEmail(), "Verify your email", htmlBody);
    }

    @Transactional
    public void sendEmailChangeVerification(User currentUser, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (verificationRepository.existsByUserAndExpiresAtAfter(currentUser, LocalDateTime.now())) return;

        verificationRepository.deleteByUser(currentUser);
        verificationRepository.flush();
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken, currentUser.getId());
        EmailVerificationToken token = new EmailVerificationToken(
                tokenHash, currentUser, newEmail, LocalDateTime.now().plusHours(verificationExpiryHours));
        verificationRepository.save(token);

        String verificationLink = frontendUrl + "/verify?userId=" + currentUser.getId() + "&token=" + rawToken;

        String htmlBody = emailTemplateService.renderEmailChangeVerification(
                currentUser.getName(),
                newEmail,
                verificationLink,
                verificationExpiryHours);
        emailService.sendHtml(newEmail, "Confirm your new email", htmlBody);
        
        // Send notification to old email
        String notificationHtml = emailTemplateService.renderEmailChangeNotification(
                currentUser.getName(),
                newEmail,
                verificationExpiryHours);
        emailService.sendHtml(currentUser.getEmail(), "Email change requested", notificationHtml);
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
        if (token.getNewEmail() != null) {
            user.setEmail(token.getNewEmail());
        } else {
            user.setUserVerified(true);
        }
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
