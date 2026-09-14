package com.expensetracker.big_brother.recurring.dto;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.recurring.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecurringTransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        CategoryResponse category,
        RecurrenceFrequency frequency,
        LocalDate nextExecutionDate,
        boolean isActive,
        String paymentMethod,
        String note,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
}