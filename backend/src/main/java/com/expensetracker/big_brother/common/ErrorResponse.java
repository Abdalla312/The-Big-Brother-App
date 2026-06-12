package com.expensetracker.big_brother.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status,
                            String error,
                            String message,
                            Instant timestamp,
                            String path,
                            List<FieldError> errors) {

    public record FieldError(String field, String message) {}
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, Instant.now(), path, null);
    }
}
