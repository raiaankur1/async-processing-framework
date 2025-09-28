package com.pravah.framework.async.model;

/**
 * Represents a custom AsyncRequestType that can be registered at runtime.
 * This class provides the same interface as AsyncRequestType but for custom types.
 */
public class CustomAsyncRequestType {

    private final String type;
    private final String implementationClassName;

    /**
     * Creates a new custom async request type
     * @param type the type identifier
     * @param implementationClassName the implementation class name
     */
    public CustomAsyncRequestType(String type, String implementationClassName) {
        this.type = type;
        this.implementationClassName = implementationClassName;
    }

    /**
     * Gets the string representation of the request type
     * @return the type string
     */
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
     * Returns the string representation of this custom type
     * @return the type string
     */
    @Override
    public String toString() {
        return this.type;
    }

    /**
     * Gets the name of this custom type (for compatibility with enum-like behavior)
     * @return the custom type name
     */
    public String name() {
        return "CUSTOM_" + type.toUpperCase();
    }

    /**
     * Checks if this is a custom registered type
     * @return always true for CustomAsyncRequestType instances
     */
    public boolean isCustomType() {
        return true;
    }

    /**
     * Creates a pseudo-AsyncRequestType that behaves like the CUSTOM enum value
     * but carries the custom type information
     * @return AsyncRequestType.CUSTOM
     */
    public AsyncRequestType asAsyncRequestType() {
        return AsyncRequestType.CUSTOM;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomAsyncRequestType that = (CustomAsyncRequestType) obj;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}