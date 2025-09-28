package com.pravah.framework.async.model;

public class CustomAsyncRequestAPI {
    private final String api;
    private final String description;
    private final long defaultTimeoutMs;
    private final boolean supportsBatchOperations;
    private final boolean requiresAuthentication;
    private final String retryStrategy;

    public CustomAsyncRequestAPI(String api, String description, long defaultTimeoutMs,
                                 boolean supportsBatchOperations, boolean requiresAuthentication, String retryStrategy) {
        this.api = api;
        this.description = description;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.supportsBatchOperations = supportsBatchOperations;
        this.requiresAuthentication = requiresAuthentication;
        this.retryStrategy = retryStrategy;
    }

    public String getApi() { return api; }
    public String getDescription() { return description; }
    public long getDefaultTimeoutMs() { return defaultTimeoutMs; }
    public boolean supportsBatchOperations() { return supportsBatchOperations; }
    public boolean requiresAuthentication() { return requiresAuthentication; }
    public String getRetryStrategy() { return retryStrategy; }
    public String getParamName() { return "api_" + this.api.toLowerCase(); }
    public boolean isCustomAPI() { return true; }

    @Override
    public String toString() { return this.api; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomAsyncRequestAPI that = (CustomAsyncRequestAPI) obj;
        return api.equals(that.api);
    }

    @Override
    public int hashCode() { return api.hashCode(); }
}