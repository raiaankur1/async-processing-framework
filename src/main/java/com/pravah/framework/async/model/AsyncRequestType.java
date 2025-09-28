package com.pravah.framework.async.model;

/**
 * Enhanced enumeration representing different types of async requests.
 * <br>
 * This enum provides extensibility support for different request types
 * and includes metadata for each type.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestType {

    /**
     * LMS (Loan Management System) related requests.
     */
    LMS("lms", "Loan Management System requests"),

    /**
     * Payment processing requests.
     */
    PAYMENT("payment", "Payment processing requests"),

    /**
     * Notification delivery requests.
     */
    NOTIFICATION("notification", "Notification delivery requests"),

    /**
     * Document processing requests.
     */
    DOCUMENT("document", "Document processing requests"),

    /**
     * Credit bureau requests.
     */
    CREDIT_BUREAU("credit_bureau", "Credit bureau API requests"),

    /**
     * KYC (Know Your Customer) verification requests.
     */
    KYC("kyc", "KYC verification requests"),

    /**
     * Custom request type for extensibility.
     */
    CUSTOM("custom", "Custom request type");

    private final String type;
    private final String description;

    AsyncRequestType(String type, String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * Get the string representation of the request type.
     *
     * @return type string
     */
    public String getType() {
        return type;
    }

    /**
     * Get the human-readable description of the request type.
     *
     * @return type description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse request type from string value.
     *
     * @param value string value to parse
     * @return corresponding AsyncRequestType
     * @throws IllegalArgumentException if value is not recognized
     */
    public static AsyncRequestType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Request type value cannot be null");
        }

        for (AsyncRequestType type : values()) {
            if (type.type.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown request type: " + value);
    }

    /**
     * Check if this is a built-in request type.
     *
     * @return true if built-in, false if custom
     */
    public boolean isBuiltIn() {
        return this != CUSTOM;
    }

    /**
     * Get the default queue name for this request type.
     *
     * @return default queue name
     */
    public String getDefaultQueueName() {
        return "async-" + type.replace("_", "-") + "-queue";
    }

    /**
     * Get the default retry count for this request type.
     *
     * @return default retry count
     */
    public int getDefaultRetryCount() {
        return switch (this) {
            case PAYMENT -> 5; // Payment operations need more retries
            case CREDIT_BUREAU -> 4; // Credit bureau APIs can be flaky
            case NOTIFICATION -> 2; // Notifications are less critical
            default -> 3; // Default retry count
        };
    }

    /**
     * Get the default polling delay for this request type.
     *
     * @return default polling delay in seconds
     */
    public int getDefaultPollingDelaySeconds() {
        return switch (this) {
            case PAYMENT -> 60; // Payment operations might take longer
            case DOCUMENT -> 120; // Document processing can be slow
            case CREDIT_BUREAU -> 45; // Credit bureau APIs have rate limits
            default -> 30; // Default delay
        };
    }

    @Override
    public String toString() {
        return type;
    }
}