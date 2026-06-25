package com.expensetracker.big_brother.category.dto;

import com.expensetracker.big_brother.common.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be 100 characters or less")
        String name,

        @NotNull(message = "Category type is required (INCOME or EXPENSE)")
        TransactionType type,

        @Size(max = 20, message = "Color code must be 20 characters or less")
        String color,

        @Size(max = 50, message = "Icon name must be 50 characters or less")
        String icon
) {
}
