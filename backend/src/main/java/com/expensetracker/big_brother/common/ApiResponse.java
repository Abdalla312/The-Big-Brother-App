package com.expensetracker.big_brother.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int status,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(200, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(201, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null, Instant.now());
    }
}
