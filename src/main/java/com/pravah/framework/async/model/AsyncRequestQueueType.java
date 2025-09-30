package com.pravah.framework.async.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration representing different queue types for asynchronous request processing.
 * Each queue type has an associated delay/polling interval.
 * This enum is extensible and supports custom queue types through registration.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestQueueType {

    /**
     * Immediate processing queue (no delay)
     */
    IMMEDIATE(0, "immediate", "Process immediately without delay"),

    /**
     * Short delay queue (30 seconds)
     */
    SHORT_DELAY(30, "short", "Process after 30 seconds"),

    /**
     * Medium delay queue (3 minutes)
     */
    MEDIUM_DELAY(180, "medium", "Process after 3 minutes"),

    /**
     * Long delay queue (10 minutes)
     */
    LONG_DELAY(600, "long", "Process after 10 minutes"),

    /**
     * Extended delay queue (30 minutes)
     */
    EXTENDED_DELAY(1800, "extended", "Process after 30 minutes"),

    /**
     * Hourly processing queue (1 hour)
     */
    HOURLY(3600, "hourly", "Process after 1 hour"),

    /**
     * Daily processing queue (24 hours)
     */
    DAILY(86400, "daily", "Process after 24 hours"),

    /**
     * Default priority queue
     */
    DEFAULT(0, "default", "Default priority queue"),

    /**
     * High priority queue
     */
    HIGH_PRIORITY(0, "high-priority", "High priority queue"),

    /**
     * Low priority queue
     */
    LOW_PRIORITY(0, "low-priority", "Low priority queue"),

    // Legacy queue types for backward compatibility
    /**
     * Legacy 3 minutes queue
     * @deprecated Use {@link #MEDIUM_DELAY} instead
     */
    @Deprecated
    THREE_MINS(180, "three_mins", "Legacy 3 minutes queue"),

    /**
     * Legacy 10 minutes queue
     * @deprecated Use {@link #LONG_DELAY} instead
     */
    @Deprecated
    TEN_MINS(600, "ten_mins", "Legacy 10 minutes queue");

    private final int delaySeconds;
    private final String queueName;
    private final String description;

    // Static registry for custom queue types
    private static final Map<String, CustomAsyncRequestQueueType> CUSTOM_QUEUES = new ConcurrentHashMap<>();
    private static final Map<String, AsyncRequestQueueType> QUEUE_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AsyncRequestQueueType::getQueueName, Function.identity()));
    private static final Map<Integer, AsyncRequestQueueType> DELAY_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(
                AsyncRequestQueueType::getDelaySeconds, 
                Function.identity(),
                (existing, replacement) -> existing // Keep the first one in case of duplicates
            ));

    AsyncRequestQueueType(int delaySeconds, String queueName, String description) {
        this.delaySeconds = delaySeconds;
        this.queueName = queueName;
        this.description = description;
    }

    /**
     * Gets the delay in seconds for this queue type
     * @return delay in seconds
     */
    public int getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Gets the delay as a Duration object
     * @return delay as Duration
     */
    public Duration getDelay() {
        return Duration.ofSeconds(delaySeconds);
    }

    /**
     * Gets the queue name identifier
     * @return the queue name
     */
    @JsonValue
    public String getQueueName() {
        return queueName;
    }

    /**
     * Gets the human-readable description of the queue type
     * @return the queue description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the parameter name for this queue type
     * @return the parameter name (delay in seconds as string)
     */
    public String getParamName() {
        return String.valueOf(delaySeconds);
    }

    /**
     * Legacy method for backward compatibility
     * @return delay in minutes
     * @deprecated Use {@link #getDelaySeconds()} instead
     */
    @Deprecated
    public int getMins() {
        return delaySeconds / 60;
    }

    /**
     * Returns the string representation of this enum
     * @return the queue name
     */
    @Override
    public String toString() {
        return this.queueName;
    }

    /**
     * Creates an AsyncRequestQueueType from a string value.
     * Supports both built-in and custom registered queue types.
     *
     * @param value the string value to convert (queue name)
     * @return the corresponding AsyncRequestQueueType, or null if not found
     */
    @JsonCreator
    public static AsyncRequestQueueType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.toLowerCase().trim();

        // Check built-in queue types first
        AsyncRequestQueueType builtInQueue = QUEUE_LOOKUP.get(normalizedValue);
        if (builtInQueue != null) {
            return builtInQueue;
        }

        // Check custom registered queue types - return null since we can't extend enums
        return CUSTOM_QUEUES.containsKey(normalizedValue) ? null : null;
    }

    /**
     * Creates an AsyncRequestQueueType from delay in seconds
     * @param delaySeconds the delay in seconds
     * @return the corresponding AsyncRequestQueueType, or null if not found
     */
    public static AsyncRequestQueueType fromDelaySeconds(int delaySeconds) {
        // Check built-in queue types first
        AsyncRequestQueueType builtInQueue = DELAY_LOOKUP.get(delaySeconds);
        if (builtInQueue != null) {
            return builtInQueue;
        }

        // Check custom registered queue types - but can't return them as enum values
        return null;
    }

    /**
     * Creates an AsyncRequestQueueType from delay in minutes (legacy support)
     * @param delayMinutes the delay in minutes
     * @return the corresponding AsyncRequestQueueType, or null if not found
     * @deprecated Use {@link #fromDelaySeconds(int)} instead
     */
    @Deprecated
    public static AsyncRequestQueueType fromDelayMinutes(int delayMinutes) {
        return fromDelaySeconds(delayMinutes * 60);
    }

    /**
     * Validates if a string represents a valid AsyncRequestQueueType
     * @param value the string value to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        return fromString(value) != null;
    }

    /**
     * Validates if a delay in seconds represents a valid AsyncRequestQueueType
     * @param delaySeconds the delay in seconds to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidDelay(int delaySeconds) {
        return fromDelaySeconds(delaySeconds) != null;
    }

    /**
     * Registers a custom AsyncRequestQueueType at runtime.
     * This allows applications to extend the framework with their own queue types.
     *
     * @param queueName the string identifier for the custom queue
     * @param delaySeconds the delay in seconds for this queue type
     * @param description the human-readable description of the queue type
     * @return a new CustomAsyncRequestQueueType instance for the custom queue
     * @throws IllegalArgumentException if the queue is already registered or invalid
     */
    public static CustomAsyncRequestQueueType registerCustomQueue(String queueName, int delaySeconds, String description) {
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom queue name cannot be null or empty");
        }

        if (delaySeconds < 0) {
            throw new IllegalArgumentException("Delay seconds cannot be negative");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue description cannot be null or empty");
        }

        String normalizedQueueName = queueName.toLowerCase().trim();

        // Check if it conflicts with built-in queue types
        if (QUEUE_LOOKUP.containsKey(normalizedQueueName)) {
            throw new IllegalArgumentException("Cannot register custom queue '" + queueName + "' - conflicts with built-in queue");
        }

        // Check if already registered
        if (CUSTOM_QUEUES.containsKey(normalizedQueueName)) {
            throw new IllegalArgumentException("Custom queue '" + queueName + "' is already registered");
        }

        // Check if delay conflicts with existing queues
        if (DELAY_LOOKUP.containsKey(delaySeconds) ||
                CUSTOM_QUEUES.values().stream().anyMatch(q -> q.getDelaySeconds() == delaySeconds)) {
            throw new IllegalArgumentException("Cannot register custom queue with delay " + delaySeconds + " seconds - conflicts with existing queue");
        }

        // Create and register the custom queue
        CustomAsyncRequestQueueType customQueue = new CustomAsyncRequestQueueType(normalizedQueueName, delaySeconds, description.trim());
        CUSTOM_QUEUES.put(normalizedQueueName, customQueue);
        return customQueue;
    }

    /**
     * Gets all registered custom queue types
     * @return a map of custom queue names to CustomAsyncRequestQueueType instances
     */
    public static Map<String, CustomAsyncRequestQueueType> getCustomQueues() {
        return Map.copyOf(CUSTOM_QUEUES);
    }

    /**
     * Gets a custom queue by name
     * @param queueName the queue name to look up
     * @return the CustomAsyncRequestQueueType if found, null otherwise
     */
    public static CustomAsyncRequestQueueType getCustomQueue(String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            return null;
        }
        return CUSTOM_QUEUES.get(queueName.toLowerCase().trim());
    }

    /**
     * Checks if this is a custom registered queue type
     * @return true if this is a custom queue, false if built-in
     */
    public boolean isCustomQueue() {
        return false; // Built-in enum values are never custom
    }

    /**
     * Gets all available queue names including custom ones
     * @return array of all queue identifiers (both built-in and custom)
     */
    public static String[] allQueueNames() {
        String[] builtIn = Arrays.stream(values()).map(AsyncRequestQueueType::getQueueName).toArray(String[]::new);
        String[] custom = CUSTOM_QUEUES.keySet().toArray(new String[0]);

        String[] all = new String[builtIn.length + custom.length];
        System.arraycopy(builtIn, 0, all, 0, builtIn.length);
        System.arraycopy(custom, 0, all, builtIn.length, custom.length);

        return all;
    }

    /**
     * Gets the most appropriate queue type for a given delay
     * @param delaySeconds the desired delay in seconds
     * @return the closest matching queue type
     */
    public static AsyncRequestQueueType getBestMatchForDelay(int delaySeconds) {
        if (delaySeconds < 0) {
            return IMMEDIATE;
        }

        // First check for exact match
        AsyncRequestQueueType exactMatch = fromDelaySeconds(delaySeconds);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Find the closest match (prefer slightly longer delays for safety)
        AsyncRequestQueueType[] builtInQueues = values();
        CustomAsyncRequestQueueType[] customQueues = CUSTOM_QUEUES.values().toArray(new CustomAsyncRequestQueueType[0]);
        AsyncRequestQueueType bestMatch = IMMEDIATE;
        int smallestDifference = Integer.MAX_VALUE;

        // Check built-in queues
        for (AsyncRequestQueueType queue : builtInQueues) {
            int difference = Math.abs(queue.getDelaySeconds() - delaySeconds);
            if (difference < smallestDifference ||
                    (difference == smallestDifference && queue.getDelaySeconds() >= delaySeconds)) {
                smallestDifference = difference;
                bestMatch = queue;
            }
        }

        // Check custom queues (but can only return built-in matches)
        for (CustomAsyncRequestQueueType customQueue : customQueues) {
            int difference = Math.abs(customQueue.getDelaySeconds() - delaySeconds);
            if (difference < smallestDifference ||
                    (difference == smallestDifference && customQueue.getDelaySeconds() >= delaySeconds)) {
                // For custom queues, we find the closest built-in match
                smallestDifference = difference;
                // Keep the current bestMatch as we can't return custom types
            }
        }

        return bestMatch;
    }
}