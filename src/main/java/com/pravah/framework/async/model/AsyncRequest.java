package com.pravah.framework.async.model;

import com.pravah.framework.async.api.AsyncDAO;
import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.api.StorageClient;
import com.pravah.framework.async.config.AsyncRequestConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.monitoring.MetricsCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base class for asynchronous request processing.
 * <br>
 * This class provides a comprehensive framework for handling distributed
 * asynchronous operations
 * with support for:
 * - CompletableFuture-based async processing
 * - Exponential backoff retry logic
 * - Metrics collection integration
 * - Comprehensive error handling and lifecycle management
 * - Payload storage optimization (inline vs external)
 *
 * @author Ankur Rai
 * @version 1.0
 */
public abstract class AsyncRequest {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequest.class);

    // Core identification properties
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("appId")
    private String appId;

    @JsonProperty("type")
    private AsyncRequestType type;

    // Status tracking
    @JsonProperty("status")
    private AsyncRequestStatus status;

    @JsonProperty("apiStatus")
    private Map<AsyncRequestAPI, AsyncRequestStatus> apiStatus;

    // Payload handling
    @JsonProperty("inlinePayload")
    private String inlinePayload;

    @JsonProperty("externalPayloadKey")
    private String externalPayloadKey;

    // Timing and lifecycle
    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("lastProcessedAt")
    private Instant lastProcessedAt;

    @JsonProperty("completedAt")
    private Instant completedAt;

    // Retry and error handling
    @JsonProperty("retryCount")
    private int retryCount;

    @JsonProperty("maxRetries")
    private int maxRetries;

    @JsonProperty("lastError")
    private String lastError;

    @JsonProperty("errorCode")
    private String errorCode;

    // Configuration
    @JsonProperty("config")
    private AsyncRequestConfig config;

    @JsonProperty("pollingDelayInSecs")
    private int pollingDelayInSecs;

    // Dependencies (not serialized)
    @JsonIgnore
    private AsyncDAO asyncDAO;

    @JsonIgnore
    private Queue queue;

    @JsonIgnore
    private StorageClient storageClient;

    @JsonIgnore
    private MetricsCollector metricsCollector;

    // Processing state
    @JsonIgnore
    private volatile boolean processing = false;

    /**
     * Constructor for creating a new AsyncRequest with dependencies.
     */
    protected AsyncRequest(AsyncDAO asyncDAO, Queue queue, StorageClient storageClient,
            MetricsCollector metricsCollector) {
        this.asyncDAO = asyncDAO;
        this.queue = queue;
        this.storageClient = storageClient;
        this.metricsCollector = metricsCollector;
        this.requestId = generateRequestId();
        this.createdAt = Instant.now();
        this.status = AsyncRequestStatus.CREATED;
        this.retryCount = 0;
        this.maxRetries = 3; // Default value
        this.config = new AsyncRequestConfig();
        this.initializeApiStatus();
    }

    /**
     * Default constructor for deserialization.
     */
    protected AsyncRequest() {
        this.requestId = generateRequestId();
        this.createdAt = Instant.now();
        this.status = AsyncRequestStatus.CREATED;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.config = new AsyncRequestConfig();
    }

    /**
     * Initialize the API status map for all required APIs.
     */
    private void initializeApiStatus() {
        this.apiStatus = new HashMap<>();
        List<AsyncRequestAPI> apis = this.getRequiredAPIs();
        for (AsyncRequestAPI api : apis) {
            this.apiStatus.put(api, AsyncRequestStatus.CREATED);
        }
    }

    /**
     * Generate a unique request ID.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Main processing method with CompletableFuture support and retry logic.
     *
     * @return CompletableFuture that completes when processing is done
     */
    public final CompletableFuture<Boolean> process() {
        if (processing) {
            logger.warn("Request {} is already being processed", requestId);
            return CompletableFuture.completedFuture(false);
        }

        processing = true;
        setupMDC();

        try {
            logger.info("Starting processing for request {} of type {}", requestId, type);
            metricsCollector.recordRequestStarted(type);

            return executeWithRetry()
                    .whenComplete((result, throwable) -> {
                        processing = false;
                        clearMDC();

                        if (throwable != null) {
                            handleProcessingFailure(throwable);
                        } else if (result) {
                            handleProcessingSuccess();
                        }
                    });

        } catch (Exception e) {
            processing = false;
            clearMDC();
            logger.error("Unexpected error during processing setup for request {}", requestId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Execute the async operation with retry logic and exponential backoff.
     */
    private CompletableFuture<Boolean> executeWithRetry() {
        return executeAsyncInternal()
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        return handleRetryLogic(throwable);
                    }
                    return CompletableFuture.completedFuture(result);
                })
                .thenCompose(future -> future);
    }

    /**
     * Internal async execution wrapper.
     */
    private CompletableFuture<Boolean> executeAsyncInternal() {
        try {
            updateStatus(AsyncRequestStatus.PROCESSING);
            this.lastProcessedAt = Instant.now();

            return executeAsync()
                    .exceptionally(throwable -> {
                        logger.error("Error during async execution for request {}", requestId, throwable);
                        throw new CompletionException(throwable);
                    });

        } catch (Exception e) {
            logger.error("Error setting up async execution for request {}", requestId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Handle retry logic with exponential backoff.
     */
    private CompletableFuture<Boolean> handleRetryLogic(Throwable throwable) {
        if (!shouldRetry(throwable)) {
            logger.error("Non-retryable error for request {}: {}", requestId, throwable.getMessage());
            recordError(throwable);
            updateStatus(AsyncRequestStatus.FAILED);
            return CompletableFuture.completedFuture(false);
        }

        if (retryCount >= maxRetries) {
            logger.error("Retry limit exceeded for request {} after {} attempts", requestId, retryCount);
            recordError(throwable);
            updateStatus(AsyncRequestStatus.DEAD_LETTER);
            metricsCollector.recordRetryExhausted(requestId, type);
            return CompletableFuture.completedFuture(false);
        }

        retryCount++;
        Duration delay = calculateRetryDelay();

        logger.warn("Retrying request {} (attempt {}/{}) after {} seconds",
                requestId, retryCount, maxRetries, delay.getSeconds());

        metricsCollector.recordRetryAttempt(requestId, retryCount);
        updateStatus(AsyncRequestStatus.RETRYING);

        // Schedule retry with exponential backoff
        return CompletableFuture
                .supplyAsync(() -> null,
                        CompletableFuture.delayedExecutor(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS))
                .thenCompose(v -> executeWithRetry());
    }

    /**
     * Calculate retry delay with exponential backoff and jitter.
     */
    private Duration calculateRetryDelay() {
        long baseDelayMs = config.getInitialRetryDelay().toMillis();
        double multiplier = config.getRetryBackoffMultiplier();
        long maxDelayMs = config.getMaxRetryDelay().toMillis();

        // Exponential backoff: delay = baseDelay * (multiplier ^ (retryCount - 1))
        long delayMs = (long) (baseDelayMs * Math.pow(multiplier, retryCount - 1));

        // Cap at max delay
        delayMs = Math.min(delayMs, maxDelayMs);

        // Add jitter (±25% of calculated delay)
        double jitterFactor = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.5);
        delayMs = (long) (delayMs * jitterFactor);

        return Duration.ofMillis(delayMs);
    }

    /**
     * Determine if an error should trigger a retry.
     */
    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof AsyncFrameworkException) {
            AsyncFrameworkException afe = (AsyncFrameworkException) throwable;
            return afe.getErrorCode().isRetryable();
        }

        // Retry on network/timeout issues, but not on validation errors
        String message = throwable.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                    lowerMessage.contains("connection") ||
                    lowerMessage.contains("network") ||
                    lowerMessage.contains("throttl");
        }

        return true; // Default to retry for unknown errors
    }

    /**
     * Handle successful processing completion.
     */
    private void handleProcessingSuccess() {
        this.completedAt = Instant.now();
        updateStatus(AsyncRequestStatus.COMPLETED);

        Duration processingTime = Duration.between(createdAt, completedAt);
        logger.info("Successfully completed processing for request {} in {}", requestId, processingTime);

        metricsCollector.recordRequestCompleted(type, processingTime);

        // Update individual API statuses to completed
        for (AsyncRequestAPI api : apiStatus.keySet()) {
            apiStatus.put(api, AsyncRequestStatus.COMPLETED);
        }

        // Persist final state
        if (asyncDAO != null) {
            try {
                asyncDAO.updateStatus(requestId, AsyncRequestStatus.COMPLETED);
            } catch (Exception e) {
                logger.error("Failed to update final status for request {}", requestId, e);
            }
        }
    }

    /**
     * Handle processing failure.
     */
    private void handleProcessingFailure(Throwable throwable) {
        recordError(throwable);

        Duration processingTime = Duration.between(createdAt, Instant.now());
        logger.error("Failed to process request {} after {}", requestId, processingTime, throwable);

        metricsCollector.recordRequestFailed(type, getErrorCode(throwable));
    }

    /**
     * Record error information.
     */
    private void recordError(Throwable throwable) {
        this.lastError = throwable.getMessage();
        this.errorCode = getErrorCode(throwable);
        this.lastProcessedAt = Instant.now();
    }

    /**
     * Extract error code from throwable.
     */
    private String getErrorCode(Throwable throwable) {
        if (throwable instanceof AsyncFrameworkException) {
            return ((AsyncFrameworkException) throwable).getErrorCode().name();
        }
        return ErrorCode.PROCESSING_ERROR.name();
    }

    /**
     * Update the overall status and persist if DAO is available.
     */
    private void updateStatus(AsyncRequestStatus newStatus) {
        AsyncRequestStatus oldStatus = this.status;
        this.status = newStatus;

        logger.debug("Status updated for request {} from {} to {}", requestId, oldStatus, newStatus);

        if (asyncDAO != null) {
            try {
                asyncDAO.updateStatus(requestId, newStatus);
            } catch (Exception e) {
                logger.error("Failed to persist status update for request {}", requestId, e);
            }
        }
    }

    /**
     * Set up MDC for logging context.
     */
    private void setupMDC() {
        MDC.put("requestId", requestId);
        MDC.put("appId", appId);
        MDC.put("requestType", type != null ? type.toString() : "unknown");
    }

    /**
     * Clear MDC logging context.
     */
    private void clearMDC() {
        MDC.remove("requestId");
        MDC.remove("appId");
        MDC.remove("requestType");
    }

    // Public API methods

    /**
     * Check if the request processing is complete and successful.
     */
    public final boolean isSuccess() {
        return status == AsyncRequestStatus.COMPLETED &&
                !apiStatus.values().contains(AsyncRequestStatus.CREATED) &&
                !apiStatus.values().contains(AsyncRequestStatus.PROCESSING) &&
                !apiStatus.values().contains(AsyncRequestStatus.RETRYING);
    }

    /**
     * Check if a specific API call is completed.
     */
    public final boolean isCompleted(AsyncRequestAPI api) {
        if (api == null) {
            throw new IllegalArgumentException("API cannot be null");
        }
        return apiStatus.get(api) == AsyncRequestStatus.COMPLETED;
    }

    /**
     * Queue the request for processing with configured delay.
     */
    public final void insertIntoQueue() {
        if (queue == null) {
            throw new AsyncFrameworkException(ErrorCode.CONFIGURATION_ERROR,
                    "Queue not configured for request " + requestId, requestId);
        }

        int delay = this.pollingDelayInSecs > 0 ? this.pollingDelayInSecs : 30;

        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("requestType", type.toString());
            attributes.put("appId", appId);
            attributes.put("retryCount", String.valueOf(retryCount));

            queue.sendMessage(requestId, delay, attributes);
            updateStatus(AsyncRequestStatus.QUEUED);

            logger.info("Request {} queued for processing with delay {} seconds", requestId, delay);
            metricsCollector.recordRequestQueued(type);

        } catch (Exception e) {
            logger.error("Failed to queue request {}", requestId, e);
            throw new AsyncFrameworkException(ErrorCode.QUEUE_ERROR,
                    "Failed to queue request: " + e.getMessage(), requestId, e);
        }
    }

    /**
     * Mark a specific API as successfully completed.
     */
    public final void updateSuccess(AsyncRequestAPI api) {
        if (api == null) {
            throw new IllegalArgumentException("API cannot be null");
        }

        try {
            if (asyncDAO != null) {
                asyncDAO.updateSuccess(requestId, api);
            }

            apiStatus.put(api, AsyncRequestStatus.COMPLETED);
            logger.info("API {} marked as completed for request {}", api, requestId);

            // Check if all APIs are completed
            if (isSuccess()) {
                handleProcessingSuccess();
            }

        } catch (Exception e) {
            logger.error("Failed to update success status for API {} in request {}", api, requestId, e);
            throw new AsyncFrameworkException(ErrorCode.PROCESSING_ERROR,
                    "Failed to update API status: " + e.getMessage(), requestId, e);
        }
    }

    /**
     * Get the request payload, handling both inline and external storage.
     */
    public final String getRequestPayload() {
        try {
            if (inlinePayload != null && !inlinePayload.trim().isEmpty()) {
                return inlinePayload;
            }

            if (externalPayloadKey != null && storageClient != null) {
                return storageClient.getObject(requestId, externalPayloadKey);
            }

            // Fallback to default payload key
            if (storageClient != null) {
                return storageClient.getObject(requestId, "payload.json");
            }

            logger.warn("No payload available for request {}", requestId);
            return null;

        } catch (Exception e) {
            logger.error("Failed to retrieve payload for request {}", requestId, e);
            throw new AsyncFrameworkException(ErrorCode.STORAGE_ERROR,
                    "Failed to retrieve payload: " + e.getMessage(), requestId, e);
        }
    }

    /**
     * Store payload externally if it exceeds the inline threshold.
     */
    public final void storePayloadIfNeeded(String payload) {
        if (payload == null) {
            return;
        }

        int inlineThreshold = config.getInlinePayloadThreshold();

        if (payload.length() <= inlineThreshold) {
            this.inlinePayload = payload;
            this.externalPayloadKey = null;
        } else {
            if (storageClient == null) {
                throw new AsyncFrameworkException(ErrorCode.CONFIGURATION_ERROR,
                        "Storage client not configured but payload exceeds inline threshold", requestId);
            }

            try {
                String key = "payload.json";
                storageClient.putObject(requestId, key, payload);
                this.externalPayloadKey = key;
                this.inlinePayload = null;

                logger.info("Payload stored externally for request {} with key {}", requestId, key);

            } catch (Exception e) {
                logger.error("Failed to store payload externally for request {}", requestId, e);
                throw new AsyncFrameworkException(ErrorCode.STORAGE_ERROR,
                        "Failed to store payload: " + e.getMessage(), requestId, e);
            }
        }
    }

    // Abstract methods that subclasses must implement

    /**
     * Execute the actual asynchronous operation.
     * Subclasses should implement their specific business logic here.
     *
     * @return CompletableFuture that resolves to true if successful, false
     *         otherwise
     */
    protected abstract CompletableFuture<Boolean> executeAsync();

    /**
     * Get the list of APIs that this request type requires.
     *
     * @return List of required APIs
     */
    public abstract List<AsyncRequestAPI> getRequiredAPIs();

    // Getters and setters with fluent interface

    public String getRequestId() {
        return requestId;
    }

    public AsyncRequest withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    public AsyncRequest withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public AsyncRequestType getType() {
        return type;
    }

    public AsyncRequest withType(AsyncRequestType type) {
        this.type = type;
        return this;
    }

    public AsyncRequestStatus getStatus() {
        return status;
    }

    public AsyncRequest withStatus(AsyncRequestStatus status) {
        this.status = status;
        return this;
    }

    public Map<AsyncRequestAPI, AsyncRequestStatus> getApiStatus() {
        return new HashMap<>(apiStatus); // Return defensive copy
    }

    public AsyncRequest withApiStatus(Map<AsyncRequestAPI, AsyncRequestStatus> apiStatus) {
        this.apiStatus = new HashMap<>(apiStatus);
        return this;
    }

    public int getPollingDelayInSecs() {
        return pollingDelayInSecs;
    }

    public AsyncRequest withPollingDelayInSecs(int pollingDelayInSecs) {
        this.pollingDelayInSecs = pollingDelayInSecs;
        return this;
    }

    public String getInlinePayload() {
        return inlinePayload;
    }

    public AsyncRequest withInlinePayload(String inlinePayload) {
        this.inlinePayload = inlinePayload;
        return this;
    }

    public String getExternalPayloadKey() {
        return externalPayloadKey;
    }

    public AsyncRequest withExternalPayloadKey(String externalPayloadKey) {
        this.externalPayloadKey = externalPayloadKey;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastProcessedAt() {
        return lastProcessedAt;
    }

    public void setLastProcessedAt(Instant lastProcessedAt) {
        this.lastProcessedAt = lastProcessedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public AsyncRequest withMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public String getLastError() {
        return lastError;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return lastError;
    }

    public void setErrorMessage(String errorMessage) {
        this.lastError = errorMessage;
    }

    public AsyncRequestConfig getConfig() {
        return config;
    }

    public AsyncRequest withConfig(AsyncRequestConfig config) {
        this.config = config;
        return this;
    }

    // Dependency injection methods

    public void setAsyncDAO(AsyncDAO asyncDAO) {
        this.asyncDAO = asyncDAO;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public void setStorageClient(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    public void setMetricsCollector(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public String toString() {
        return String.format("AsyncRequest{requestId='%s', appId='%s', type=%s, status=%s, retryCount=%d}",
                requestId, appId, type, status, retryCount);
    }
}