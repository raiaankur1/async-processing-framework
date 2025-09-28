package com.pravah.framework.async.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration representing different types of asynchronous requests.
 * This enum is extensible and supports custom request types through registration.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public enum AsyncRequestType {

    /**
     * LMS (Loan Management System) request type
     */
    LMS("lms", "com.pravah.framework.async.impl.LMSAsyncRequest"),

    /**
     * Payment processing request type
     */
    PAYMENT("payment", "com.pravah.framework.async.impl.PaymentAsyncRequest"),

    /**
     * Notification request type
     */
    NOTIFICATION("notification", "com.pravah.framework.async.impl.NotificationAsyncRequest"),

    /**
     * Custom request type for user-defined implementations
     */
    CUSTOM("custom", "com.pravah.framework.async.impl.CustomAsyncRequest");

    private final String type;
    private final String implementationClassName;

    // Static registry for custom types
    private static final Map<String, CustomAsyncRequestType> CUSTOM_TYPES = new ConcurrentHashMap<>();
    private static final Map<String, AsyncRequestType> TYPE_LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(AsyncRequestType::getType, Function.identity()));

    AsyncRequestType(String type, String implementationClassName) {
        this.type = type;
        this.implementationClassName = implementationClassName;
    }

    /**
     * Gets the string representation of the request type
     * @return the type string
     */
    @JsonValue
    public String getType() {
        return type;
    }

    /**
     * Gets the implementation class name for this request type
     * @return the fully qualified class name
     */
    public String getImplementationClassName() {
        return implementationClassName;
    }

    /**
     * Returns the string representation of this enum
     * @return the type string
     */
    @Override
    public String toString() {
        return this.type;
    }

    /**
     * Creates an AsyncRequestType from a string value.
     * Supports both built-in and custom registered types.
     *
     * @param value the string value to convert
     * @return the corresponding AsyncRequestType, or null if not found
     */
    @JsonCreator
    public static AsyncRequestType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.toLowerCase().trim();

        // Check built-in types first
        AsyncRequestType builtInType = TYPE_LOOKUP.get(normalizedValue);
        if (builtInType != null) {
            return builtInType;
        }

        // Check custom registered types
        CustomAsyncRequestType customType = CUSTOM_TYPES.get(normalizedValue);
        return customType != null ? customType.asAsyncRequestType() : null;
    }

    /**
     * Legacy method for backward compatibility
     * @param value the string value to convert
     * @return the corresponding AsyncRequestType, or null if not found
     * @deprecated Use {@link #fromString(String)} instead
     */
    @Deprecated
    public static AsyncRequestType fromStringValue(String value) {
        return fromString(value);
    }

    /**
     * Validates if a string represents a valid AsyncRequestType
     * @param value the string value to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String value) {
        return fromString(value) != null;
    }

    /**
     * Registers a custom AsyncRequestType at runtime.
     * This allows applications to extend the framework with their own request types.
     *
     * @param type the string identifier for the custom type
     * @param implementationClassName the fully qualified class name of the implementation
     * @return a CustomAsyncRequestType instance for the custom type
     * @throws IllegalArgumentException if the type is already registered or invalid
     */
    public static CustomAsyncRequestType registerCustomType(String type, String implementationClassName) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom type cannot be null or empty");
        }

        if (implementationClassName == null || implementationClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("Implementation class name cannot be null or empty");
        }

        String normalizedType = type.toLowerCase().trim();

        // Check if it conflicts with built-in types
        if (TYPE_LOOKUP.containsKey(normalizedType)) {
            throw new IllegalArgumentException("Cannot register custom type '" + type + "' - conflicts with built-in type");
        }

        // Check if already registered
        if (CUSTOM_TYPES.containsKey(normalizedType)) {
            throw new IllegalArgumentException("Custom type '" + type + "' is already registered");
        }

        // Create and register the custom type
        CustomAsyncRequestType customType = new CustomAsyncRequestType(normalizedType, implementationClassName.trim());
        CUSTOM_TYPES.put(normalizedType, customType);
        return customType;
    }

    /**
     * Gets all registered custom types
     * @return a map of custom type names to CustomAsyncRequestType instances
     */
    public static Map<String, CustomAsyncRequestType> getCustomTypes() {
        return Map.copyOf(CUSTOM_TYPES);
    }

    /**
     * Checks if this is a custom registered type
     * @return true if this is a custom type, false if built-in
     */
    public boolean isCustomType() {
        return false; // Built-in enum values are never custom
    }

    /**
     * Gets all available AsyncRequestType values including custom ones
     * @return array of all type identifiers (both built-in and custom)
     */
    public static String[] allTypeNames() {
        String[] builtIn = Arrays.stream(values()).map(AsyncRequestType::getType).toArray(String[]::new);
        String[] custom = CUSTOM_TYPES.keySet().toArray(new String[0]);

        String[] all = new String[builtIn.length + custom.length];
        System.arraycopy(builtIn, 0, all, 0, builtIn.length);
        System.arraycopy(custom, 0, all, builtIn.length, custom.length);

        return all;
    }
}