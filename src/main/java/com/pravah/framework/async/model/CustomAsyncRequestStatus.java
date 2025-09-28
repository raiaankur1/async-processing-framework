package com.pravah.framework.async.model;

/**
 * Represents a custom AsyncRequestStatus that can be registered at runtime.
 * This class provides the same interface as AsyncRequestStatus but for custom statuses.
 */
public class CustomAsyncRequestStatus {

    private final String status;
    private final String description;
    private final boolean terminal;
    private final boolean error;

    /**
     * Creates a new custom async request status
     * @param status the status identifier
     * @param description the human-readable description
     * @param terminal whether this status is terminal
     * @param error whether this status represents an error
     */
    public CustomAsyncRequestStatus(String status, String description, boolean terminal, boolean error) {
        this.status = status;
        this.description = description;
        this.terminal = terminal;
        this.error = error;
    }

    /**
     * Gets the status string
     * @return the status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Gets the human-readable description of the status
     * @return the status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status represents a terminal state (no further processing)
     * @return true if terminal, false otherwise
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Checks if this status represents an error state
     * @return true if error, false otherwise
     */
    public boolean isError() {
        return error;
    }

    /**
     * Checks if this status represents an active processing state
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return !terminal;
    }

    /**
     * Checks if this status represents a successful completion
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return terminal && !error;
    }

    /**
     * Returns the string representation of this custom status
     * @return the status string
     */
    @Override
    public String toString() {
        return this.status;
    }

    /**
     * Gets the name of this custom status (for compatibility with enum-like behavior)
     * @return the custom status name
     */
    public String name() {
        return "CUSTOM_" + status;
    }

    /**
     * Checks if this is a custom registered status
     * @return always true for CustomAsyncRequestStatus instances
     */
    public boolean isCustomStatus() {
        return true;
    }

    /**
     * Creates a pseudo-AsyncRequestStatus that represents this custom status
     * For custom statuses, we return a generic status based on the characteristics
     * @return appropriate AsyncRequestStatus based on terminal/error flags
     */
    public AsyncRequestStatus asAsyncRequestStatus() {
        if (terminal && error) {
            return AsyncRequestStatus.FAILED;
        } else if (terminal) {
            return AsyncRequestStatus.COMPLETED;
        } else {
            return AsyncRequestStatus.PROCESSING;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomAsyncRequestStatus that = (CustomAsyncRequestStatus) obj;
        return status.equals(that.status);
    }

    @Override
    public int hashCode() {
        return status.hashCode();
    }
}