package com.expensetracker.big_brother.recurring.dto;

import com.expensetracker.big_brother.common.TransactionType;
import com.expensetracker.big_brother.recurring.RecurrenceFrequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRecurringTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull UUID categoryId,
        @NotNull RecurrenceFrequency frequency,
        @NotNull LocalDate startDate,
        @Length(message = "Payment method must not exceed 50 characters", max = 50) String paymentMethod,
        String note
) {
}