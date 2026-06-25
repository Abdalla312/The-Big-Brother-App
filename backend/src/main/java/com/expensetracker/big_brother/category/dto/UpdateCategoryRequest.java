package com.expensetracker.big_brother.category.dto;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 100) String name,
        @Size(max = 20) String color,
        @Size(max = 50) String icon
) {}
