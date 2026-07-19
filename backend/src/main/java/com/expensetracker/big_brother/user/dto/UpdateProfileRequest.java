package com.expensetracker.big_brother.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String name,
        @Email String email
) {
}
