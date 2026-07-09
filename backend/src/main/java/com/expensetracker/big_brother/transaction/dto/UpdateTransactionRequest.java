package com.expensetracker.big_brother.transaction.dto;

import com.expensetracker.big_brother.common.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionRequest(
        TransactionType type,

        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
        BigDecimal amount,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        LocalDate transactionDate,

        @Size(max = 500, message = "Note must be 500 characters or less")
        String note,

        @Size(max = 100, message = "Payment method must be 100 characters or less")
        String paymentMethod,

        UUID categoryId
) {
}
