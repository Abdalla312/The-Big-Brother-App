package com.expensetracker.big_brother.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentBreakdown(
        String paymentMethod,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal amount,
        double percentage
) {
}
