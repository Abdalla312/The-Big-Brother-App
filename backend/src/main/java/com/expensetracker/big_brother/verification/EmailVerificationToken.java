package com.expensetracker.big_brother.verification;

import com.expensetracker.big_brother.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
@NoArgsConstructor
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Getter
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Getter
    @Column(nullable = true)
    private String newEmail;  // null for registration token, set = email-change token

    @Getter
    @Column(name = "expires_at",nullable = false)
    private LocalDateTime expiresAt;

    public EmailVerificationToken(String tokenHash, User user, String newEmail, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.newEmail = newEmail;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

}
