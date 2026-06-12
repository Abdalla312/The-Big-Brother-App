package com.expensetracker.big_brother.exception;

import com.expensetracker.big_brother.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            WebRequest request) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        String errorCode = status.name();
        String message = exception.getReason();
        String path = request.getDescription(false).replace("uri=", "");
        log.warn("ResponseStatusException: {} - {}", errorCode, message);
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorCode,
                message,
                Instant.now(),
                path,
                null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String path = request.getDescription(false).replace("uri", "");

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();
        log.warn("Validation field: {} errors", fieldErrors.size());
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                Instant.now(),
                path,
                fieldErrors
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String path = request.getDescription(false).replace("uri=", "");
        log.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                Instant.now(),
                path,
                null
        );
        return new ResponseEntity<>(errorResponse,status);
    }

}
