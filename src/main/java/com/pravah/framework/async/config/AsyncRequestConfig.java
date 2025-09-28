package com.pravah.framework.async.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

/**
 * Configuration class for AsyncRequest behavior.
 * <br>
 * This class contains all configurable parameters that control
 * the behavior of async request processing, including retry logic,
 * payload handling, and timeouts.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public class AsyncRequestConfig {

    // Retry configuration
    @JsonProperty("initialRetryDelay")
    private Duration initialRetryDelay = Duration.ofSeconds(30);

    @JsonProperty("maxRetryDelay")
    private Duration maxRetryDelay = Duration.ofMinutes(10);

    @JsonProperty("retryBackoffMultiplier")
    private double retryBackoffMultiplier = 2.0;

    @JsonProperty("maxRetries")
    private int maxRetries = 3;

    @JsonProperty("enableJitter")
    private boolean enableJitter = true;

    // Payload configuration
    @JsonProperty("inlinePayloadThreshold")
    private int inlinePayloadThreshold = 1024; // 1KB default

    @JsonProperty("maxPayloadSize")
    private int maxPayloadSize = 10 * 1024 * 1024; // 10MB default

    @JsonProperty("compressPayload")
    private boolean compressPayload = false;

    // Timeout configuration
    @JsonProperty("processingTimeout")
    private Duration processingTimeout = Duration.ofMinutes(30);

    @JsonProperty("queueVisibilityTimeout")
    private Duration queueVisibilityTimeout = Duration.ofMinutes(5);

    // Monitoring configuration
    @JsonProperty("enableMetrics")
    private boolean enableMetrics = true;

    @JsonProperty("enableTracing")
    private boolean enableTracing = true;

    @JsonProperty("logPayload")
    private boolean logPayload = false;

    // Circuit breaker configuration
    @JsonProperty("enableCircuitBreaker")
    private boolean enableCircuitBreaker = true;

    @JsonProperty("circuitBreakerFailureThreshold")
    private int circuitBreakerFailureThreshold = 5;

    @JsonProperty("circuitBreakerRecoveryTimeout")
    private Duration circuitBreakerRecoveryTimeout = Duration.ofMinutes(1);

    /**
     * Default constructor with sensible defaults.
     */
    public AsyncRequestConfig() {
        // All defaults are set via field initialization
    }

    /**
     * Copy constructor.
     *
     * @param other configuration to copy from
     */
    public AsyncRequestConfig(AsyncRequestConfig other) {
        this.initialRetryDelay = other.initialRetryDelay;
        this.maxRetryDelay = other.maxRetryDelay;
        this.retryBackoffMultiplier = other.retryBackoffMultiplier;
        this.maxRetries = other.maxRetries;
        this.enableJitter = other.enableJitter;
        this.inlinePayloadThreshold = other.inlinePayloadThreshold;
        this.maxPayloadSize = other.maxPayloadSize;
        this.compressPayload = other.compressPayload;
        this.processingTimeout = other.processingTimeout;
        this.queueVisibilityTimeout = other.queueVisibilityTimeout;
        this.enableMetrics = other.enableMetrics;
        this.enableTracing = other.enableTracing;
        this.logPayload = other.logPayload;
        this.enableCircuitBreaker = other.enableCircuitBreaker;
        this.circuitBreakerFailureThreshold = other.circuitBreakerFailureThreshold;
        this.circuitBreakerRecoveryTimeout = other.circuitBreakerRecoveryTimeout;
    }

    // Getters and setters with fluent interface

    public Duration getInitialRetryDelay() {
        return initialRetryDelay;
    }

    public AsyncRequestConfig setInitialRetryDelay(Duration initialRetryDelay) {
        this.initialRetryDelay = initialRetryDelay;
        return this;
    }

    public Duration getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public AsyncRequestConfig setMaxRetryDelay(Duration maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
        return this;
    }

    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public AsyncRequestConfig setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public AsyncRequestConfig setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public boolean isEnableJitter() {
        return enableJitter;
    }

    public AsyncRequestConfig setEnableJitter(boolean enableJitter) {
        this.enableJitter = enableJitter;
        return this;
    }

    public int getInlinePayloadThreshold() {
        return inlinePayloadThreshold;
    }

    public AsyncRequestConfig setInlinePayloadThreshold(int inlinePayloadThreshold) {
        this.inlinePayloadThreshold = inlinePayloadThreshold;
        return this;
    }

    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public AsyncRequestConfig setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
        return this;
    }

    public boolean isCompressPayload() {
        return compressPayload;
    }

    public AsyncRequestConfig setCompressPayload(boolean compressPayload) {
        this.compressPayload = compressPayload;
        return this;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public AsyncRequestConfig setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
        return this;
    }

    public Duration getQueueVisibilityTimeout() {
        return queueVisibilityTimeout;
    }

    public AsyncRequestConfig setQueueVisibilityTimeout(Duration queueVisibilityTimeout) {
        this.queueVisibilityTimeout = queueVisibilityTimeout;
        return this;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public AsyncRequestConfig setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
        return this;
    }

    public boolean isEnableTracing() {
        return enableTracing;
    }

    public AsyncRequestConfig setEnableTracing(boolean enableTracing) {
        this.enableTracing = enableTracing;
        return this;
    }

    public boolean isLogPayload() {
        return logPayload;
    }

    public AsyncRequestConfig setLogPayload(boolean logPayload) {
        this.logPayload = logPayload;
        return this;
    }

    public boolean isEnableCircuitBreaker() {
        return enableCircuitBreaker;
    }

    public AsyncRequestConfig setEnableCircuitBreaker(boolean enableCircuitBreaker) {
        this.enableCircuitBreaker = enableCircuitBreaker;
        return this;
    }

    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public AsyncRequestConfig setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        return this;
    }

    public Duration getCircuitBreakerRecoveryTimeout() {
        return circuitBreakerRecoveryTimeout;
    }

    public AsyncRequestConfig setCircuitBreakerRecoveryTimeout(Duration circuitBreakerRecoveryTimeout) {
        this.circuitBreakerRecoveryTimeout = circuitBreakerRecoveryTimeout;
        return this;
    }

    /**
     * Validate the configuration parameters.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void validate() {
        if (initialRetryDelay == null || initialRetryDelay.isNegative()) {
            throw new IllegalArgumentException("Initial retry delay must be positive");
        }

        if (maxRetryDelay == null || maxRetryDelay.isNegative()) {
            throw new IllegalArgumentException("Max retry delay must be positive");
        }

        if (initialRetryDelay.compareTo(maxRetryDelay) > 0) {
            throw new IllegalArgumentException("Initial retry delay cannot be greater than max retry delay");
        }

        if (retryBackoffMultiplier <= 1.0) {
            throw new IllegalArgumentException("Retry backoff multiplier must be greater than 1.0");
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }

        if (inlinePayloadThreshold < 0) {
            throw new IllegalArgumentException("Inline payload threshold cannot be negative");
        }

        if (maxPayloadSize <= 0) {
            throw new IllegalArgumentException("Max payload size must be positive");
        }

        if (inlinePayloadThreshold > maxPayloadSize) {
            throw new IllegalArgumentException("Inline payload threshold cannot be greater than max payload size");
        }

        if (processingTimeout == null || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("Processing timeout must be positive");
        }

        if (queueVisibilityTimeout == null || queueVisibilityTimeout.isNegative()) {
            throw new IllegalArgumentException("Queue visibility timeout must be positive");
        }

        if (circuitBreakerFailureThreshold <= 0) {
            throw new IllegalArgumentException("Circuit breaker failure threshold must be positive");
        }

        if (circuitBreakerRecoveryTimeout == null || circuitBreakerRecoveryTimeout.isNegative()) {
            throw new IllegalArgumentException("Circuit breaker recovery timeout must be positive");
        }
    }

    /**
     * Create a configuration optimized for high-throughput scenarios.
     *
     * @return high-throughput configuration
     */
    public static AsyncRequestConfig forHighThroughput() {
        return new AsyncRequestConfig()
                .setInitialRetryDelay(Duration.ofSeconds(10))
                .setMaxRetryDelay(Duration.ofMinutes(2))
                .setRetryBackoffMultiplier(1.5)
                .setMaxRetries(2)
                .setInlinePayloadThreshold(512)
                .setProcessingTimeout(Duration.ofMinutes(10))
                .setEnableJitter(true)
                .setEnableCircuitBreaker(true)
                .setCircuitBreakerFailureThreshold(3);
    }

    /**
     * Create a configuration optimized for reliability scenarios.
     *
     * @return reliability-focused configuration
     */
    public static AsyncRequestConfig forReliability() {
        return new AsyncRequestConfig()
                .setInitialRetryDelay(Duration.ofMinutes(1))
                .setMaxRetryDelay(Duration.ofMinutes(30))
                .setRetryBackoffMultiplier(2.5)
                .setMaxRetries(5)
                .setInlinePayloadThreshold(2048)
                .setProcessingTimeout(Duration.ofHours(1))
                .setEnableJitter(true)
                .setEnableCircuitBreaker(false); // Disable for maximum retry attempts
    }

    /**
     * Create a configuration optimized for development/testing.
     *
     * @return development configuration
     */
    public static AsyncRequestConfig forDevelopment() {
        return new AsyncRequestConfig()
                .setInitialRetryDelay(Duration.ofSeconds(5))
                .setMaxRetryDelay(Duration.ofMinutes(1))
                .setRetryBackoffMultiplier(1.5)
                .setMaxRetries(1)
                .setInlinePayloadThreshold(4096)
                .setProcessingTimeout(Duration.ofMinutes(5))
                .setLogPayload(true)
                .setEnableJitter(false)
                .setEnableCircuitBreaker(false);
    }

    @Override
    public String toString() {
        return String.format("AsyncRequestConfig{" +
                        "initialRetryDelay=%s, " +
                        "maxRetryDelay=%s, " +
                        "retryBackoffMultiplier=%.2f, " +
                        "maxRetries=%d, " +
                        "inlinePayloadThreshold=%d, " +
                        "processingTimeout=%s" +
                        "}",
                initialRetryDelay, maxRetryDelay, retryBackoffMultiplier,
                maxRetries, inlinePayloadThreshold, processingTimeout);
    }
}