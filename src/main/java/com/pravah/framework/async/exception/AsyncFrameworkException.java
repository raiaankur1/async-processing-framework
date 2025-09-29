package com.pravah.framework.async.exception;

/**
 * Base exception class for the async processing framework.
 * <br>
 * This exception provides structured error information including
 * error codes, request context, and retry hints.
 *
 * @author Ankur Rai
 * @version 1.0
 */
public class AsyncFrameworkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String requestId;
    private final String context;

    /**
     * Create exception with error code and message.
     *
     * @param errorCode the error code
     * @param message the error message
     */
    public AsyncFrameworkException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = null;
        this.context = null;
    }

    /**
     * Create exception with error code, message, and request ID.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param requestId the request ID for context
     */
    public AsyncFrameworkException(ErrorCode errorCode, String message, String requestId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.context = null;
    }

    /**
     * Create exception with error code, message, request ID, and cause.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param requestId the request ID for context
     * @param cause the underlying cause
     */
    public AsyncFrameworkException(ErrorCode errorCode, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.context = null;
    }

    /**
     * Create exception with full context information.
     *
     * @param errorCode the error code
     * @param message the error message
     * @param requestId the request ID for context
     * @param context additional context information
     * @param cause the underlying cause
     */
    public AsyncFrameworkException(ErrorCode errorCode, String message, String requestId,
                                   String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.context = context;
    }

    /**
     * Get the error code associated with this exception.
     *
     * @return error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Get the request ID associated with this exception.
     *
     * @return request ID, or null if not available
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Get additional context information.
     *
     * @return context information, or null if not available
     */
    public String getContext() {
        return context;
    }

    /**
     * Check if this exception represents a retryable error.
     *
     * @return true if retryable, false otherwise
     */
    public boolean isRetryable() {
        return errorCode != null && errorCode.isRetryable();
    }

    /**
     * Get the recommended retry delay for this exception.
     *
     * @return recommended delay in seconds
     */
    public int getRecommendedRetryDelaySeconds() {
        return errorCode != null ? errorCode.getRecommendedRetryDelaySeconds() : 30;
    }

    /**
     * Get the severity level of this exception.
     *
     * @return severity level
     */
    public ErrorCode.Severity getSeverity() {
        return errorCode != null ? errorCode.getSeverity() : ErrorCode.Severity.MEDIUM;
    }

    /**
     * Create a formatted error message with all available context.
     *
     * @return formatted error message
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();

        if (errorCode != null) {
            sb.append("[").append(errorCode.getCode()).append("] ");
        }

        sb.append(getMessage());

        if (requestId != null) {
            sb.append(" (Request: ").append(requestId).append(")");
        }

        if (context != null) {
            sb.append(" - ").append(context);
        }

        return sb.toString();
    }

    /**
     * Create exception from a generic throwable with error code inference.
     *
     * @param cause the underlying cause
     * @param requestId the request ID for context
     * @return AsyncFrameworkException with inferred error code
     */
    public static AsyncFrameworkException fromCause(Throwable cause, String requestId) {
        if (cause instanceof AsyncFrameworkException) {
            return (AsyncFrameworkException) cause;
        }

        ErrorCode errorCode = inferErrorCode(cause);
        String message = cause.getMessage() != null ? cause.getMessage() : "Unknown error";

        return new AsyncFrameworkException(errorCode, message, requestId, cause);
    }

    /**
     * Infer error code from throwable type and message.
     *
     * @param throwable the throwable to analyze
     * @return inferred error code
     */
    private static ErrorCode inferErrorCode(Throwable throwable) {
        if (throwable == null) {
            return ErrorCode.UNKNOWN_ERROR;
        }

        String className = throwable.getClass().getSimpleName().toLowerCase();
        String message = throwable.getMessage();
        String lowerMessage = message != null ? message.toLowerCase() : "";

        // Network and connectivity errors
        if (className.contains("connect") || className.contains("socket") ||
                lowerMessage.contains("connection") || lowerMessage.contains("network")) {
            return ErrorCode.NETWORK_ERROR;
        }

        // Timeout errors
        if (className.contains("timeout") || lowerMessage.contains("timeout") ||
                lowerMessage.contains("timed out")) {
            return ErrorCode.TIMEOUT_ERROR;
        }

        // Authentication/authorization errors
        if (className.contains("auth") || lowerMessage.contains("unauthorized") ||
                lowerMessage.contains("forbidden") || lowerMessage.contains("authentication")) {
            return ErrorCode.AUTHENTICATION_ERROR;
        }

        // Rate limiting
        if (lowerMessage.contains("rate limit") || lowerMessage.contains("throttl") ||
                lowerMessage.contains("too many requests")) {
            return ErrorCode.RATE_LIMIT_EXCEEDED;
        }

        // Service unavailable
        if (lowerMessage.contains("service unavailable") || lowerMessage.contains("503") ||
                className.contains("unavailable")) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }

        // Serialization errors
        if (className.contains("json") || className.contains("serializ") ||
                className.contains("marshal") || lowerMessage.contains("parse")) {
            return ErrorCode.SERIALIZATION_ERROR;
        }

        // Validation errors
        if (className.contains("validation") || className.contains("illegal") ||
                lowerMessage.contains("invalid") || lowerMessage.contains("validation")) {
            return ErrorCode.VALIDATION_ERROR;
        }

        // Default to processing error for runtime exceptions
        if (throwable instanceof RuntimeException) {
            return ErrorCode.PROCESSING_ERROR;
        }

        return ErrorCode.UNKNOWN_ERROR;
    }

    @Override
    public String toString() {
        return String.format("AsyncFrameworkException{errorCode=%s, requestId='%s', message='%s'}",
                errorCode, requestId, getMessage());
    }
}