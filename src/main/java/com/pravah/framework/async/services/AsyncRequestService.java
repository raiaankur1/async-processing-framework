package com.pravah.framework.async.services;

import com.pravah.framework.async.api.AsyncDAO;
import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.api.StorageClient;
import com.pravah.framework.async.config.AsyncFrameworkConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.model.AsyncRequestBuilder;
import com.pravah.framework.async.model.AsyncRequestQueueType;
import com.pravah.framework.async.model.AsyncRequestStatus;
import com.pravah.framework.async.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Core service class for managing async request lifecycle with Spring Boot integration.
 * 
 * <p>This service provides:
 * <ul>
 *   <li>Request creation and lifecycle management</li>
 *   <li>Spring Boot auto-configuration and dependency injection</li>
 *   <li>Synchronous and asynchronous processing modes</li>
 *   <li>Request status monitoring and updates</li>
 *   <li>Integration with metrics collection and monitoring</li>
 *   <li>Graceful shutdown and resource cleanup</li>
 * </ul>
 * 
 * <p>The service is designed to be a Spring-managed singleton that coordinates
 * all async request processing activities within an application.
 * 
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private AsyncRequestService asyncRequestService;
 * 
 * // Create a new request
 * AsyncRequest request = asyncRequestService.newAsyncRequestBuilder()
 *     .withType(AsyncRequestType.PAYMENT)
 *     .withAppId("loan-app-123")
 *     .withPayload(jsonPayload)
 *     .create();
 * 
 * // Process request asynchronously
 * CompletableFuture<Boolean> result = asyncRequestService.processRequestAsync(request.getRequestId());
 * }</pre>
 *
 * @author Ankur Rai
 * @version 1.0
 */
