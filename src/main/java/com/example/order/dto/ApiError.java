package com.example.order.dto;

import java.time.Instant;
import java.util.Map;

public class ApiError {
    private String errorCode;
    private String message;
    private Instant timestamp;
    private String correlationId;
    private Map<String, Object> details;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ApiError(String errorCode, String message, String correlationId) {
        this(errorCode, message);
        this.correlationId = correlationId;
    }

    // Getters and setters
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}

