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
        verificationRepository.flush();
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken, user.getId());
        EmailVerificationToken token = new EmailVerificationToken(
                tokenHash,
                user,
                LocalDateTime.now().plusHours(1));
        verificationRepository.save(token);
        String verificationLink = backendUrl + "/api/v1/auth/verify-email?userId="
                + user.getId() + "&token=" + rawToken;
        String htmlBody = """
         <!DOCTYPE html>                                                  \s
         <html>                                                           \s
         <body style="font-family: Arial, sans-serif; padding: 20px;">    \s
           <div style="max-width: 600px; margin: auto;">                  \s
             <h2>Verify your email</h2>                                   \s
             <p>Hi %s,</p>                                                \s
             <p>Click the button below to verify your email address:</p>  \s
             <a href="%s" style="display: inline-block; padding: 12px     \s
     24px; background: #4F46E5; color: white; text-decoration: none;      \s
     border-radius: 6px;">Verify Email</a>                                \s
             <p style="margin-top: 20px; color: #666;">This link expires  \s
     in 1 hour.</p>                                                       \s
             <hr style="margin-top: 30px;">                               \s
             <p style="color: #999; font-size: 12px;">If you didn't       \s
     create this account, you can ignore this email.</p>                  \s
           </div>                                                         \s
         </body>                                                          \s
         </html>                                                          \s
        \s""".formatted(user.getName(), verificationLink);
        emailService.sendHtml(user.getEmail(),"Verify your email", htmlBody);
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
