package com.pravah.framework.async.model;

/**
 * Enhanced enumeration representing different APIs that async requests can call.
 * <br>
 * This enum provides metadata for each API and supports extensibility
 * for different API integrations.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestAPI {

    /**
     * Loan Management System API.
     */
    LMS("LMS", "Loan Management System API", "lms.api.endpoint"),

    /**
     * Payment Gateway API.
     */
    PAYMENT_GATEWAY("PAYMENT_GATEWAY", "Payment Gateway API", "payment.api.endpoint"),

    /**
     * Credit Bureau API (CIBIL, Experian, etc.).
     */
    CREDIT_BUREAU("CREDIT_BUREAU", "Credit Bureau API", "credit.bureau.api.endpoint"),

    /**
     * KYC Verification API.
     */
    KYC_VERIFICATION("KYC_VERIFICATION", "KYC Verification API", "kyc.api.endpoint"),

    /**
     * Document Processing API.
     */
    DOCUMENT_PROCESSING("DOCUMENT_PROCESSING", "Document Processing API", "document.api.endpoint"),

    /**
     * Notification Service API.
     */
    NOTIFICATION_SERVICE("NOTIFICATION_SERVICE", "Notification Service API", "notification.api.endpoint"),

    /**
     * Bank Account Verification API.
     */
    BANK_VERIFICATION("BANK_VERIFICATION", "Bank Account Verification API", "bank.verification.api.endpoint"),

    /**
     * PAN Verification API.
     */
    PAN_VERIFICATION("PAN_VERIFICATION", "PAN Verification API", "pan.verification.api.endpoint"),

    /**
     * Aadhaar Verification API.
     */
    AADHAAR_VERIFICATION("AADHAAR_VERIFICATION", "Aadhaar Verification API", "aadhaar.verification.api.endpoint"),

    /**
     * Custom API for extensibility.
     */
    CUSTOM("CUSTOM", "Custom API", "custom.api.endpoint");

    private final String api;
    private final String description;
    private final String configKey;

    AsyncRequestAPI(String api, String description, String configKey) {
        this.api = api;
        this.description = description;
        this.configKey = configKey;
    }

    /**
     * Get the string representation of the API.
     *
     * @return API string
     */
    public String getApi() {
        return api;
    }

    /**
     * Get the human-readable description of the API.
     *
     * @return API description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the configuration key for this API's endpoint.
     *
     * @return configuration key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Get the parameter name for this API in status tracking.
     *
     * @return parameter name
     */
    public String getParamName() {
        return "api_" + this.api.toLowerCase();
    }

    /**
     * Get the default timeout for this API in milliseconds.
     *
     * @return default timeout in milliseconds
     */
    public long getDefaultTimeoutMs() {
        switch (this) {
            case CREDIT_BUREAU:
                return 60000; // Credit bureau APIs can be slow
            case DOCUMENT_PROCESSING:
                return 120000; // Document processing takes time
            case BANK_VERIFICATION:
                return 45000; // Bank verification can be slow
            case KYC_VERIFICATION:
            case PAN_VERIFICATION:
            case AADHAAR_VERIFICATION:
                return 30000; // Verification APIs
            case PAYMENT_GATEWAY:
                return 20000; // Payment APIs should be fast
            case NOTIFICATION_SERVICE:
                return 15000; // Notifications should be quick
            default:
                return 30000; // Default timeout
        }
    }

    /**
     * Check if this API supports batch operations.
     *
     * @return true if batch operations are supported
     */
    public boolean supportsBatchOperations() {
        switch (this) {
            case NOTIFICATION_SERVICE:
            case DOCUMENT_PROCESSING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if this API requires authentication.
     *
     * @return true if authentication is required
     */
    public boolean requiresAuthentication() {
        // All APIs except custom require authentication by default
        return this != CUSTOM;
    }

    /**
     * Get the retry strategy for this API.
     *
     * @return retry strategy name
     */
    public String getRetryStrategy() {
        switch (this) {
            case PAYMENT_GATEWAY:
                return "EXPONENTIAL_BACKOFF_AGGRESSIVE";
            case CREDIT_BUREAU:
                return "EXPONENTIAL_BACKOFF_CONSERVATIVE";
            case NOTIFICATION_SERVICE:
                return "LINEAR_BACKOFF";
            default:
                return "EXPONENTIAL_BACKOFF_STANDARD";
        }
    }

    /**
     * Parse API from string value.
     *
     * @param value string value to parse
     * @return corresponding AsyncRequestAPI
     * @throws IllegalArgumentException if value is not recognized
     */
    public static AsyncRequestAPI fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("API value cannot be null");
        }

        for (AsyncRequestAPI api : values()) {
            if (api.api.equalsIgnoreCase(value)) {
                return api;
            }
        }

        throw new IllegalArgumentException("Unknown API: " + value);
    }

    @Override
    public String toString() {
        return api;
    }
}