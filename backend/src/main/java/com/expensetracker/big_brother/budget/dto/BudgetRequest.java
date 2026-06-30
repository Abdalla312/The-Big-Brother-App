package com.expensetracker.big_brother.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotBlank(message = "Month is required (format: YYYY-MM)")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
        String month,

        @NotNull(message = "Budget limit is required")
        @DecimalMin(value = "0.01", message = "Budget limit must be greater than zero")
        BigDecimal limitAmount
) {
}