@Service
@ConditionalOnProperty(
    prefix = "async.framework", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class AsyncRequestService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRequestService.class);

    private final AsyncDAO asyncDAO;
    private final StorageClient storageClient;
    private final Map<AsyncRequestQueueType, Queue> queueMap;
    private final MetricsCollector metricsCollector;
    private final AsyncFrameworkConfig config;

    // Thread pools for async processing
    private ExecutorService processingExecutor;
    private ScheduledExecutorService scheduledExecutor;

    // Request tracking
    private final Map<String, CompletableFuture<Boolean>> activeRequests = new ConcurrentHashMap<>();

    // Service state
    private volatile boolean initialized = false;
    private volatile boolean shutdownRequested = false;

    /**
     * Constructor with dependency injection.
     *
     * @param asyncDAO the DAO for persisting async requests
     * @param storageClient client for external payload storage (optional)
     * @param queueMap map of queue types to queue implementations
     * @param metricsCollector metrics collector for monitoring
     * @param config framework configuration
     */
    @Autowired
    public AsyncRequestService(
            AsyncDAO asyncDAO,
            StorageClient storageClient,
            Map<AsyncRequestQueueType, Queue> queueMap,
            MetricsCollector metricsCollector,
            AsyncFrameworkConfig config) {
        
        this.asyncDAO = asyncDAO;
        this.storageClient = storageClient;
        this.queueMap = queueMap != null ? queueMap : new ConcurrentHashMap<>();
        this.metricsCollector = metricsCollector;
        this.config = config;

        logger.info("AsyncRequestService initialized with {} queue types", this.queueMap.size());
    }

    /**
     * Post-construct initialization.
     */
    @PostConstruct
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Initialize thread pools
            int corePoolSize = config.getProcessing().getCorePoolSize();
            int maxPoolSize = config.getProcessing().getMaxPoolSize();
            
            processingExecutor = Executors.newFixedThreadPool(
                Math.max(corePoolSize, Runtime.getRuntime().availableProcessors())
            );
            
            scheduledExecutor = Executors.newScheduledThreadPool(2);

            // Start periodic cleanup task
            scheduledExecutor.scheduleAtFixedRate(
                this::cleanupCompletedRequests,
                1, 5, TimeUnit.MINUTES
            );

            // Record initialization metrics
            if (metricsCollector != null) {
                metricsCollector.recordServiceInitialized();
            }

            initialized = true;
            logger.info("AsyncRequestService successfully initialized");

        } catch (Exception e) {
            logger.error("Failed to initialize AsyncRequestService", e);
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Failed to initialize AsyncRequestService: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Pre-destroy cleanup.
     */
    @PreDestroy
    public void shutdown() {
        if (shutdownRequested) {
            return;
        }

        shutdownRequested = true;
        logger.info("Shutting down AsyncRequestService...");

        try {
            // Cancel all active requests
            activeRequests.values().forEach(future -> future.cancel(true));
            activeRequests.clear();

            // Shutdown thread pools gracefully
            if (scheduledExecutor != null) {
                scheduledExecutor.shutdown();
                if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            }

            if (processingExecutor != null) {
                processingExecutor.shutdown();
                if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    processingExecutor.shutdownNow();
                }
            }

            logger.info("AsyncRequestService shutdown completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown interrupted", e);
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    // Request Builder Factory Methods

    /**
     * Creates a new AsyncRequestBuilder instance with all dependencies injected.
     * This is the primary entry point for creating async requests.
     *
     * @return new AsyncRequestBuilder instance
     * @throws AsyncFrameworkException if service is not initialized
     */
    public AsyncRequestBuilder newAsyncRequestBuilder() {
        ensureInitialized();
        
        logger.debug("Creating new AsyncRequestBuilder");
        return AsyncRequestBuilder.create(asyncDAO, queueMap, storageClient, metricsCollector);
    }

    /**
     * Creates a pre-configured AsyncRequestBuilder for LMS requests.
     *
     * @return AsyncRequestBuilder configured for LMS requests
     */
    public AsyncRequestBuilder newLMSRequestBuilder() {
        ensureInitialized();
        
        return AsyncRequestBuilder.forLMS(asyncDAO, queueMap, storageClient, metricsCollector);
    }

    /**
     * Creates a pre-configured AsyncRequestBuilder for payment requests.
     *
     * @return AsyncRequestBuilder configured for payment requests
     */
    public AsyncRequestBuilder newPaymentRequestBuilder() {
        ensureInitialized();
        
        return AsyncRequestBuilder.forPayment(asyncDAO, queueMap, storageClient, metricsCollector);
    }

    /**
     * Creates a pre-configured AsyncRequestBuilder for notification requests.
     *
     * @return AsyncRequestBuilder configured for notification requests
     */
    public AsyncRequestBuilder newNotificationRequestBuilder() {
        ensureInitialized();
        
        return AsyncRequestBuilder.forNotification(asyncDAO, queueMap, storageClient, metricsCollector);
    }

    // Request Lifecycle Management

    /**
     * Loads an existing async request by ID.
     *
     * @param requestId the request ID
     * @return the loaded AsyncRequest instance, or null if not found
     * @throws AsyncFrameworkException if loading fails
     */
    public AsyncRequest loadRequest(String requestId) {
        ensureInitialized();
        
        if (!StringUtils.hasText(requestId)) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Request ID cannot be null or empty"
            );
        }

        try {
            logger.debug("Loading AsyncRequest: {}", requestId);
            AsyncRequest request = asyncDAO.getAsyncRequest(requestId).orElse(null);
            
            if (request != null) {
                logger.debug("Successfully loaded AsyncRequest: {}", requestId);
                if (metricsCollector != null) {
                    metricsCollector.recordRequestLoaded(request.getType());
                }
            } else {
                logger.debug("AsyncRequest not found: {}", requestId);
            }
            
            return request;

        } catch (Exception e) {
            logger.error("Failed to load AsyncRequest: {}", requestId, e);
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                requestId,
                "Failed to load AsyncRequest: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Processes an async request synchronously.
     *
     * @param requestId the request ID to process
     * @return true if processing completed successfully, false otherwise
     * @throws AsyncFrameworkException if processing fails
     */
    public boolean processRequest(String requestId) {
        ensureInitialized();
        
        AsyncRequest request = loadRequest(requestId);
        if (request == null) {
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                requestId,
                "Request not found: " + requestId
            );
        }

        try {
            logger.info("Processing AsyncRequest synchronously: {}", requestId);
            
            // Update status to processing
            updateRequestStatus(requestId, AsyncRequestStatus.PROCESSING);
            
            // Execute the request
            boolean result = request.process().get();
            
            // Update final status
            AsyncRequestStatus finalStatus = result ? AsyncRequestStatus.COMPLETED : AsyncRequestStatus.FAILED;
            updateRequestStatus(requestId, finalStatus);
            
            // Record metrics
            if (metricsCollector != null) {
                if (result) {
                    metricsCollector.recordRequestCompleted(request.getType());
                } else {
                    metricsCollector.recordRequestFailed(request.getType(), "PROCESSING_FAILED");
                }
            }
            
            logger.info("AsyncRequest processing completed: {} - {}", requestId, finalStatus);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process AsyncRequest: {}", requestId, e);
            
            // Update status to failed
            try {
                updateRequestStatus(requestId, AsyncRequestStatus.FAILED);
            } catch (Exception statusUpdateError) {
                logger.error("Failed to update request status to FAILED: {}", requestId, statusUpdateError);
            }
            
            // Record failure metrics
            if (metricsCollector != null) {
                metricsCollector.recordRequestFailed(request.getType(), e.getClass().getSimpleName());
            }
            
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                requestId,
                "Failed to process AsyncRequest: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Processes an async request asynchronously.
     *
     * @param requestId the request ID to process
     * @return CompletableFuture that resolves to the processing result
     */
    public CompletableFuture<Boolean> processRequestAsync(String requestId) {
        ensureInitialized();
        
        // Check if request is already being processed
        CompletableFuture<Boolean> existingFuture = activeRequests.get(requestId);
        if (existingFuture != null && !existingFuture.isDone()) {
            logger.debug("Request already being processed asynchronously: {}", requestId);
            return existingFuture;
        }

        // Create new async processing future
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return processRequest(requestId);
            } catch (AsyncFrameworkException e) {
                logger.error("Async processing failed for request: {}", requestId, e);
                throw new RuntimeException(e);
            }
        }, processingExecutor);

        // Track the future
        activeRequests.put(requestId, future);
        
        // Clean up when done
        future.whenComplete((result, throwable) -> {
            activeRequests.remove(requestId);
            if (throwable != null) {
                logger.error("Async processing completed with error for request: {}", requestId, throwable);
            } else {
                logger.debug("Async processing completed successfully for request: {} - {}", requestId, result);
            }
        });

        logger.info("Started async processing for request: {}", requestId);
        return future;
    }

    /**
     * Gets an async request by ID.
     *
     * @param requestId the request ID
     * @return the async request, or null if not found
     */
    public AsyncRequest getAsyncRequest(String requestId) {
        return loadRequest(requestId);
    }

    /**
     * Updates an async request in storage.
     *
     * @param request the request to update
     * @throws AsyncFrameworkException if update fails
     */
    public void updateAsyncRequest(AsyncRequest request) {
        ensureInitialized();
        
        if (request == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Request cannot be null"
            );
        }

        try {
            logger.debug("Updating AsyncRequest: {}", request.getRequestId());
            asyncDAO.updateRequest(request);
            logger.debug("Successfully updated AsyncRequest: {}", request.getRequestId());

        } catch (Exception e) {
            logger.error("Failed to update AsyncRequest: {}", request.getRequestId(), e);
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                request.getRequestId(),
                "Failed to update AsyncRequest: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Processes an async request directly.
     *
     * @param request the request to process
     * @return CompletableFuture that resolves to the processing result
     */
    public CompletableFuture<Boolean> processAsyncRequest(AsyncRequest request) {
        ensureInitialized();
        
        if (request == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Request cannot be null"
            );
        }

        return processRequestAsync(request.getRequestId());
    }

    /**
     * Updates the status of an async request.
     *
     * @param requestId the request ID
     * @param status the new status
     * @throws AsyncFrameworkException if update fails
     */
    public void updateRequestStatus(String requestId, AsyncRequestStatus status) {
        ensureInitialized();
        
        if (!StringUtils.hasText(requestId)) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Request ID cannot be null or empty"
            );
        }

        if (status == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                requestId,
                "Status cannot be null"
            );
        }

        try {
            logger.debug("Updating request status: {} -> {}", requestId, status);
            asyncDAO.updateStatus(requestId, status);
            
            // Record metrics
            if (metricsCollector != null) {
                metricsCollector.recordStatusUpdate(status);
            }
            
            logger.debug("Successfully updated request status: {} -> {}", requestId, status);

        } catch (Exception e) {
            logger.error("Failed to update request status: {} -> {}", requestId, status, e);
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                requestId,
                "Failed to update request status: " + e.getMessage(),
                e
            );
        }
    }

    // Query Methods

    /**
     * Gets all requests with the specified status.
     *
     * @param status the status to filter by
     * @return list of requests with the specified status
     * @throws AsyncFrameworkException if query fails
     */
    public List<AsyncRequest> getRequestsByStatus(AsyncRequestStatus status) {
        ensureInitialized();
        
        if (status == null) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Status cannot be null"
            );
        }

        try {
            logger.debug("Querying requests by status: {}", status);
            List<AsyncRequest> requests = asyncDAO.getRequestsByStatus(status);
            logger.debug("Found {} requests with status: {}", requests.size(), status);
            return requests;

        } catch (Exception e) {
            logger.error("Failed to query requests by status: {}", status, e);
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                null,
                "Failed to query requests by status: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Gets the count of active processing requests.
     *
     * @return number of requests currently being processed asynchronously
     */
    public int getActiveRequestCount() {
        return activeRequests.size();
    }

    /**
     * Checks if a request is currently being processed asynchronously.
     *
     * @param requestId the request ID
     * @return true if the request is being processed asynchronously
     */
    public boolean isRequestActive(String requestId) {
        CompletableFuture<Boolean> future = activeRequests.get(requestId);
        return future != null && !future.isDone();
    }

    // Health and Status Methods

    /**
     * Checks if the service is healthy and ready to process requests.
     *
     * @return true if the service is healthy
     */
    public boolean isHealthy() {
        if (!initialized || shutdownRequested) {
            return false;
        }

        try {
            // Check if thread pools are healthy
            if (processingExecutor.isShutdown() || scheduledExecutor.isShutdown()) {
                return false;
            }

            // Check if dependencies are healthy
            // Note: Individual health checks for DAO, storage, etc. should be implemented
            // in their respective health indicators
            
            return true;

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Gets service statistics for monitoring.
     *
     * @return map of service statistics
     */
    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("initialized", initialized);
        stats.put("shutdownRequested", shutdownRequested);
        stats.put("activeRequestCount", activeRequests.size());
        stats.put("queueTypesConfigured", queueMap.size());
        stats.put("processingExecutorActive", processingExecutor != null && !processingExecutor.isShutdown());
        stats.put("scheduledExecutorActive", scheduledExecutor != null && !scheduledExecutor.isShutdown());
        return stats;
    }

    // Private Helper Methods

    /**
     * Ensures the service is initialized before processing requests.
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "AsyncRequestService is not initialized"
            );
        }

        if (shutdownRequested) {
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "AsyncRequestService is shutting down"
            );
        }
    }

    /**
     * Periodic cleanup task for completed requests.
     */
    private void cleanupCompletedRequests() {
        try {
            int initialSize = activeRequests.size();
            activeRequests.entrySet().removeIf(entry -> entry.getValue().isDone());
            int finalSize = activeRequests.size();
            
            if (initialSize != finalSize) {
                logger.debug("Cleaned up {} completed request futures", initialSize - finalSize);
            }

        } catch (Exception e) {
            logger.error("Error during request cleanup", e);
        }
    }

    // Getter methods for testing and configuration access

    public AsyncDAO getAsyncDAO() { return asyncDAO; }
    public StorageClient getStorageClient() { return storageClient; }
    public Map<AsyncRequestQueueType, Queue> getQueueMap() { return queueMap; }
    public MetricsCollector getMetricsCollector() { return metricsCollector; }
    public AsyncFrameworkConfig getConfig() { return config; }
    public boolean isInitialized() { return initialized; }
    public boolean isShutdownRequested() { return shutdownRequested; }
}