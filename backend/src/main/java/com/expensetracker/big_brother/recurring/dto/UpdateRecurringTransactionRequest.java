package com.expensetracker.big_brother.recurring.dto;

import com.expensetracker.big_brother.recurring.RecurrenceFrequency;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateRecurringTransactionRequest(
        @Positive BigDecimal amount,
        UUID categoryId,
        RecurrenceFrequency frequency,
        LocalDate nextExecutionDate,
        String paymentMethod,
        String note
) {
}
