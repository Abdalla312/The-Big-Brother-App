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
        if (verificationRepository.existsByUserAndExpiresAtAfter(user, LocalDateTime.now())) return;
        verificationRepository.deleteByUser(user);
        verificationRepository.flush();
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken, user.getId());
        EmailVerificationToken token = new EmailVerificationToken(
                tokenHash,
                user,
                null,
                LocalDateTime.now().plusHours(1));
        verificationRepository.save(token);
        String verificationLink = backendUrl + "/api/v1/auth/verify-email?userId=" + user.getId() + "&token=" + rawToken;
        String htmlBody = buildEmailHtml(
                user.getName(),
                "Verify your email",
                "Click the button below to verify your email address:",
                verificationLink,
                "Verify Email",
                "If you didn't create this account, you can ignore this email.");
        emailService.sendHtml(user.getEmail(),"Verify your email", htmlBody);
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
                tokenHash, currentUser, newEmail, LocalDateTime.now().plusHours(1));
        verificationRepository.save(token);

        String verificationLink = backendUrl + "/api/v1/auth/verify-email?userId=" + currentUser.getId() + "&token=" + rawToken;

        String htmlBody = buildEmailHtml(
                currentUser.getName(),
                "Confirm your new email",
                "Click the button below to confirm this is your new email address:",
                verificationLink,
                "Confirm Email",
                "If you didn't request this change, please contact support immediately.");
        emailService.sendHtml(newEmail, "Confirm your new email", htmlBody);
        emailService.sendHtml(currentUser.getEmail(), "Email change requested",
                "A request to change your account email to " + newEmail
                        + " was made. If this wasn't you, please contact support immediately.");
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


    private String buildEmailHtml(
            String name, String title, String instruction,
            String link, String buttonText, String footerNote) {
        return """
                <!DOCTYPE html>
                         <html>
                         <body style="font-family: Arial, sans-serif; padding: 20px;">
                           <div style="max-width: 600px; margin: auto;">
                             <h2>%s</h2>
                             <p>Hi %s,</p>
                             <p>%s</p>
                             <a href="%s" style="display: inline-block; padding: 12px 24px; background: #4F46E5; color: white; text-decoration: none; border-radius: 6px;">%s</a>
                             <p style="margin-top: 20px; color: #666;">This link expires in 1 hour.</p>
                             <hr style="margin-top: 30px;">
                             <p style="color: #999; font-size: 12px;">%s</p>
                           </div>
                         </body>
                         </html>
                """.formatted(title, name, instruction, link, buttonText, footerNote);
    }
}
