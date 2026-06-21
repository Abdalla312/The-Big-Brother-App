package com.expensetracker.big_brother.exception;

import com.expensetracker.big_brother.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.View;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final View error;

    public GlobalExceptionHandler(View error) {
        this.error = error;
    }

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException exception, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Missing request parameter: {}", exception.getParameterName());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "BAD_REQUEST",
                "Required parameter '" + exception.getParameterName() + "' is missing",
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Type mismatch for parameter: {}", exception.getName());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "BAD_REQUEST",
                "Invalid value for parameter '" + exception.getName() + "': " + exception.getValue(),
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstrainViolation(
            ConstraintViolationException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Constrain violation: {} errors", exception.getConstraintViolations().size());
        List<ErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Method not allowed: {}", exception.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "METHOD_NOT_ALLOWED",
                exception.getMessage(),
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Unsupported media type: {}", exception.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "UNSUPPORTED_MEDIA_TYPE",
                exception.getMessage(),
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        String path = request.getDescription(false).replace("uri", "");
        log.error("Data integrity violation", exception);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "CONFLICT",
                "Operation would violate a database constraint",
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String path = request.getDescription(false).replace("uri", "");
        log.warn("Illegal argument: {}", exception.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "BAD_REQUEST",
                exception.getMessage(),
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException exception,
            WebRequest request) {
            HttpStatus status = HttpStatus.UNAUTHORIZED;
            String path = request.getDescription(false).replace("uri=", "");
            log.warn("User not found: {}", exception.getMessage());
            ErrorResponse errorResponse = ErrorResponse.of(
                    status.value(),
                    "UNAUTHORIZED",
                    "Invalid email or password",
                    path
            );
            return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException (
            RuntimeException exception,
            WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String path = request.getDescription(false).replace("uri=", "");
        log.error("Unexpected runtime error", exception);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                path
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
