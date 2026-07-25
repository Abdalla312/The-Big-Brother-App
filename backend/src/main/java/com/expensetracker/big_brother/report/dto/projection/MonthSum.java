package com.expensetracker.big_brother.report.dto.projection;

import com.expensetracker.big_brother.common.TransactionType;

import java.math.BigDecimal;

public record MonthSum(
        String month,
        TransactionType type,
        BigDecimal total) {
}
