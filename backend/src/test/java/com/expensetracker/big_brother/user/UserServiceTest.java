package com.expensetracker.big_brother.user;


import com.expensetracker.big_brother.exception.DuplicateResourceException;
import com.expensetracker.big_brother.exception.ResourceNotFoundException;
import com.expensetracker.big_brother.refreshtoken.RefreshTokenService;
import com.expensetracker.big_brother.user.dto.ChangePasswordRequest;
import com.expensetracker.big_brother.user.dto.DeleteAccountRequest;
import com.expensetracker.big_brother.user.dto.UpdateProfileRequest;
import com.expensetracker.big_brother.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private final UUID userId = UUID.randomUUID();
    @Mock
    private UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RefreshTokenService refreshTokenService;
    @InjectMocks
    UserService userService;

    private User aUser() {
        User u = new User();
        u.setId(userId);
        u.setName("test_user");
        u.setEmail("test@example.com");
        u.setPasswordHash(passwordEncoder.encode("CurrentPass1!"));
        u.setUserVerified(true);
        return u;
    }

    private UserResponse aUserResponse(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(), null);
    }

    @Test
    void getProfile_Success() {
        User user = aUser();
        UserResponse response = aUserResponse(user);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);
        UserResponse result = userService.getProfile(userId);
        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(userId);
    }

    @Test
    void getProfile_NotFound_ThrowsException() {
        UUID aUserId = UUID.randomUUID();
        when(userRepository.findById(aUserId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getProfile(aUserId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userMapper, never()).toResponse(any(User.class));
    }

    @Test
    void updateProfile_ChangeName_Success() {
        User user = aUser();
        UpdateProfileRequest request = new UpdateProfileRequest("newName", null);
        UserResponse response = new UserResponse(
                userId, "newName", user.getEmail(), user.getRole(), user.getCreatedAt(), null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class))).thenReturn(response);
        when(userRepository.save(any(User.class))).thenReturn(user);
        UserResponse result = userService.updateProfile(userId, request);
        assertThat(result.name()).isEqualTo("newName");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    void updateProfile_EmailAlreadyTaken_ThrowsException() {
        User user = aUser();

        UpdateProfileRequest request = new UpdateProfileRequest(null, "existing@email.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(request.email())).thenReturn(true);
        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateProfile_emailUnchanged_Skips() {
        User user = aUser();
        UpdateProfileRequest request = new UpdateProfileRequest(null, "test@example.com");
        UserResponse response = aUserResponse(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateProfile(userId, request);

        assertThat(result).isEqualTo(response);

        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void changePassword_Success() {
        User user = aUser();
        ChangePasswordRequest request = new ChangePasswordRequest(
                "hashed", "newPassw0rd");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        userService.changePassword(userId, request);
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).revokeAllUserTokens(userId);
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        User user = aUser();
        ChangePasswordRequest request = new ChangePasswordRequest("Wrong_Password", "newPassw0rd");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccount_Success() {
        User user = aUser();
        DeleteAccountRequest request = new DeleteAccountRequest("CurrentPass1!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        userService.deleteAccount(userId, request);
        verify(userRepository).delete(user);
        verify(refreshTokenService).revokeAllUserTokens(userId);
    }

    @Test
    void deleteAccount_WrongPassword_ThrowsException() {
        User user = aUser();
        DeleteAccountRequest request = new DeleteAccountRequest("WrongPass");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(),user.getPasswordHash())).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteAccount(userId, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).delete(user);
    }
}
