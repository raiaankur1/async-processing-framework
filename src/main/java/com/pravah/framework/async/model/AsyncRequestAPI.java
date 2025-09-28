package com.pravah.framework.async.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration representing different APIs that can be called as part of asynchronous requests.
 * This enum is extensible and supports custom API types through registration.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestAPI {

    /**
     * Loan Management System API
     */
    LMS("LMS", "Loan Management System API", 30000L, false, true, "exponential_backoff"),

    /**
     * Payment Gateway API
     */
    PAYMENT_GATEWAY("PAYMENT_GATEWAY", "Payment Gateway API", 15000L, true, true, "linear_backoff"),

    /**
     * Notification Service API
     */
    NOTIFICATION_SERVICE("NOTIFICATION_SERVICE", "Notification Service API", 10000L, true, false, "immediate_retry"),

    /**
     * Credit Bureau API
     */
    CREDIT_BUREAU("CREDIT_BUREAU", "Credit Bureau API", 45000L, false, true, "exponential_backoff"),

    /**
     * Document Verification API
     */
    DOCUMENT_VERIFICATION("DOCUMENT_VERIFICATION", "Document Verification API", 60000L, false, true, "exponential_backoff"),

    /**
     * KYC Verification API
     */
    KYC_VERIFICATION("KYC_VERIFICATION", "KYC Verification API", 30000L, false, true, "exponential_backoff");

    private final String api;
    private final String description;
    private final long defaultTimeoutMs;
    private final boolean supportsBatchOperations;
    private final boolean requiresAuthentication;
    private final String retryStrategy;

    // Static registry for custom APIs
    private static final Map<String, CustomAsyncRequestAPI> CUSTOM_APIS = new ConcurrentHashMap<>();
    private static final Map<String, AsyncRequestAPI> API_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AsyncRequestAPI::getApi, Function.identity()));

    AsyncRequestAPI(String api, String description, long defaultTimeoutMs,
                    boolean supportsBatchOperations, boolean requiresAuthentication, String retryStrategy) {
        this.api = api;
        this.description = description;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.supportsBatchOperations = supportsBatchOperations;
        this.requiresAuthentication = requiresAuthentication;
        this.retryStrategy = retryStrategy;
    }

    /**
     * Gets the API identifier
     * @return the API string
     */
    @JsonValue
    public String getApi() {
        return api;
    }

    /**
     * Gets the human-readable description of the API
     * @return the API description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the default timeout in milliseconds for this API
     * @return the default timeout in milliseconds
     */
    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    /**
     * Checks if this API supports batch operations
     * @return true if batch operations are supported, false otherwise
     */
    public boolean supportsBatchOperations() {
        return supportsBatchOperations;
    }

    /**
     * Checks if this API requires authentication
     * @return true if authentication is required, false otherwise
     */
    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    /**
     * Gets the retry strategy for this API
     * @return the retry strategy identifier
     */
    public String getRetryStrategy() {
        return retryStrategy;
    }

    /**
     * Gets the parameter name for this API in the format "api_{lowercase_name}"
     * @return the parameter name
     */
    public String getParamName() {
        return "api_" + this.api.toLowerCase();
    }

    /**
     * Returns the string representation of this enum
     * @return the API string
     */
    @Override
    public String toString() {
        return this.api;
    }

    /**
     * Creates an AsyncRequestAPI from a string value.
     * Supports both built-in and custom registered APIs.
     *
     * @param value the string value to convert
     * @return the corresponding AsyncRequestAPI, or null if not found
     */
    @JsonCreator
    public static AsyncRequestAPI fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.toUpperCase().trim();

        // Check built-in APIs first
        AsyncRequestAPI builtInApi = API_LOOKUP.get(normalizedValue);
        if (builtInApi != null) {
            return builtInApi;
        }

        // For custom APIs, we need to check if they exist but return null since we can't extend enums
        // The custom API information can be retrieved separately using getCustomAPI method
        return CUSTOM_APIS.containsKey(normalizedValue) ? null : null;
    }

    /**
     * Gets a custom API by name
     * @param apiName the API name to look up
     * @return the CustomAsyncRequestAPI if found, null otherwise
     */
    public static CustomAsyncRequestAPI getCustomAPI(String apiName) {
        if (apiName == null || apiName.trim().isEmpty()) {
            return null;
        }
        return CUSTOM_APIS.get(apiName.toUpperCase().trim());
    }

    /**
     * Validates if a string represents a valid AsyncRequestAPI (built-in only)
     * @param value the string value to validate
     * @return true if valid built-in API, false otherwise
     */
    public static boolean isValid(String value) {
        return fromString(value) != null;
    }

    /**
     * Validates if a string represents either a built-in or custom API
     * @param value the string value to validate
     * @return true if valid (built-in or custom), false otherwise
     */
    public static boolean isValidIncludingCustom(String value) {
        return isValid(value) || getCustomAPI(value) != null;
    }

    /**
     * Registers a custom AsyncRequestAPI at runtime.
     * This allows applications to extend the framework with their own API types.
     *
     * @param api the string identifier for the custom API
     * @param description the human-readable description of the API
     * @return a new CustomAsyncRequestAPI instance for the custom API
     * @throws IllegalArgumentException if the API is already registered or invalid
     */
    public static CustomAsyncRequestAPI registerCustomAPI(String api, String description) {
        return registerCustomAPI(api, description, 30000L, false, true, "exponential_backoff");
    }

    /**
     * Registers a custom AsyncRequestAPI at runtime with full configuration.
     * This allows applications to extend the framework with their own API types.
     *
     * @param api the string identifier for the custom API
     * @param description the human-readable description of the API
     * @param defaultTimeoutMs the default timeout in milliseconds
     * @param supportsBatchOperations whether the API supports batch operations
     * @param requiresAuthentication whether the API requires authentication
     * @param retryStrategy the retry strategy identifier
     * @return a new CustomAsyncRequestAPI instance for the custom API
     * @throws IllegalArgumentException if the API is already registered or invalid
     */
    public static CustomAsyncRequestAPI registerCustomAPI(String api, String description, long defaultTimeoutMs,
                                                          boolean supportsBatchOperations, boolean requiresAuthentication,
                                                          String retryStrategy) {
        if (api == null || api.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom API identifier cannot be null or empty");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("API description cannot be null or empty");
        }

        if (defaultTimeoutMs <= 0) {
            throw new IllegalArgumentException("Default timeout must be positive");
        }

        if (retryStrategy == null || retryStrategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Retry strategy cannot be null or empty");
        }

        String normalizedApi = api.toUpperCase().trim();

        // Check if it conflicts with built-in APIs
        if (API_LOOKUP.containsKey(normalizedApi)) {
            throw new IllegalArgumentException("Cannot register custom API '" + api + "' - conflicts with built-in API");
        }

        // Check if already registered
        if (CUSTOM_APIS.containsKey(normalizedApi)) {
            throw new IllegalArgumentException("Custom API '" + api + "' is already registered");
        }

        // Create and register the custom API
        CustomAsyncRequestAPI customApi = new CustomAsyncRequestAPI(normalizedApi, description.trim(), defaultTimeoutMs,
                supportsBatchOperations, requiresAuthentication, retryStrategy.trim());

        CUSTOM_APIS.put(normalizedApi, customApi);
        return customApi;
    }

    /**
     * Gets all registered custom APIs
     * @return a map of custom API names to CustomAsyncRequestAPI instances
     */
    public static Map<String, CustomAsyncRequestAPI> getCustomAPIs() {
        return Map.copyOf(CUSTOM_APIS);
    }

    /**
     * Checks if this is a custom registered API
     * @return true if this is a custom API, false if built-in
     */
    public boolean isCustomAPI() {
        return false; // Built-in enum values are never custom
    }

    /**
     * Gets all available API names including custom ones
     * @return array of all API identifiers (both built-in and custom)
     */
    public static String[] allAPINames() {
        String[] builtIn = Arrays.stream(values()).map(AsyncRequestAPI::getApi).toArray(String[]::new);
        String[] custom = CUSTOM_APIS.keySet().toArray(new String[0]);

        String[] all = new String[builtIn.length + custom.length];
        System.arraycopy(builtIn, 0, all, 0, builtIn.length);
        System.arraycopy(custom, 0, all, builtIn.length, custom.length);

        return all;
    }

    /**
     * Creates a standardized parameter name for API status tracking
     * @param api the API to create parameter name for
     * @return standardized parameter name
     */
    public static String createParamName(AsyncRequestAPI api) {
        if (api == null) {
            throw new IllegalArgumentException("API cannot be null");
        }
        return api.getParamName();
    }
}