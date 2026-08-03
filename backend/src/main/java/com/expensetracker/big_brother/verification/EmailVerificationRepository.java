package com.expensetracker.big_brother.verification;

import com.expensetracker.big_brother.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);

    boolean existsByUserAndExpiresAtAfter(User user, LocalDateTime now);

    void deleteByUserAndTokenType(User user, TokenType tokenType);
}
