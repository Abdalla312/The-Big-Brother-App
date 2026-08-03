package com.expensetracker.big_brother.user.dto;

import com.expensetracker.big_brother.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Size(min = 8)
        @Size(max = 128)
        @ValidPassword
        String newPassword
) {
}
