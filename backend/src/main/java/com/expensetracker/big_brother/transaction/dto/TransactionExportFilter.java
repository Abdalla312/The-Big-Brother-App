package com.expensetracker.big_brother.transaction.dto;

import com.expensetracker.big_brother.common.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionExportFilter(
        LocalDate from,
        LocalDate to,
        TransactionType type,
        UUID categoryId
) {
}
