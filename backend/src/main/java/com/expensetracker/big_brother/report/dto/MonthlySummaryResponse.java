package com.expensetracker.big_brother.report.dto;

import java.math.BigDecimal;

public record MonthlySummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance,
        long transactionCount
) {
}
