package com.pravah.framework.async.services;

import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.config.AsyncFrameworkConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.model.AsyncRequestQueueType;
import com.pravah.framework.async.model.AsyncRequestStatus;
import com.pravah.framework.async.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SQS Message Listener for processing async requests using polling-based approach.
 * 
 * <p>This component provides:
 * <ul>
 *   <li>Polling-based SQS message consumption</li>
 *   <li>Concurrent message processing with configurable thread pools</li>
 *   <li>Comprehensive error handling and retry mechanisms</li>
 *   <li>Dead letter queue integration for failed messages</li>
 *   <li>Message acknowledgment and visibility timeout management</li>
 *   <li>Metrics collection and monitoring integration</li>
 * </ul>
 * 
 * <p>The listener supports multiple queue types and can be configured to process
 * different types of async requests with appropriate concurrency levels.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@Component
@ConditionalOnProperty(
    prefix = "async.framework.listener", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class SQSMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SQSMessageListener.class);

    private final AsyncRequestService asyncRequestService;
    private final MetricsCollector metricsCollector;
    private final AsyncFrameworkConfig config;
    private final Map<AsyncRequestQueueType, Queue> queueMap;

    // Processing state
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicLong processedMessageCount = new AtomicLong(0);
    private final AtomicLong failedMessageCount = new AtomicLong(0);

    // Thread pool for async processing
    private ExecutorService processingExecutor;

    // Active message tracking
    private final Map<String, CompletableFuture<Void>> activeMessages = new ConcurrentHashMap<>();

    /**
     * Constructor with dependency injection.
     *
     * @param asyncRequestService service for processing async requests
     * @param metricsCollector metrics collector for monitoring
     * @param config framework configuration
     * @param queueMap map of queue types to queue implementations
     */
    @Autowired
    public SQSMessageListener(
            AsyncRequestService asyncRequestService,
            MetricsCollector metricsCollector,
            AsyncFrameworkConfig config,
            Map<AsyncRequestQueueType, Queue> queueMap) {
        
        this.asyncRequestService = asyncRequestService;
        this.metricsCollector = metricsCollector;
        this.config = config;
        this.queueMap = queueMap != null ? queueMap : new ConcurrentHashMap<>();

        logger.info("SQSMessageListener initialized with {} queue types", this.queueMap.size());
    }

    /**
     * Post-construct initialization.
     */
    @PostConstruct
    public void initialize() {
        if (initialized.get()) {
            return;
        }

        try {
            // Initialize thread pool for async processing
            int corePoolSize = config.getProcessing().getCorePoolSize();
            int maxPoolSize = config.getProcessing().getMaxPoolSize();
            
            processingExecutor = Executors.newFixedThreadPool(
                Math.max(corePoolSize, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, config.getProcessing().getThreadNamePrefix() + "listener-" + System.currentTimeMillis());
                    t.setDaemon(false);
                    return t;
                }
            );

            // Record initialization metrics
            if (metricsCollector != null) {
                metricsCollector.recordListenerInitialized();
            }

            initialized.set(true);
            logger.info("SQSMessageListener successfully initialized with thread pool size: {}", corePoolSize);

        } catch (Exception e) {
            logger.error("Failed to initialize SQSMessageListener", e);
            throw new AsyncFrameworkException(
                ErrorCode.CONFIGURATION_ERROR,
                null,
                "Failed to initialize SQSMessageListener: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Pre-destroy cleanup.
     */
    @PreDestroy
    public void shutdown() {
        if (shutdownRequested.getAndSet(true)) {
            return;
        }

        logger.info("Shutting down SQSMessageListener...");

        try {
            // Cancel all active message processing
            activeMessages.values().forEach(future -> future.cancel(true));
            activeMessages.clear();

            // Shutdown thread pool gracefully
            if (processingExecutor != null) {
                processingExecutor.shutdown();
                if (!processingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    processingExecutor.shutdownNow();
                    if (!processingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        logger.warn("Thread pool did not terminate gracefully");
                    }
                }
            }

            logger.info("SQSMessageListener shutdown completed. Processed: {}, Failed: {}", 
                processedMessageCount.get(), failedMessageCount.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown interrupted", e);
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    // SQS Message Polling Methods

    /**
     * Polls messages from all configured queues.
     */
    @Scheduled(fixedDelayString = "${async.framework.listener.poll-interval:5000}")
    public void pollMessages() {
        if (shutdownRequested.get() || !initialized.get()) {
            return;
        }

        // Poll each queue type
        for (Map.Entry<AsyncRequestQueueType, Queue> entry : queueMap.entrySet()) {
            AsyncRequestQueueType queueType = entry.getKey();
            Queue queue = entry.getValue();
            
            try {
                pollQueueMessages(queueType, queue);
            } catch (Exception e) {
                logger.error("Error polling messages from queue: {}", queueType, e);
            }
        }
    }

    /**
     * Polls messages from a specific queue.
     *
     * @param queueType the queue type
     * @param queue the queue implementation
     */
    private void pollQueueMessages(AsyncRequestQueueType queueType, Queue queue) {
        try {
            int maxMessages = Math.min(10, config.getQueues().getOrDefault(queueType.toString().toLowerCase(), 
                new AsyncFrameworkConfig.QueueConfig()).getMaxConcurrency());
            int waitTimeSeconds = 20; // Long polling
            
            List<Queue.QueueMessage> messages = queue.receiveMessages(maxMessages, waitTimeSeconds);
            
            for (Queue.QueueMessage message : messages) {
                handleQueueMessage(message, queueType, queue);
            }
            
        } catch (Exception e) {
            logger.error("Failed to poll messages from queue: {}", queueType, e);
        }
    }

    /**
     * Handles a single queue message.
     *
     * @param message the queue message
     * @param queueType the queue type
     * @param queue the queue implementation
     */
    private void handleQueueMessage(Queue.QueueMessage message, AsyncRequestQueueType queueType, Queue queue) {
        String requestId = extractRequestId(message.getBody());
        if (!StringUtils.hasText(requestId)) {
            logger.error("Invalid message body - no request ID found: {}", message.getBody());
            // Delete invalid message to prevent reprocessing
            queue.deleteMessage(message.getReceiptHandle());
            return;
        }

        // Convert message attributes
        Map<String, Object> messageAttributes = new HashMap<>();
        if (message.getMessageAttributes() != null) {
            messageAttributes.putAll(message.getMessageAttributes());
        }

        // Create a simple acknowledgment implementation
        MessageAcknowledgment acknowledgment = new MessageAcknowledgment(message.getReceiptHandle(), queue);
        
        handleMessage(message.getBody(), messageAttributes, acknowledgment, queueType);
    }

    /**
     * Polls dead letter queue messages.
     */
    @Scheduled(fixedDelayString = "${async.framework.listener.dlq-poll-interval:30000}")
    public void pollDeadLetterMessages() {
        if (shutdownRequested.get() || !initialized.get()) {
            return;
        }

        String deadLetterQueueUrl = config.getRetry().getDeadLetterQueueUrl();
        if (!StringUtils.hasText(deadLetterQueueUrl)) {
            return;
        }

        // Find queue implementation for dead letter queue
        Queue deadLetterQueue = queueMap.values().stream()
            .filter(q -> deadLetterQueueUrl.equals(q.getDeadLetterQueueUrl()) || deadLetterQueueUrl.equals(q.getQueueUrl()))
            .findFirst()
            .orElse(null);

        if (deadLetterQueue == null) {
            logger.warn("No queue implementation found for dead letter queue URL: {}", deadLetterQueueUrl);
            return;
        }

        try {
            List<Queue.QueueMessage> messages = deadLetterQueue.receiveMessages(5, 20);
            
            for (Queue.QueueMessage message : messages) {
                String requestId = extractRequestId(message.getBody());
                if (StringUtils.hasText(requestId)) {
                    Map<String, Object> messageAttributes = new HashMap<>();
                    if (message.getMessageAttributes() != null) {
                        messageAttributes.putAll(message.getMessageAttributes());
                    }
                    
                    MessageAcknowledgment acknowledgment = new MessageAcknowledgment(message.getReceiptHandle(), deadLetterQueue);
                    handleDeadLetterMessage(message.getBody(), messageAttributes, acknowledgment);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to poll dead letter queue messages", e);
        }
    }

    // Core Message Processing Logic

    /**
     * Handles incoming SQS messages for async request processing.
     *
     * @param messageBody the message body
     * @param messageAttributes message attributes
     * @param acknowledgment acknowledgment handle
     * @param queueType the queue type
     */
    private void handleMessage(
            String messageBody, 
            Map<String, Object> messageAttributes, 
            MessageAcknowledgment acknowledgment,
            AsyncRequestQueueType queueType) {

        if (shutdownRequested.get()) {
            logger.warn("Rejecting message processing due to shutdown request");
            acknowledgment.nack();
            return;
        }

        String requestId = extractRequestId(messageBody);
        if (!StringUtils.hasText(requestId)) {
            logger.error("Invalid message body - no request ID found: {}", messageBody);
            acknowledgment.nack();
            return;
        }

        Instant startTime = Instant.now();
        logger.info("Processing message for request: {} from queue: {}", requestId, queueType);

        // Record message received metrics
        if (metricsCollector != null) {
            metricsCollector.recordMessageReceived(queueType);
        }

        // Process message asynchronously
        CompletableFuture<Void> processingFuture = CompletableFuture.runAsync(() -> {
            try {
                processAsyncRequest(requestId, messageAttributes, queueType);
                
                // Acknowledge successful processing
                acknowledgment.ack();
                processedMessageCount.incrementAndGet();
                
                Duration processingTime = Duration.between(startTime, Instant.now());
                logger.info("Successfully processed request: {} in {}ms", requestId, processingTime.toMillis());
                
                // Record success metrics
                if (metricsCollector != null) {
                    metricsCollector.recordMessageProcessed(queueType, processingTime);
                }

            } catch (Exception e) {
                logger.error("Failed to process request: {}", requestId, e);
                
                // Handle processing failure
                handleProcessingFailure(requestId, e, acknowledgment, queueType);
                failedMessageCount.incrementAndGet();
                
                Duration processingTime = Duration.between(startTime, Instant.now());
                
                // Record failure metrics
                if (metricsCollector != null) {
                    metricsCollector.recordMessageFailed(queueType, e.getClass().getSimpleName(), processingTime);
                }
            }
        }, processingExecutor);

        // Track active processing
        activeMessages.put(requestId, processingFuture);
        
        // Clean up when done
        processingFuture.whenComplete((result, throwable) -> {
            activeMessages.remove(requestId);
        });
    }

    /**
     * Handles dead letter queue messages for manual intervention.
     *
     * @param messageBody the message body
     * @param messageAttributes message attributes
     * @param acknowledgment acknowledgment handle
     */
    private void handleDeadLetterMessage(
            String messageBody, 
            Map<String, Object> messageAttributes, 
            MessageAcknowledgment acknowledgment) {

        String requestId = extractRequestId(messageBody);
        if (!StringUtils.hasText(requestId)) {
            logger.error("Invalid dead letter message body - no request ID found: {}", messageBody);
            acknowledgment.ack(); // Acknowledge to prevent reprocessing
            return;
        }

        logger.warn("Processing dead letter message for request: {}", requestId);

        try {
            // Update request status to indicate dead letter processing
            AsyncRequest request = asyncRequestService.loadRequest(requestId);
            if (request != null) {
                asyncRequestService.updateRequestStatus(requestId, AsyncRequestStatus.DEAD_LETTER);
                logger.info("Updated request status to DEAD_LETTER: {}", requestId);
            } else {
                logger.warn("Request not found for dead letter message: {}", requestId);
            }

            // Record dead letter metrics
            if (metricsCollector != null) {
                metricsCollector.recordDeadLetterMessage(requestId);
            }

            // Always acknowledge dead letter messages to prevent infinite reprocessing
            acknowledgment.ack();

        } catch (Exception e) {
            logger.error("Failed to process dead letter message for request: {}", requestId, e);
            
            // Still acknowledge to prevent infinite loop
            acknowledgment.ack();
            
            if (metricsCollector != null) {
                metricsCollector.recordDeadLetterProcessingFailed(requestId, e.getClass().getSimpleName());
            }
        }
    }

    /**
     * Processes an async request.
     *
     * @param requestId the request ID
     * @param messageAttributes message attributes
     * @param queueType the queue type
     * @throws AsyncFrameworkException if processing fails
     */
    private void processAsyncRequest(
            String requestId, 
            Map<String, Object> messageAttributes, 
            AsyncRequestQueueType queueType) throws AsyncFrameworkException {

        try {
            // Load the request
            AsyncRequest request = asyncRequestService.loadRequest(requestId);
            if (request == null) {
                throw new AsyncFrameworkException(
                    ErrorCode.PROCESSING_ERROR,
                    requestId,
                    "Request not found: " + requestId
                );
            }

            // Check if request is already completed
            if (request.getOverallStatus() == AsyncRequestStatus.COMPLETED) {
                logger.info("Request already completed, skipping: {}", requestId);
                return;
            }

            // Check if request is in a failed state that shouldn't be retried
            if (request.getOverallStatus() == AsyncRequestStatus.DEAD_LETTER) {
                logger.warn("Request is in DEAD_LETTER state, skipping: {}", requestId);
                return;
            }

            // Process the request
            boolean success = asyncRequestService.processRequest(requestId);
            
            if (!success) {
                throw new AsyncFrameworkException(
                    ErrorCode.PROCESSING_ERROR,
                    requestId,
                    "Request processing returned false"
                );
            }

        } catch (AsyncFrameworkException e) {
            throw e;
        } catch (Exception e) {
            throw new AsyncFrameworkException(
                ErrorCode.PROCESSING_ERROR,
                requestId,
                "Unexpected error during request processing: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Handles processing failures with retry logic.
     *
     * @param requestId the request ID
     * @param error the processing error
     * @param acknowledgment acknowledgment handle
     * @param queueType the queue type
     */
    private void handleProcessingFailure(
            String requestId, 
            Exception error, 
            MessageAcknowledgment acknowledgment,
            AsyncRequestQueueType queueType) {

        try {
            // Load request to check retry count
            AsyncRequest request = asyncRequestService.loadRequest(requestId);
            if (request == null) {
                logger.error("Cannot handle failure - request not found: {}", requestId);
                acknowledgment.nack();
                return;
            }

            int currentRetryCount = request.getRetryCount();
            int maxRetries = config.getRetry().getMaxAttempts();

            if (currentRetryCount >= maxRetries) {
                // Max retries exceeded - send to dead letter queue
                logger.error("Max retries exceeded for request: {} ({}), sending to dead letter queue", 
                    requestId, currentRetryCount);
                
                sendToDeadLetterQueue(requestId, error, queueType);
                asyncRequestService.updateRequestStatus(requestId, AsyncRequestStatus.FAILED);
                
                // Acknowledge to prevent further reprocessing
                acknowledgment.ack();
                
            } else {
                // Increment retry count and update status
                request.incrementRetryCount();
                asyncRequestService.updateRequestStatus(requestId, AsyncRequestStatus.RETRYING);
                
                // Calculate retry delay with exponential backoff
                Duration retryDelay = calculateRetryDelay(currentRetryCount + 1);
                
                logger.warn("Request processing failed, scheduling retry {} of {} in {}ms: {}", 
                    currentRetryCount + 1, maxRetries, retryDelay.toMillis(), requestId);
                
                // Requeue message with delay
                requeueMessageWithDelay(requestId, queueType, retryDelay);
                
                // Acknowledge current message
                acknowledgment.ack();
            }

        } catch (Exception e) {
            logger.error("Failed to handle processing failure for request: {}", requestId, e);
            
            // Negative acknowledge to trigger SQS retry
            acknowledgment.nack();
        }
    }

    /**
     * Sends a failed request to the dead letter queue.
     *
     * @param requestId the request ID
     * @param error the processing error
     * @param queueType the original queue type
     */
    private void sendToDeadLetterQueue(String requestId, Exception error, AsyncRequestQueueType queueType) {
        try {
            String deadLetterQueueUrl = config.getRetry().getDeadLetterQueueUrl();
            if (!StringUtils.hasText(deadLetterQueueUrl)) {
                logger.warn("No dead letter queue configured, cannot send failed request: {}", requestId);
                return;
            }

            // Find a queue implementation to send the message
            Queue queue = queueMap.values().stream()
                .filter(q -> deadLetterQueueUrl.equals(q.getDeadLetterQueueUrl()))
                .findFirst()
                .orElse(null);

            if (queue == null) {
                logger.error("No queue implementation found for dead letter queue URL: {}", deadLetterQueueUrl);
                return;
            }

            // Prepare message attributes with error information
            Map<String, String> attributes = Map.of(
                "originalQueue", queueType.toString(),
                "errorType", error.getClass().getSimpleName(),
                "errorMessage", error.getMessage() != null ? error.getMessage() : "Unknown error",
                "timestamp", Instant.now().toString()
            );

            // Send to dead letter queue
            queue.sendMessage(requestId, 0, attributes);
            
            logger.info("Sent failed request to dead letter queue: {}", requestId);

        } catch (Exception e) {
            logger.error("Failed to send request to dead letter queue: {}", requestId, e);
        }
    }

    /**
     * Requeues a message with delay for retry processing.
     *
     * @param requestId the request ID
     * @param queueType the queue type
     * @param delay the retry delay
     */
    private void requeueMessageWithDelay(String requestId, AsyncRequestQueueType queueType, Duration delay) {
        try {
            Queue queue = queueMap.get(queueType);
            if (queue == null) {
                logger.error("No queue implementation found for queue type: {}", queueType);
                return;
            }

            // Prepare retry attributes
            Map<String, String> attributes = Map.of(
                "retryAttempt", "true",
                "retryTimestamp", Instant.now().toString()
            );

            // Send message with delay
            queue.sendMessage(requestId, (int) delay.getSeconds(), attributes);
            
            logger.debug("Requeued request for retry with {}s delay: {}", delay.getSeconds(), requestId);

        } catch (Exception e) {
            logger.error("Failed to requeue request for retry: {}", requestId, e);
        }
    }

    /**
     * Calculates retry delay with exponential backoff.
     *
     * @param retryAttempt the retry attempt number (1-based)
     * @return the calculated delay
     */
    Duration calculateRetryDelay(int retryAttempt) {
        Duration initialDelay = config.getRetry().getInitialDelay();
        Duration maxDelay = config.getRetry().getMaxDelay();
        double backoffMultiplier = config.getRetry().getBackoffMultiplier();

        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, retryAttempt - 1));
        Duration calculatedDelay = Duration.ofMillis(delayMillis);

        // Cap at max delay
        return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
    }

    /**
     * Extracts request ID from message body.
     *
     * @param messageBody the message body
     * @return the request ID, or null if not found
     */
    String extractRequestId(String messageBody) {
        if (!StringUtils.hasText(messageBody)) {
            return null;
        }

        // Handle both plain request ID and JSON message body
        String trimmed = messageBody.trim();
        
        // If it's a simple request ID (UUID format)
        if (trimmed.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) {
            return trimmed;
        }

        // If it's JSON, try to extract requestId field
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                // Simple JSON parsing for requestId field
                String[] parts = trimmed.split("\"requestId\"\\s*:\\s*\"");
                if (parts.length > 1) {
                    String idPart = parts[1].split("\"")[0];
                    return idPart;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse JSON message body: {}", messageBody, e);
            }
        }

        // Fallback - return the entire message body as request ID
        return trimmed;
    }

    // Health and Status Methods

    /**
     * Checks if the listener is healthy and ready to process messages.
     *
     * @return true if the listener is healthy
     */
    public boolean isHealthy() {
        return initialized.get() && 
               !shutdownRequested.get() && 
               processingExecutor != null && 
               !processingExecutor.isShutdown();
    }

    /**
     * Gets listener statistics for monitoring.
     *
     * @return map of listener statistics
     */
    public Map<String, Object> getListenerStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("initialized", initialized.get());
        stats.put("shutdownRequested", shutdownRequested.get());
        stats.put("processedMessageCount", processedMessageCount.get());
        stats.put("failedMessageCount", failedMessageCount.get());
        stats.put("activeMessageCount", activeMessages.size());
        stats.put("queueTypesConfigured", queueMap.size());
        
        if (processingExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) processingExecutor;
            stats.put("threadPoolSize", tpe.getPoolSize());
            stats.put("activeThreads", tpe.getActiveCount());
            stats.put("completedTasks", tpe.getCompletedTaskCount());
            stats.put("queuedTasks", tpe.getQueue().size());
        }
        
        return stats;
    }

    // Getter methods for testing and configuration access

    public AsyncRequestService getAsyncRequestService() { return asyncRequestService; }
    public MetricsCollector getMetricsCollector() { return metricsCollector; }
    public AsyncFrameworkConfig getConfig() { return config; }
    public Map<AsyncRequestQueueType, Queue> getQueueMap() { return queueMap; }
    public boolean isInitialized() { return initialized.get(); }
    public boolean isShutdownRequested() { return shutdownRequested.get(); }
    public long getProcessedMessageCount() { return processedMessageCount.get(); }
    public long getFailedMessageCount() { return failedMessageCount.get(); }
    public int getActiveMessageCount() { return activeMessages.size(); }

    /**
     * Simple acknowledgment implementation for message processing.
     */
    public static class MessageAcknowledgment {
        private final String receiptHandle;
        private final Queue queue;
        private boolean acknowledged = false;

        public MessageAcknowledgment(String receiptHandle, Queue queue) {
            this.receiptHandle = receiptHandle;
            this.queue = queue;
        }

        /**
         * Acknowledges successful message processing.
         */
        public void ack() {
            if (!acknowledged) {
                try {
                    queue.deleteMessage(receiptHandle);
                    acknowledged = true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to acknowledge message", e);
                }
            }
        }

        /**
         * Negative acknowledges message processing (makes message visible again).
         */
        public void nack() {
            if (!acknowledged) {
                try {
                    // Change visibility timeout to 0 to make message immediately visible
                    queue.changeMessageVisibility(receiptHandle, 0);
                    acknowledged = true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to negative acknowledge message", e);
                }
            }
        }

        public boolean isAcknowledged() {
            return acknowledged;
        }
    }
}