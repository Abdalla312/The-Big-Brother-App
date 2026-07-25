package com.expensetracker.big_brother.report.dto.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSum(
        String paymentMethod,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal amount
) {
}
