package com.expensetracker.big_brother.report.dto.projection;

import com.expensetracker.big_brother.common.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySum(
        UUID categoryId,
        String name,
        String color,
        String icon,
        TransactionType type,
        BigDecimal amount
) {
}
