package com.expensetracker.big_brother.report.dto.projection;

import com.expensetracker.big_brother.common.TransactionType;

import java.math.BigDecimal;

public record TypeSum(TransactionType type, BigDecimal total) {}
