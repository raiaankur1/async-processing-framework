package com.pravah.framework.async.model;

/**
 * Enhanced enumeration representing the various states of an async request.
 * <br>
 * This enum provides a comprehensive set of statuses to track the lifecycle
 * of asynchronous requests from creation to completion or failure.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestStatus {

    /**
     * Request has been created but not yet queued for processing.
     */
    CREATED("CREATED", "Request created"),

    /**
     * Request has been queued for processing.
     */
    QUEUED("QUEUED", "Request queued for processing"),

    /**
     * Request is currently being processed.
     */
    PROCESSING("PROCESSING", "Request is being processed"),

    /**
     * Request processing completed successfully.
     */
    COMPLETED("COMPLETED", "Request completed successfully"),

    /**
     * Request processing failed and will not be retried.
     */
    FAILED("FAILED", "Request processing failed"),

    /**
     * Request is being retried after a failure.
     */
    RETRYING("RETRYING", "Request is being retried"),

    /**
     * Request has exhausted all retry attempts and moved to dead letter queue.
     */
    DEAD_LETTER("DEAD_LETTER", "Request moved to dead letter queue");

    private final String status;
    private final String description;

    AsyncRequestStatus(String status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * Get the string representation of the status.
     *
     * @return status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get the human-readable description of the status.
     *
     * @return status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this status represents a terminal state (no further processing).
     *
     * @return true if terminal, false otherwise
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == DEAD_LETTER;
    }

    /**
     * Check if this status represents an active processing state.
     *
     * @return true if actively processing, false otherwise
     */
    public boolean isActive() {
        return this == PROCESSING || this == RETRYING;
    }

    /**
     * Check if this status represents a successful completion.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Check if this status represents a failure state.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return this == FAILED || this == DEAD_LETTER;
    }

    /**
     * Parse status from string value.
     *
     * @param value string value to parse
     * @return corresponding AsyncRequestStatus
     * @throws IllegalArgumentException if value is not recognized
     */
    public static AsyncRequestStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status value cannot be null");
        }

        for (AsyncRequestStatus status : values()) {
            if (status.status.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown status: " + value);
    }

    @Override
    public String toString() {
        return status;
    }
}