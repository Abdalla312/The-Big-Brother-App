package com.expensetracker.big_brother.user.dto;

import com.expensetracker.big_brother.user.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
