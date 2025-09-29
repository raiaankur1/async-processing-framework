package com.pravah.framework.async.model;

import com.pravah.framework.async.api.AsyncDAO;
import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.api.StorageClient;
import com.pravah.framework.async.config.AsyncRequestConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluent builder for creating AsyncRequest instances with comprehensive
 * configuration options, validation, and factory methods for different request types.
 * 
 * <p>This builder provides:
 * <ul>
 *   <li>Fluent API for easy configuration</li>
 *   <li>Validation for required fields and configuration consistency</li>
 *   <li>Factory methods for different request types</li>
 *   <li>Support for both synchronous and asynchronous request creation</li>
 *   <li>Automatic payload optimization (inline vs external storage)</li>
 *   <li>Retry policy configuration</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * AsyncRequest request = AsyncRequestBuilder.create()
 *     .withType(AsyncRequestType.PAYMENT)
 *     .withAppId("loan-app-123")
 *     .withPayload(jsonPayload)
 *     .withQueueType(AsyncRequestQueueType.DAILY)
 *     .withRetryPolicy(RetryPolicy.forReliability())
 *     .build();
 * }</pre>
 *
 * @author Ankur Rai
 * @version 1.0
 */
public class AsyncRequestBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequestBuilder.class);

    private final AsyncDAO asyncDAO;
    private final Map<AsyncRequestQueueType, Queue> queues;
    private final StorageClient storageClient;
    private final MetricsCollector metricsCollector;

    private String requestId;
    private String appId;
    private AsyncRequestType type;
    private AsyncRequestQueueType queueType;
    private String payload;
    private boolean forceExternalStorage = false;
    private AsyncRequestConfig config;
    private RetryPolicy retryPolicy;
    private int pollingDelayInSecs = -1; // -1 means use queue type default
    private Map<String, String> customAttributes;

    // Validation flags
    private boolean skipValidation = false;

    // Static cache for request type implementations
    private static final Map<AsyncRequestType, Class<? extends AsyncRequest>> TYPE_IMPLEMENTATIONS = 
        new ConcurrentHashMap<>();

    /**
     * Private constructor to enforce factory method usage.
     */
    private AsyncRequestBuilder(AsyncDAO asyncDAO, Map<AsyncRequestQueueType, Queue> queues, 
                               StorageClient storageClient, MetricsCollector metricsCollector) {
        this.asyncDAO = asyncDAO;
        this.queues = queues != null ? new HashMap<>(queues) : new HashMap<>();
        this.storageClient = storageClient;
        this.metricsCollector = metricsCollector;
        this.config = new AsyncRequestConfig(); // Default configuration
        this.customAttributes = new HashMap<>();
    }

    /**
     * Creates a new AsyncRequestBuilder instance with required dependencies.
     *
     * @param asyncDAO the DAO for persisting async requests
     * @param queues map of queue types to queue implementations
     * @param storageClient client for external payload storage
     * @param metricsCollector metrics collector for monitoring
     * @return new builder instance
     * @throws IllegalArgumentException if required dependencies are null
     */
    public static AsyncRequestBuilder create(AsyncDAO asyncDAO, Map<AsyncRequestQueueType, Queue> queues,
                                           StorageClient storageClient, MetricsCollector metricsCollector) {
        if (asyncDAO == null) {
            throw new IllegalArgumentException("AsyncDAO cannot be null");
        }
        if (metricsCollector == null) {
            throw new IllegalArgumentException("MetricsCollector cannot be null");
        }

        return new AsyncRequestBuilder(asyncDAO, queues, storageClient, metricsCollector);
    }

    /**
     * Factory method for creating LMS async requests with pre-configured settings.
     *
     * @param asyncDAO the DAO for persisting async requests
     * @param queues map of queue types to queue implementations
     * @param storageClient client for external payload storage
     * @param metricsCollector metrics collector for monitoring
     * @return builder pre-configured for LMS requests
     */
    public static AsyncRequestBuilder forLMS(AsyncDAO asyncDAO, Map<AsyncRequestQueueType, Queue> queues,
                                           StorageClient storageClient, MetricsCollector metricsCollector) {
        return create(asyncDAO, queues, storageClient, metricsCollector)
                .withType(AsyncRequestType.LMS)
                .withQueueType(AsyncRequestQueueType.MEDIUM_DELAY)
                .withConfig(AsyncRequestConfig.forReliability());
    }

    /**
     * Factory method for creating payment async requests with pre-configured settings.
     *
     * @param asyncDAO the DAO for persisting async requests
     * @param queues map of queue types to queue implementations
     * @param storageClient client for external payload storage
     * @param metricsCollector metrics collector for monitoring
     * @return builder pre-configured for payment requests
     */
    public static AsyncRequestBuilder forPayment(AsyncDAO asyncDAO, Map<AsyncRequestQueueType, Queue> queues,
                                               StorageClient storageClient, MetricsCollector metricsCollector) {
        return create(asyncDAO, queues, storageClient, metricsCollector)
                .withType(AsyncRequestType.PAYMENT)
                .withQueueType(AsyncRequestQueueType.SHORT_DELAY)
                .withConfig(AsyncRequestConfig.forHighThroughput());
    }

    /**
     * Factory method for creating notification async requests with pre-configured settings.
     *
     * @param asyncDAO the DAO for persisting async requests
     * @param queues map of queue types to queue implementations
     * @param storageClient client for external payload storage
     * @param metricsCollector metrics collector for monitoring
     * @return builder pre-configured for notification requests
     */
    public static AsyncRequestBuilder forNotification(AsyncDAO asyncDAO, Map<AsyncRequestQueueType, Queue> queues,
                                                    StorageClient storageClient, MetricsCollector metricsCollector) {
        return create(asyncDAO, queues, storageClient, metricsCollector)
                .withType(AsyncRequestType.NOTIFICATION)
                .withQueueType(AsyncRequestQueueType.IMMEDIATE)
                .withConfig(AsyncRequestConfig.forHighThroughput());
    }

    // Fluent configuration methods

    /**
     * Sets the request ID. If not provided, a UUID will be generated.
     *
     * @param requestId the request ID
     * @return this builder instance
     */
    public AsyncRequestBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Sets the application ID that initiated this request.
     *
     * @param appId the application ID
     * @return this builder instance
     */
    public AsyncRequestBuilder withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * Sets the async request type.
     *
     * @param type the request type
     * @return this builder instance
     */
    public AsyncRequestBuilder withType(AsyncRequestType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the queue type for processing delay.
     *
     * @param queueType the queue type
     * @return this builder instance
     */
    public AsyncRequestBuilder withQueueType(AsyncRequestQueueType queueType) {
        this.queueType = queueType;
        return this;
    }

    /**
     * Sets the request payload. The builder will automatically determine
     * whether to store inline or externally based on size and configuration.
     *
     * @param payload the request payload
     * @return this builder instance
     */
    public AsyncRequestBuilder withPayload(String payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Forces the payload to be stored externally regardless of size.
     *
     * @param payload the request payload
     * @return this builder instance
     */
    public AsyncRequestBuilder withS3Payload(String payload) {
        this.payload = payload;
        this.forceExternalStorage = true;
        return this;
    }

    /**
     * Sets the async request configuration.
     *
     * @param config the configuration
     * @return this builder instance
     */
    public AsyncRequestBuilder withConfig(AsyncRequestConfig config) {
        this.config = config != null ? config : new AsyncRequestConfig();
        return this;
    }

    /**
     * Sets the retry policy for this request.
     *
     * @param retryPolicy the retry policy
     * @return this builder instance
     */
    public AsyncRequestBuilder withRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Sets a custom polling delay, overriding the queue type default.
     *
     * @param delaySeconds the delay in seconds
     * @return this builder instance
     */
    public AsyncRequestBuilder withPollingDelay(int delaySeconds) {
        this.pollingDelayInSecs = delaySeconds;
        return this;
    }

    /**
     * Sets a custom polling delay, overriding the queue type default.
     *
     * @param delay the delay duration
     * @return this builder instance
     */
    public AsyncRequestBuilder withPollingDelay(Duration delay) {
        this.pollingDelayInSecs = (int) delay.getSeconds();
        return this;
    }

    /**
     * Adds a custom attribute to the request.
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this builder instance
     */
    public AsyncRequestBuilder withCustomAttribute(String key, String value) {
        if (StringUtils.hasText(key)) {
            this.customAttributes.put(key, value);
        }
        return this;
    }

    /**
     * Adds multiple custom attributes to the request.
     *
     * @param attributes map of attributes to add
     * @return this builder instance
     */
    public AsyncRequestBuilder withCustomAttributes(Map<String, String> attributes) {
        if (attributes != null) {
            this.customAttributes.putAll(attributes);
        }
        return this;
    }

    /**
     * Skips validation during build (use with caution).
     *
     * @return this builder instance
     */
    public AsyncRequestBuilder skipValidation() {
        this.skipValidation = true;
        return this;
    }

    // Build methods

    /**
     * Builds and returns the AsyncRequest instance synchronously.
     * This method validates the configuration and creates the request.
     *
     * @return the configured AsyncRequest instance
     * @throws AsyncFrameworkException if validation fails or request creation fails
     */
    public AsyncRequest build() {
        try {
            if (!skipValidation) {
                validateConfiguration();
            }

            AsyncRequest request = createRequestInstance();
            configureRequest(request);
            
            logger.debug("Successfully built AsyncRequest: {}", request.getRequestId());
            return request;

        } catch (AsyncFrameworkException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to build AsyncRequest", e);
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Failed to build AsyncRequest: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Builds the AsyncRequest instance and persists it to storage synchronously.
     * This method also handles payload storage and queue insertion.
     *
     * @return the created and persisted AsyncRequest instance
     * @throws AsyncFrameworkException if creation or persistence fails
     */
    public AsyncRequest create() {
        AsyncRequest request = build();

        try {
            // Store payload if needed
            if (payload != null) {
                request.storePayloadIfNeeded(payload);
            }

            // Persist to database
            asyncDAO.createRequest(request);
            logger.info("Created AsyncRequest: {}", request.getRequestId());

            // Queue for processing
            request.insertIntoQueue();
            logger.info("Queued AsyncRequest for processing: {}", request.getRequestId());

            // Record metrics
            if (metricsCollector != null) {
                metricsCollector.recordRequestQueued(request.getType());
            }

            return request;

        } catch (Exception e) {
            logger.error("Failed to create and persist AsyncRequest: {}", request.getRequestId(), e);
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                request.getRequestId(),
                "Failed to create AsyncRequest: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Builds the AsyncRequest instance asynchronously.
     *
     * @return CompletableFuture that resolves to the configured AsyncRequest instance
     */
    public CompletableFuture<AsyncRequest> buildAsync() {
        return CompletableFuture.supplyAsync(this::build);
    }

    /**
     * Creates and persists the AsyncRequest instance asynchronously.
     *
     * @return CompletableFuture that resolves to the created AsyncRequest instance
     */
    public CompletableFuture<AsyncRequest> createAsync() {
        return CompletableFuture.supplyAsync(this::create);
    }

    // Validation methods

    /**
     * Validates the builder configuration before creating the request.
     *
     * @throws AsyncFrameworkException if validation fails
     */
    private void validateConfiguration() {
        // Validate required fields
        if (type == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "AsyncRequestType is required"
            );
        }

        if (!StringUtils.hasText(appId)) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Application ID is required"
            );
        }

        // Validate queue configuration
        if (queueType != null && !queues.containsKey(queueType)) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Queue not configured for type: " + queueType
            );
        }

        // Validate payload size
        if (payload != null && payload.length() > config.getMaxPayloadSize()) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                String.format("Payload size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    payload.length(), config.getMaxPayloadSize())
            );
        }

        // Validate external storage requirement
        if (forceExternalStorage && storageClient == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "External storage requested but StorageClient not available"
            );
        }

        // Validate configuration consistency
        if (config != null) {
            try {
                config.validate();
            } catch (IllegalArgumentException e) {
                throw new AsyncFrameworkException(
                    ErrorCode.CONFIGURATION_ERROR,
                    null,
                    "Invalid AsyncRequestConfig: " + e.getMessage(),
                    e
                );
            }
        }

        // Validate retry policy consistency
        if (retryPolicy != null && config != null) {
            validateRetryPolicyConsistency();
        }
    }

    /**
     * Validates that retry policy is consistent with configuration.
     */
    private void validateRetryPolicyConsistency() {
        if (retryPolicy.getMaxRetries() != config.getMaxRetries()) {
            logger.warn("Retry policy max retries ({}) differs from config max retries ({}). Using retry policy value.",
                retryPolicy.getMaxRetries(), config.getMaxRetries());
        }
    }

    // Request creation methods

    /**
     * Creates the appropriate AsyncRequest instance based on the type.
     *
     * @return new AsyncRequest instance
     * @throws AsyncFrameworkException if request creation fails
     */
    private AsyncRequest createRequestInstance() {
        try {
            Class<? extends AsyncRequest> implementationClass = getImplementationClass(type);
            
            // Try to create instance using constructor with dependencies
            Constructor<? extends AsyncRequest> constructor = implementationClass.getConstructor(
                AsyncDAO.class, Queue.class, StorageClient.class, MetricsCollector.class
            );

            Queue queue = queueType != null ? queues.get(queueType) : null;
            return constructor.newInstance(asyncDAO, queue, storageClient, metricsCollector);

        } catch (Exception e) {
            logger.error("Failed to create AsyncRequest instance for type: {}", type, e);
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Failed to create AsyncRequest instance for type " + type + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Gets the implementation class for the given request type.
     */
    private Class<? extends AsyncRequest> getImplementationClass(AsyncRequestType type) {
        return TYPE_IMPLEMENTATIONS.computeIfAbsent(type, this::loadImplementationClass);
    }

    /**
     * Loads the implementation class for the given request type.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends AsyncRequest> loadImplementationClass(AsyncRequestType type) {
        try {
            String className = type.getImplementationClassName();
            Class<?> clazz = Class.forName(className);
            
            if (!AsyncRequest.class.isAssignableFrom(clazz)) {
                throw new AsyncFrameworkException(
                    ErrorCode.CONFIGURATION_ERROR,
                    null,
                    "Implementation class " + className + " does not extend AsyncRequest"
                );
            }
            
            return (Class<? extends AsyncRequest>) clazz;

        } catch (ClassNotFoundException e) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Implementation class not found for type " + type + ": " + type.getImplementationClassName(),
                e
            );
        }
    }

    /**
     * Configures the created AsyncRequest instance with builder settings.
     */
    private void configureRequest(AsyncRequest request) {
        // Set basic properties
        if (StringUtils.hasText(requestId)) {
            request.withRequestId(requestId);
        }
        
        request.withAppId(appId)
               .withType(type)
               .withConfig(config);

        // Set polling delay
        if (pollingDelayInSecs >= 0) {
            request.withPollingDelayInSecs(pollingDelayInSecs);
        } else if (queueType != null) {
            request.withPollingDelayInSecs(queueType.getDelaySeconds());
        }

        // Apply retry policy if specified
        if (retryPolicy != null) {
            request.withMaxRetries(retryPolicy.getMaxRetries());
            
            // Update config with retry policy settings
            config.setMaxRetries(retryPolicy.getMaxRetries())
                  .setInitialRetryDelay(retryPolicy.getInitialDelay())
                  .setMaxRetryDelay(retryPolicy.getMaxDelay())
                  .setRetryBackoffMultiplier(retryPolicy.getBackoffMultiplier());
        }

        // Handle payload storage
        if (payload != null) {
            if (forceExternalStorage || payload.length() > config.getInlinePayloadThreshold()) {
                // Will be stored externally during create()
                request.withExternalPayloadKey("payload.json");
            } else {
                request.withInlinePayload(payload);
            }
        }
    }

    // Getter methods for testing and debugging

    public String getRequestId() { return requestId; }
    public String getAppId() { return appId; }
    public AsyncRequestType getType() { return type; }
    public AsyncRequestQueueType getQueueType() { return queueType; }
    public String getPayload() { return payload; }
    public AsyncRequestConfig getConfig() { return config; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public Map<String, String> getCustomAttributes() { return new HashMap<>(customAttributes); }

    /**
     * Simple retry policy configuration class.
     */
    public static class RetryPolicy {
        private final int maxRetries;
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final double backoffMultiplier;

        public RetryPolicy(int maxRetries, Duration initialDelay, Duration maxDelay, double backoffMultiplier) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            this.backoffMultiplier = backoffMultiplier;
        }

        public static RetryPolicy forReliability() {
            return new RetryPolicy(5, Duration.ofMinutes(1), Duration.ofMinutes(30), 2.5);
        }

        public static RetryPolicy forHighThroughput() {
            return new RetryPolicy(2, Duration.ofSeconds(10), Duration.ofMinutes(2), 1.5);
        }

        public static RetryPolicy forDevelopment() {
            return new RetryPolicy(1, Duration.ofSeconds(5), Duration.ofMinutes(1), 1.5);
        }

        public static RetryPolicy custom(int maxRetries, Duration initialDelay, Duration maxDelay, double backoffMultiplier) {
            return new RetryPolicy(maxRetries, initialDelay, maxDelay, backoffMultiplier);
        }

        // Getters
        public int getMaxRetries() { return maxRetries; }
        public Duration getInitialDelay() { return initialDelay; }
        public Duration getMaxDelay() { return maxDelay; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
    }
}