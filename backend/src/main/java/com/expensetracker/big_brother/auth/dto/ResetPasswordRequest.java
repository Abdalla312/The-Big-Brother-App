package com.expensetracker.big_brother.auth.dto;

import com.expensetracker.big_brother.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotNull UUID userId,
        @NotBlank
        @Size(min = 8)
        @Size(max = 128)
        @ValidPassword
        String newPassword
) {
}
