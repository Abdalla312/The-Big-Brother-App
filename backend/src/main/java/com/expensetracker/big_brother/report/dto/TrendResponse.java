package com.expensetracker.big_brother.report.dto;

import java.math.BigDecimal;

public record TrendResponse(
        String month,
        BigDecimal income,
        BigDecimal expenses
) {}
