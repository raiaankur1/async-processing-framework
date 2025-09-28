package com.pravah.framework.async.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration representing the status of asynchronous requests.
 * This enum is extensible and supports custom status types through registration.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestStatus {

    /**
     * Request has been created but not yet queued for processing
     */
    CREATED("CREATED", "Request has been created", false, false),

    /**
     * Request has been queued for processing
     */
    QUEUED("QUEUED", "Request is queued for processing", false, false),

    /**
     * Request is currently being processed
     */
    PROCESSING("PROCESSING", "Request is being processed", false, false),

    /**
     * Request has been completed successfully
     */
    COMPLETED("COMPLETED", "Request completed successfully", true, false),

    /**
     * Request processing is in progress (legacy status)
     * @deprecated Use {@link #PROCESSING} instead
     */
    @Deprecated
    INPROCESS("INPROCESS", "Request is in process", false, false),

    /**
     * Request has failed and will be retried
     */
    RETRYING("RETRYING", "Request failed and will be retried", false, false),

    /**
     * Request has failed permanently
     */
    FAILED("FAILED", "Request has failed permanently", true, true),

    /**
     * Request has been sent to dead letter queue after exhausting retries
     */
    DEAD_LETTER("DEAD_LETTER", "Request sent to dead letter queue", true, true),

    /**
     * Request has been cancelled by user or system
     */
    CANCELLED("CANCELLED", "Request has been cancelled", true, false),

    /**
     * Request has timed out
     */
    TIMEOUT("TIMEOUT", "Request has timed out", true, true);

    private final String status;
    private final String description;
    private final boolean terminal;
    private final boolean error;

    // Static registry for custom statuses
    private static final Map<String, CustomAsyncRequestStatus> CUSTOM_STATUSES = new ConcurrentHashMap<>();
    private static final Map<String, AsyncRequestStatus> STATUS_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AsyncRequestStatus::getStatus, Function.identity()));

    // Pre-computed sets for performance
    private static final Set<AsyncRequestStatus> TERMINAL_STATUSES = Arrays.stream(values())
            .filter(AsyncRequestStatus::isTerminal)
            .collect(Collectors.toSet());

    private static final Set<AsyncRequestStatus> ERROR_STATUSES = Arrays.stream(values())
            .filter(AsyncRequestStatus::isError)
            .collect(Collectors.toSet());

    private static final Set<AsyncRequestStatus> ACTIVE_STATUSES = Arrays.stream(values())
            .filter(status -> !status.isTerminal())
            .collect(Collectors.toSet());

    AsyncRequestStatus(String status, String description, boolean terminal, boolean error) {
        this.status = status;
        this.description = description;
        this.terminal = terminal;
        this.error = error;
    }

    /**
     * Gets the status string
     * @return the status string
     */
    @JsonValue
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
     * Returns the string representation of this enum
     * @return the status string
     */
    @Override
    public String toString() {
        return this.status;
    }

    /**
     * Creates an AsyncRequestStatus from a string value.
     * Supports both built-in and custom registered statuses.
     *
     * @param value the string value to convert
     * @return the corresponding AsyncRequestStatus, or null if not found
     */
    @JsonCreator
    public static AsyncRequestStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.toUpperCase().trim();

        // Check built-in statuses first
        return STATUS_LOOKUP.get(normalizedValue);

        // For custom statuses, we need to check if they exist but return null since we can't extend enums
    }

    /**
     * Gets a custom status by name
     * @param statusName the status name to look up
     * @return the CustomAsyncRequestStatus if found, null otherwise
     */
    public static CustomAsyncRequestStatus getCustomStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            return null;
        }
        return CUSTOM_STATUSES.get(statusName.toUpperCase().trim());
    }

    /**
     * Validates if a string represents a valid AsyncRequestStatus (built-in only)
     * @param value the string value to validate
     * @return true if valid built-in status, false otherwise
     */
    public static boolean isValid(String value) {
        return fromString(value) != null;
    }

    /**
     * Validates if a string represents either a built-in or custom status
     * @param value the string value to validate
     * @return true if valid (built-in or custom), false otherwise
     */
    public static boolean isValidIncludingCustom(String value) {
        return isValid(value) || getCustomStatus(value) != null;
    }

    /**
     * Registers a custom AsyncRequestStatus at runtime.
     * This allows applications to extend the framework with their own status types.
     *
     * @param status the string identifier for the custom status
     * @param description the human-readable description of the status
     * @param terminal whether this status is terminal (no further processing)
     * @param error whether this status represents an error state
     * @return a new CustomAsyncRequestStatus instance for the custom status
     * @throws IllegalArgumentException if the status is already registered or invalid
     */
    public static CustomAsyncRequestStatus registerCustomStatus(String status, String description,
                                                                boolean terminal, boolean error) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom status identifier cannot be null or empty");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Status description cannot be null or empty");
        }

        String normalizedStatus = status.toUpperCase().trim();

        // Check if it conflicts with built-in statuses
        if (STATUS_LOOKUP.containsKey(normalizedStatus)) {
            throw new IllegalArgumentException("Cannot register custom status '" + status + "' - conflicts with built-in status");
        }

        // Check if already registered
        if (CUSTOM_STATUSES.containsKey(normalizedStatus)) {
            throw new IllegalArgumentException("Custom status '" + status + "' is already registered");
        }

        // Create and register the custom status
        CustomAsyncRequestStatus customStatus = new CustomAsyncRequestStatus(normalizedStatus, description.trim(), terminal, error);
        CUSTOM_STATUSES.put(normalizedStatus, customStatus);
        return customStatus;
    }

    /**
     * Gets all registered custom statuses
     * @return a map of custom status names to CustomAsyncRequestStatus instances
     */
    public static Map<String, CustomAsyncRequestStatus> getCustomStatuses() {
        return Map.copyOf(CUSTOM_STATUSES);
    }

    /**
     * Checks if this is a custom registered status
     * @return true if this is a custom status, false if built-in
     */
    public boolean isCustomStatus() {
        return false; // Built-in enum values are never custom
    }

    /**
     * Gets all available status names including custom ones
     * @return array of all status identifiers (both built-in and custom)
     */
    public static String[] allStatusNames() {
        String[] builtIn = Arrays.stream(values()).map(AsyncRequestStatus::getStatus).toArray(String[]::new);
        String[] custom = CUSTOM_STATUSES.keySet().toArray(new String[0]);

        String[] all = new String[builtIn.length + custom.length];
        System.arraycopy(builtIn, 0, all, 0, builtIn.length);
        System.arraycopy(custom, 0, all, builtIn.length, custom.length);

        return all;
    }

    /**
     * Gets all terminal statuses (including custom ones)
     * @return set of terminal statuses
     */
    public static Set<AsyncRequestStatus> getTerminalStatuses() {
        // Note: Custom statuses can't be returned as AsyncRequestStatus enum values
        return new HashSet<>(TERMINAL_STATUSES);
    }

    /**
     * Gets all error statuses (including custom ones)
     * @return set of error statuses
     */
    public static Set<AsyncRequestStatus> getErrorStatuses() {
        // Note: Custom statuses can't be returned as AsyncRequestStatus enum values
        return new HashSet<>(ERROR_STATUSES);
    }

    /**
     * Gets all active (non-terminal) statuses (including custom ones)
     * @return set of active statuses
     */
    public static Set<AsyncRequestStatus> getActiveStatuses() {
        // Note: Custom statuses can't be returned as AsyncRequestStatus enum values
        return new HashSet<>(ACTIVE_STATUSES);
    }

    /**
     * Checks if a status transition is valid
     * @param from the current status
     * @param to the target status
     * @return true if transition is valid, false otherwise
     */
    public static boolean isValidTransition(AsyncRequestStatus from, AsyncRequestStatus to) {
        if (from == null || to == null) {
            return false;
        }

        // Cannot transition from terminal states
        if (from.isTerminal()) {
            return false;
        }

        // Cannot transition to CREATED from any other state
        return to != CREATED || from == CREATED;
    }
}