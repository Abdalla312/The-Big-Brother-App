package com.expensetracker.big_brother.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetComparisonResponse(
        UUID categoryId,
        String name,
        String color,
        BigDecimal budgeted,
        BigDecimal spent,
        BigDecimal remaining,
        double percentUsed
) {
}
