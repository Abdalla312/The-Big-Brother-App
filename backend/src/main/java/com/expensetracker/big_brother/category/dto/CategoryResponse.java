package com.expensetracker.big_brother.category.dto;

import com.expensetracker.big_brother.common.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;


public record CategoryResponse(
        UUID id,
        String name,
        TransactionType type,
        String color,
        String icon,
        boolean isDefault,
        LocalDateTime deletedAt
) {}
