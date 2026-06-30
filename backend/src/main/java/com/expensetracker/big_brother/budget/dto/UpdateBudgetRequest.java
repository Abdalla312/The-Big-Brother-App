package com.expensetracker.big_brother.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateBudgetRequest(
        UUID categoryId,

        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in YYYY-MM format")
        String month,

        @DecimalMin(value = "0.01", message = "Budget limit must be greater than zero")
        BigDecimal limitAmount
) {
}
