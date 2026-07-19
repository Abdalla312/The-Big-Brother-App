package com.expensetracker.big_brother.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String password
) {
}
