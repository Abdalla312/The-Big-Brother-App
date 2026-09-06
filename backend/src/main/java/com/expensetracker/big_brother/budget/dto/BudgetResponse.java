package com.expensetracker.big_brother.budget.dto;

import com.expensetracker.big_brother.category.dto.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        CategoryResponse category,
        String month,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double percentUsed,
        LocalDateTime deletedAt
) {
}
