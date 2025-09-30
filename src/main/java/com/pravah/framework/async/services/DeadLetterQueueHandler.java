package com.pravah.framework.async.services;

import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.config.AsyncFrameworkConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.model.AsyncRequestStatus;
import com.pravah.framework.async.monitoring.MetricsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for handling dead letter queue operations
 * when retry attempts are exhausted.
 * <br>
 * This component manages the sending of failed requests to dead letter queues
 * and provides functionality to retrieve and reprocess dead letter requests.
 *
 * @author Ankur Rai
 * @version 1.0
 */
@Component
public class DeadLetterQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);

    private final AsyncFrameworkConfig.RetryConfig retryConfig;
    private final Queue deadLetterQueue;
    private final AsyncRequestService asyncRequestService;
    private final MetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for DeadLetterQueueHandler.
     *
     * @param frameworkConfig the framework configuration
     * @param deadLetterQueue the dead letter queue implementation
     * @param asyncRequestService the async request service
     * @param metricsCollector the metrics collector
     * @param objectMapper the JSON object mapper
     */
    public DeadLetterQueueHandler(AsyncFrameworkConfig frameworkConfig,
                                 Queue deadLetterQueue,
                                 AsyncRequestService asyncRequestService,
                                 MetricsCollector metricsCollector,
                                 ObjectMapper objectMapper) {
        this.retryConfig = frameworkConfig.getRetry();
        this.deadLetterQueue = deadLetterQueue;
        this.asyncRequestService = asyncRequestService;
        this.metricsCollector = metricsCollector;
        this.objectMapper = objectMapper;
    }

    /**
     * Send a failed request to the dead letter queue.
     *
     * @param request the failed async request
     * @param lastException the final exception that caused the failure
     */
    public void sendToDeadLetterQueue(AsyncRequest request, AsyncFrameworkException lastException) {
        if (!retryConfig.isEnableDeadLetterQueue()) {
            logger.debug("Dead letter queue is disabled, skipping request: {}", request.getRequestId());
            return;
        }

        String requestId = request.getRequestId();
        
        try {
            logger.warn("Sending request to dead letter queue: {}, final error: {}", 
                       requestId, lastException.getFormattedMessage());

            // Update request status to DEAD_LETTER
            updateRequestStatusToDeadLetter(request, lastException);

            // Create dead letter message
            DeadLetterMessage deadLetterMessage = createDeadLetterMessage(request, lastException);
            
            // Send to dead letter queue with message attributes
            Map<String, String> messageAttributes = createMessageAttributes(request, lastException);
            String messageBody = objectMapper.writeValueAsString(deadLetterMessage);
            
            deadLetterQueue.sendMessage(messageBody, 0, messageAttributes);
            
            metricsCollector.recordDeadLetterQueueSent(requestId, request.getType().name());
            
            logger.info("Successfully sent request to dead letter queue: {}", requestId);
            
        } catch (Exception e) {
            logger.error("Failed to send request to dead letter queue: {}", requestId, e);
            metricsCollector.recordDeadLetterQueueError(requestId, e.getClass().getSimpleName());
            
            // If we can't send to DLQ, at least update the status
            try {
                updateRequestStatusToDeadLetter(request, lastException);
            } catch (Exception statusUpdateException) {
                logger.error("Failed to update request status to DEAD_LETTER: {}", requestId, statusUpdateException);
            }
        }
    }

    /**
     * Retrieve dead letter requests for manual inspection or reprocessing.
     *
     * @param maxMessages maximum number of messages to retrieve
     * @return list of dead letter messages
     */
    public List<DeadLetterMessage> getDeadLetterRequests(int maxMessages) {
        try {
            logger.debug("Retrieving up to {} dead letter messages", maxMessages);
            
            // This would typically involve receiving messages from the DLQ
            // For now, we'll return an empty list as the actual implementation
            // would depend on the specific queue implementation
            
            // TODO: Implement actual DLQ message retrieval
            // var messages = deadLetterQueue.receiveMessages(maxMessages, 20);
            // return messages.stream()
            //     .map(this::parseDeadLetterMessage)
            //     .collect(Collectors.toList());
            
            return List.of();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve dead letter requests", e);
            throw new AsyncFrameworkException(
                ErrorCode.QUEUE_ERROR,
                "Failed to retrieve dead letter requests",
                null,
                e
            );
        }
    }

    /**
     * Reprocess a request from the dead letter queue.
     *
     * @param requestId the request ID to reprocess
     * @return true if reprocessing was initiated successfully
     */
    public boolean reprocessDeadLetterRequest(String requestId) {
        try {
            logger.info("Attempting to reprocess dead letter request: {}", requestId);
            
            // Retrieve the request from storage
            AsyncRequest request = asyncRequestService.getAsyncRequest(requestId);
            
            if (request == null) {
                logger.warn("Request not found for reprocessing: {}", requestId);
                return false;
            }
            
            if (request.getStatus() != AsyncRequestStatus.DEAD_LETTER) {
                logger.warn("Request is not in DEAD_LETTER status, cannot reprocess: {} (status: {})", 
                           requestId, request.getStatus());
                return false;
            }
            
            // Reset request for reprocessing
            resetRequestForReprocessing(request);
            
            // Submit for processing
            asyncRequestService.processAsyncRequest(request);
            
            metricsCollector.recordDeadLetterQueueReprocessed(requestId);
            
            logger.info("Successfully initiated reprocessing for dead letter request: {}", requestId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reprocess dead letter request: {}", requestId, e);
            metricsCollector.recordDeadLetterQueueReprocessError(requestId, e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Get statistics about dead letter queue usage.
     *
     * @return dead letter queue statistics
     */
    public DeadLetterQueueStats getDeadLetterQueueStats() {
        // This would typically query the DLQ for statistics
        // For now, return basic stats
        return new DeadLetterQueueStats(0, 0, Instant.now());
    }

    /**
     * Update the request status to DEAD_LETTER in persistent storage.
     *
     * @param request the request to update
     * @param lastException the final exception
     */
    private void updateRequestStatusToDeadLetter(AsyncRequest request, AsyncFrameworkException lastException) {
        try {
            request.setStatus(AsyncRequestStatus.DEAD_LETTER);
            request.setLastProcessedAt(Instant.now());
            request.setErrorMessage(lastException.getFormattedMessage());
            
            asyncRequestService.updateAsyncRequest(request);
            
        } catch (Exception e) {
            logger.error("Failed to update request status to DEAD_LETTER: {}", request.getRequestId(), e);
            throw new AsyncFrameworkException(
                ErrorCode.DATABASE_ERROR,
                "Failed to update request status to DEAD_LETTER",
                request.getRequestId(),
                e
            );
        }
    }

    /**
     * Create a dead letter message from the failed request.
     *
     * @param request the failed request
     * @param lastException the final exception
     * @return dead letter message
     */
    private DeadLetterMessage createDeadLetterMessage(AsyncRequest request, AsyncFrameworkException lastException) {
        return new DeadLetterMessage(
            request.getRequestId(),
            request.getAppId(),
            request.getType().name(),
            request.getCreatedAt(),
            Instant.now(),
            request.getRetryCount(),
            lastException.getErrorCode().getCode(),
            lastException.getFormattedMessage(),
            request.getInlinePayload(),
            request.getExternalPayloadKey()
        );
    }

    /**
     * Create message attributes for the dead letter queue message.
     *
     * @param request the failed request
     * @param lastException the final exception
     * @return message attributes map
     */
    private Map<String, String> createMessageAttributes(AsyncRequest request, AsyncFrameworkException lastException) {
        Map<String, String> attributes = new HashMap<>();
        
        attributes.put("requestId", request.getRequestId());
        attributes.put("appId", request.getAppId());
        attributes.put("requestType", request.getType().name());
        attributes.put("errorCode", lastException.getErrorCode().getCode());
        attributes.put("retryCount", String.valueOf(request.getRetryCount()));
        attributes.put("deadLetterTime", Instant.now().toString());
        
        return attributes;
    }

    /**
     * Reset a request for reprocessing from dead letter queue.
     *
     * @param request the request to reset
     */
    private void resetRequestForReprocessing(AsyncRequest request) {
        request.setStatus(AsyncRequestStatus.CREATED);
        request.setRetryCount(0);
        request.setErrorMessage(null);
        request.setLastProcessedAt(null);
        
        // Reset API statuses
        request.getApiStatus().replaceAll((api, status) -> AsyncRequestStatus.CREATED);
    }

    /**
     * Dead letter message data structure.
     */
    public static class DeadLetterMessage {
        private final String requestId;
        private final String appId;
        private final String requestType;
        private final Instant originalCreatedAt;
        private final Instant deadLetterTime;
        private final int finalRetryCount;
        private final String errorCode;
        private final String errorMessage;
        private final String inlinePayload;
        private final String externalPayloadKey;

        public DeadLetterMessage(String requestId, String appId, String requestType,
                               Instant originalCreatedAt, Instant deadLetterTime,
                               int finalRetryCount, String errorCode, String errorMessage,
                               String inlinePayload, String externalPayloadKey) {
            this.requestId = requestId;
            this.appId = appId;
            this.requestType = requestType;
            this.originalCreatedAt = originalCreatedAt;
            this.deadLetterTime = deadLetterTime;
            this.finalRetryCount = finalRetryCount;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.inlinePayload = inlinePayload;
            this.externalPayloadKey = externalPayloadKey;
        }

        // Getters
        public String getRequestId() { return requestId; }
        public String getAppId() { return appId; }
        public String getRequestType() { return requestType; }
        public Instant getOriginalCreatedAt() { return originalCreatedAt; }
        public Instant getDeadLetterTime() { return deadLetterTime; }
        public int getFinalRetryCount() { return finalRetryCount; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public String getInlinePayload() { return inlinePayload; }
        public String getExternalPayloadKey() { return externalPayloadKey; }
    }

    /**
     * Dead letter queue statistics.
     */
    public static class DeadLetterQueueStats {
        private final int totalMessages;
        private final int oldestMessageAgeHours;
        private final Instant lastUpdated;

        public DeadLetterQueueStats(int totalMessages, int oldestMessageAgeHours, Instant lastUpdated) {
            this.totalMessages = totalMessages;
            this.oldestMessageAgeHours = oldestMessageAgeHours;
            this.lastUpdated = lastUpdated;
        }

        public int getTotalMessages() { return totalMessages; }
        public int getOldestMessageAgeHours() { return oldestMessageAgeHours; }
        public Instant getLastUpdated() { return lastUpdated; }
    }
}