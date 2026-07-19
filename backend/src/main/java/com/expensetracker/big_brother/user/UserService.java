package com.expensetracker.big_brother.user;

import com.expensetracker.big_brother.exception.DuplicateResourceException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import com.expensetracker.big_brother.user.dto.ChangePasswordRequest;
import com.expensetracker.big_brother.user.dto.DeleteAccountRequest;
import com.expensetracker.big_brother.user.dto.UpdateProfileRequest;
import com.expensetracker.big_brother.user.dto.UserResponse;
import com.expensetracker.big_brother.verification.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (request.email() != null && !request.email().equalsIgnoreCase(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.email()))
                throw new DuplicateResourceException("Email already in use");
            emailVerificationService.sendEmailChangeVerification(currentUser, request.email());
        }
        if (request.name() != null) currentUser.setName(request.name());
        log.info("Profile updated: {}", userId);
        return userMapper.toResponse(userRepository.save(currentUser));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        refreshTokenService.revokeAllUserTokens(userId);
        userRepository.delete(user);
        log.info("Account deleted: {}", userId);
    }

}
