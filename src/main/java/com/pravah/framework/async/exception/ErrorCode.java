package com.pravah.framework.async.exception;

/**
 * Enumeration of error codes used throughout the async processing framework.
 * <br>
 * Each error code includes information about whether the error is retryable
 * and provides a human-readable description.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum ErrorCode {

    // Configuration errors (not retryable)
    CONFIGURATION_ERROR("CONFIGURATION_ERROR", "Configuration error", false),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request parameters", false),
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed", false),

    // Processing errors (retryable)
    PROCESSING_ERROR("PROCESSING_ERROR", "General processing error", true),
    API_CALL_FAILED("API_CALL_FAILED", "External API call failed", true),
    TIMEOUT_ERROR("TIMEOUT_ERROR", "Operation timed out", true),

    // Infrastructure errors (retryable)
    QUEUE_ERROR("QUEUE_ERROR", "Queue operation failed", true),
    STORAGE_ERROR("STORAGE_ERROR", "Storage operation failed", true),
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed", true),
    NETWORK_ERROR("NETWORK_ERROR", "Network connectivity error", true),

    // Service errors (conditionally retryable)
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "External service unavailable", true),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "Rate limit exceeded", true),
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", "Authentication failed", false),
    AUTHORIZATION_ERROR("AUTHORIZATION_ERROR", "Authorization failed", false),

    // Framework errors
    SERIALIZATION_ERROR("SERIALIZATION_ERROR", "Serialization/deserialization error", false),
    RETRY_EXHAUSTED("RETRY_EXHAUSTED", "All retry attempts exhausted", false),
    CIRCUIT_BREAKER_OPEN("CIRCUIT_BREAKER_OPEN", "Circuit breaker is open", true),

    // Business logic errors (not retryable)
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "Business rule violation", false),
    DUPLICATE_REQUEST("DUPLICATE_REQUEST", "Duplicate request detected", false),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Insufficient funds", false),

    // Unknown/generic errors
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Unknown error occurred", true);

    private final String code;
    private final String description;
    private final boolean retryable;

    ErrorCode(String code, String description, boolean retryable) {
        this.code = code;
        this.description = description;
        this.retryable = retryable;
    }

    /**
     * Get the error code string.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the human-readable description.
     *
     * @return error description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this error type is retryable.
     *
     * @return true if retryable, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Check if this is a configuration-related error.
     *
     * @return true if configuration error
     */
    public boolean isConfigurationError() {
        return this == CONFIGURATION_ERROR ||
                this == INVALID_REQUEST ||
                this == VALIDATION_ERROR;
    }

    /**
     * Check if this is an infrastructure-related error.
     *
     * @return true if infrastructure error
     */
    public boolean isInfrastructureError() {
        return this == QUEUE_ERROR ||
                this == STORAGE_ERROR ||
                this == DATABASE_ERROR ||
                this == NETWORK_ERROR;
    }

    /**
     * Check if this is a service-related error.
     *
     * @return true if service error
     */
    public boolean isServiceError() {
        return this == SERVICE_UNAVAILABLE ||
                this == RATE_LIMIT_EXCEEDED ||
                this == AUTHENTICATION_ERROR ||
                this == AUTHORIZATION_ERROR;
    }

    /**
     * Check if this is a business logic error.
     *
     * @return true if business logic error
     */
    public boolean isBusinessError() {
        return this == BUSINESS_RULE_VIOLATION ||
                this == DUPLICATE_REQUEST ||
                this == INSUFFICIENT_FUNDS;
    }

    /**
     * Get the severity level of this error.
     *
     * @return severity level (HIGH, MEDIUM, LOW)
     */
    public Severity getSeverity() {
        if (isConfigurationError() || this == SERIALIZATION_ERROR) {
            return Severity.HIGH;
        } else if (isInfrastructureError() || this == PROCESSING_ERROR) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    /**
     * Parse error code from string value.
     *
     * @param code string value to parse
     * @return corresponding ErrorCode
     */
    public static ErrorCode fromString(String code) {
        if (code == null) {
            return UNKNOWN_ERROR;
        }

        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equalsIgnoreCase(code)) {
                return errorCode;
            }
        }

        return UNKNOWN_ERROR;
    }

    /**
     * Get the recommended retry delay for this error type.
     *
     * @return recommended delay in seconds
     */
    public int getRecommendedRetryDelaySeconds() {
        return switch (this) {
            case RATE_LIMIT_EXCEEDED -> 300; // 5 minutes for rate limits
            case SERVICE_UNAVAILABLE -> 120; // 2 minutes for service unavailable
            case NETWORK_ERROR -> 60; // 1 minute for network issues
            case TIMEOUT_ERROR -> 90; // 1.5 minutes for timeouts
            case CIRCUIT_BREAKER_OPEN -> 180; // 3 minutes for circuit breaker
            default -> 30; // Default 30 seconds
        };
    }

    @Override
    public String toString() {
        return code;
    }

    /**
     * Severity levels for error codes.
     */
    public enum Severity {
        HIGH, MEDIUM, LOW
    }
}