package com.expensetracker.big_brother.report.dto.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryExpenses(
        UUID categoryId,
        String name,
        String color,
        BigDecimal amount
) {
}
