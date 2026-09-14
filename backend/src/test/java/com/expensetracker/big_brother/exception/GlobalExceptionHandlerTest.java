package com.expensetracker.big_brother.exception;

import com.expensetracker.big_brother.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    @Mock
    private HttpServletRequest request;
    @Mock
    private WebRequest webRequest;
    @Mock
    private HttpInputMessage inputMessage;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    void handleAccessDeniedException_Returns403() {
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(exception, request);

        assertError(response, HttpStatus.FORBIDDEN, 403, "FORBIDDEN", "Access is denied");

    }

    @Test
    void handleMethodNotAllowed_Returns405() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("PUT");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(exception, webRequest);

        assertError(response, HttpStatus.METHOD_NOT_ALLOWED, 405, "METHOD_NOT_ALLOWED",
                "Request method 'PUT' is not supported");
    }

    @Test
    void handleUnsupportedMediaType_Returns415() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException("Content-Type 'text/xml' is not supported");

        ResponseEntity<ErrorResponse> response = handler.handleUnsupportedMediaType(exception, webRequest);

        assertError(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type 'text/xml' is not supported");
    }

    @Test
    void handleDataIntegrityViolation_Returns409() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(exception, webRequest);

        assertError(response, HttpStatus.CONFLICT, 409, "CONFLICT",
                "Operation would violate a database constraint");
    }

    @Test
    void handleUsernameNotFound_Returns401() {
        UsernameNotFoundException exception = new UsernameNotFoundException("user@test.com");

        ResponseEntity<ErrorResponse> response = handler.handleUsernameNotFound(exception, webRequest);

        assertError(response, HttpStatus.UNAUTHORIZED, 401, "UNAUTHORIZED",
                "Invalid email or password");
    }

    @Test
    void handleRuntimeException_Returns500() {
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(exception, webRequest);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, 500, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred");
    }

    @Test
    void handleGlobalException_Returns500() {
        Exception exception = new Exception("unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(exception, webRequest);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, 500, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred");
    }

    @Test
    void handleDateFormat_DateTimeException_ReturnsBadRequest() {
        Throwable cause = new RuntimeException("could not parse date-time field");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Unexpected token", cause, inputMessage);

        ResponseEntity<ErrorResponse> response = handler.handleDateFormat(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST",
                "Invalid date format. Use yyyy-MM");
    }

    @Test
    void handleDateFormat_MissingBody_ReturnsBadRequest() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Required request body is missing", (Throwable) null, inputMessage);

        ResponseEntity<ErrorResponse> response = handler.handleDateFormat(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST",
                "Required request body is missing");
    }

    @Test
    void handleDateFormat_NullCauseAndNullMessage_ReturnsDefaultMessage() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(null, (Throwable) null, inputMessage);

        ResponseEntity<ErrorResponse> response = handler.handleDateFormat(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST", "Request body is not readable");
    }

    @Test
    void handleDateFormat_GenericMessage_ReturnsRawMessage() {
        Throwable cause = new RuntimeException("no matching pattern");
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("Some unexpected error", cause, inputMessage);

        ResponseEntity<ErrorResponse> response = handler.handleDateFormat(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST", "Some unexpected error");
    }

    @Test
    void handleTypeMismatch_MonthParam_SpecialMessage() {
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("13", int.class, "month", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST",
                "Invalid date format. Use yyyy-MM");
    }

    @Test
    void handleTypeMismatch_OtherParam_GenericMessage() {
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("abc", UUID.class, "id", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, webRequest);

        assertError(response, HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST",
                "Invalid value for parameter 'id': abc");
    }

    private void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status,
                             int statusCode, String error, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(statusCode);
        assertThat(response.getBody().error()).isEqualTo(error);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
