package com.example.order.controller;

import com.example.order.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void testHandleIllegalArgumentException() {
        String message = "Invalid input";
        IllegalArgumentException exception = new IllegalArgumentException(message);

        ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_INPUT", body.getErrorCode());
        assertEquals(message, body.getMessage());
    }

    @Test
    void testHandleIllegalStateException() {
        String message = "Version mismatch";
        IllegalStateException exception = new IllegalStateException(message);

        ResponseEntity<ApiError> response = handler.handleIllegalState(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INVALID_STATE", body.getErrorCode());
        assertEquals(message, body.getMessage());
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "customerId", "Customer ID is required");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        ResponseEntity<ApiError> response = handler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_ERROR", body.getErrorCode());
        assertNotNull(body.getDetails());
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ApiError> response = handler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.getErrorCode());
        assertEquals("An unexpected error occurred", body.getMessage());
    }

    @Test
    void testExceptionHandling_WithCorrelationId() {
        String correlationId = "test-correlation-id";
        MDC.put("correlationId", correlationId);

        IllegalArgumentException exception = new IllegalArgumentException("Test error");
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception);

        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals(correlationId, body.getCorrelationId());
    }
}

