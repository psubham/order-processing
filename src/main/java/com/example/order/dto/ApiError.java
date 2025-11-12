package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String errorCode;
    private String message;
    private Instant timestamp = Instant.now();
    private String correlationId;
    private Map<String, Object> details;

    public ApiError(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ApiError(String errorCode, String message, String correlationId) {
        this(errorCode, message);
        this.correlationId = correlationId;
    }
}

