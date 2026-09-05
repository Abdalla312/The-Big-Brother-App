package com.expensetracker.big_brother.transaction.dto;

import com.expensetracker.big_brother.category.dto.CategoryResponse;
import com.expensetracker.big_brother.common.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        LocalDate transactionDate,
        String note,
        String paymentMethod,
        CategoryResponse category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
