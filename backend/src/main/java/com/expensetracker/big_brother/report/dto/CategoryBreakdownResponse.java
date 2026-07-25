package com.expensetracker.big_brother.report.dto;

import com.expensetracker.big_brother.common.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBreakdownResponse(
        UUID categoryId,
        String name,
        String color,
        String icon,
        TransactionType type,
        BigDecimal amount,
        double percentage
) {
}
