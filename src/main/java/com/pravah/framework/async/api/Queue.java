package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;

import java.util.List;
import java.util.Map;

/**
 * Queue interface for async request message handling.
 * <br>
 * This interface provides comprehensive queue operations including
 * message sending, receiving, batch operations, and dead letter queue support.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public interface Queue {

    /**
     * Send a message to the queue with delay.
     *
     * @param requestId the request ID to send
     * @param delaySeconds delay before message becomes visible
     * @throws AsyncFrameworkException if send fails
     */
    void sendMessage(String requestId, int delaySeconds);

    /**
     * Send a message to the queue with delay and attributes.
     *
     * @param requestId the request ID to send
     * @param delaySeconds delay before message becomes visible
     * @param attributes message attributes for routing/filtering
     * @throws AsyncFrameworkException if send fails
     */
    void sendMessage(String requestId, int delaySeconds, Map<String, String> attributes);

    /**
     * Send a message with custom body and attributes.
     *
     * @param messageBody the message body
     * @param delaySeconds delay before message becomes visible
     * @param attributes message attributes
     * @throws AsyncFrameworkException if send fails
     */
    void sendMessageWithBody(String messageBody, int delaySeconds, Map<String, String> attributes);

    /**
     * Sends multiple messages in a single batch operation (up to 10 messages).
     *
     * @param messages List of messages to send
     * @throws com.pravah.framework.async.exception.AsyncFrameworkException if batch send fails
     */
    void sendMessages(List<QueueMessage> messages);

    /**
     * Receive messages from the queue.
     *
     * @param maxMessages maximum number of messages to receive (1-10)
     * @param waitTimeSeconds long polling wait time (0-20)
     * @return list of received messages
     * @throws AsyncFrameworkException if receive fails
     */
    List<QueueMessage> receiveMessages(int maxMessages, int waitTimeSeconds);

    /**
     * Receives messages with custom visibility timeout.
     *
     * @param maxMessages Maximum number of messages to receive (1-10)
     * @param waitTimeSeconds Long polling wait time in seconds (0-20)
     * @param visibilityTimeoutSeconds Visibility timeout for received messages
     * @return List of received messages
     * @throws AsyncFrameworkException if receive fails
     */
    List<QueueMessage> receiveMessages(int maxMessages, int waitTimeSeconds, int visibilityTimeoutSeconds);

    /**
     * Delete a message from the queue.
     *
     * @param receiptHandle the receipt handle from received message
     * @throws AsyncFrameworkException if delete fails
     */
    void deleteMessage(String receiptHandle);

    /**
     * Batch delete multiple messages.
     *
     * @param receiptHandles list of receipt handles to delete
     * @throws AsyncFrameworkException if batch delete fails
     */
    void deleteMessages(List<String> receiptHandles);

    /**
     * Change the visibility timeout of a message.
     *
     * @param receiptHandle the receipt handle
     * @param visibilityTimeoutSeconds new visibility timeout
     * @throws AsyncFrameworkException if change fails
     */
    void changeMessageVisibility(String receiptHandle, int visibilityTimeoutSeconds);

    /**
     * Changes the visibility timeout of multiple messages in a batch operation.
     *
     * @param visibilityChanges List of visibility change requests
     * @throws com.pravah.framework.async.exception.AsyncFrameworkException if batch visibility change fails
     */
    void changeMessagesVisibility(List<VisibilityChange> visibilityChanges);

    /**
     * Gets the approximate number of messages in the queue.
     *
     * @return Approximate number of messages
     * @throws com.pravah.framework.async.exception.AsyncFrameworkException if attribute retrieval fails
     */
    int getApproximateMessageCount();

    /**
     * Gets the approximate number of messages not visible (in flight).
     *
     * @return Approximate number of in-flight messages
     * @throws com.pravah.framework.async.exception.AsyncFrameworkException if attribute retrieval fails
     */
    int getApproximateInFlightMessageCount();

    /**
     * Get queue attributes and statistics.
     *
     * @return queue attributes
     * @throws AsyncFrameworkException if query fails
     */
    Map<String, String> getQueueAttributes();

    /**
     * Purge all messages from the queue.
     *
     * @throws AsyncFrameworkException if purge fails
     */
    void purgeQueue();

    /**
     * Check if the queue is healthy and accessible.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Get the queue URL or identifier.
     *
     * @return queue URL/identifier
     */
    String getQueueUrl();

    /**
     * Gets the dead letter queue URL if configured.
     *
     * @return The dead letter queue URL, or null if not configured
     */
    String getDeadLetterQueueUrl();

    /**
     * Represents a message received from the queue.
     */
    class QueueMessage {
        private String messageId;
        private String receiptHandle;
        private String body;
        private Map<String, String> attributes;
        private Map<String, String> messageAttributes;
        private int receiveCount;
        private String md5OfBody;
        private String md5OfMessageAttributes;
        private int delaySeconds;

        public QueueMessage() {}

        public QueueMessage(String body) {
            this.body = body;
        }

        public QueueMessage(String body, int delaySeconds) {
            this.body = body;
            this.delaySeconds = delaySeconds;
        }

        public QueueMessage(String body, int delaySeconds, Map<String, String> messageAttributes) {
            this.body = body;
            this.delaySeconds = delaySeconds;
            this.messageAttributes = messageAttributes;
        }

        public QueueMessage(String messageId, String receiptHandle, String body,
                            Map<String, String> attributes, int receiveCount) {
            this.messageId = messageId;
            this.receiptHandle = receiptHandle;
            this.body = body;
            this.attributes = attributes;
            this.receiveCount = receiveCount;
        }

        public String getMessageId() {
            return messageId;
        }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getReceiptHandle() {
            return receiptHandle;
        }
        public void setReceiptHandle(String receiptHandle) { this.receiptHandle = receiptHandle; }
        public String getBody() {
            return body;
        }
        public void setBody(String body) { this.body = body; }
        public Map<String, String> getAttributes() {
            return attributes;
        }
        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
        public String getAttribute(String key) {
            return attributes != null ? attributes.get(key) : null;
        }

        public Map<String, String> getMessageAttributes() { return messageAttributes; }
        public void setMessageAttributes(Map<String, String> messageAttributes) {
            this.messageAttributes = messageAttributes;
        }

        public int getReceiveCount() {
            return receiveCount;
        }
        public void setReceiveCount(int receiveCount) { this.receiveCount = receiveCount; }
        public String getMd5OfBody() { return md5OfBody; }
        public void setMd5OfBody(String md5OfBody) { this.md5OfBody = md5OfBody; }
        public String getMd5OfMessageAttributes() { return md5OfMessageAttributes; }
        public void setMd5OfMessageAttributes(String md5OfMessageAttributes) { this.md5OfMessageAttributes = md5OfMessageAttributes; }

        public int getDelaySeconds() { return delaySeconds; }
        public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }
        public boolean isFirstReceive() {
            return receiveCount == 1;
        }

        @Override
        public String toString() {
            return String.format("QueueMessage{messageId='%s', receiveCount=%d, body='%s'}",
                    messageId, receiveCount, body);
        }
    }

    /**
     * Represents a message to be sent in batch operations.
     */
    class BatchMessage {
        private final String id;
        private final String body;
        private final int delaySeconds;
        private final Map<String, String> attributes;

        public BatchMessage(String id, String body, int delaySeconds, Map<String, String> attributes) {
            this.id = id;
            this.body = body;
            this.delaySeconds = delaySeconds;
            this.attributes = attributes;
        }

        public String getId() {
            return id;
        }

        public String getBody() {
            return body;
        }

        public int getDelaySeconds() {
            return delaySeconds;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }
    }

    /**
     * Queue attributes and statistics.
     */
    class QueueAttributes {
        private final int approximateNumberOfMessages;
        private final int approximateNumberOfMessagesNotVisible;
        private final int approximateNumberOfMessagesDelayed;
        private final long createdTimestamp;
        private final long lastModifiedTimestamp;
        private final int visibilityTimeoutSeconds;
        private final int messageRetentionPeriod;
        private final int maxReceiveCount;
        private final String deadLetterQueueUrl;

        public QueueAttributes(int approximateNumberOfMessages,
                               int approximateNumberOfMessagesNotVisible,
                               int approximateNumberOfMessagesDelayed,
                               long createdTimestamp,
                               long lastModifiedTimestamp,
                               int visibilityTimeoutSeconds,
                               int messageRetentionPeriod,
                               int maxReceiveCount,
                               String deadLetterQueueUrl) {
            this.approximateNumberOfMessages = approximateNumberOfMessages;
            this.approximateNumberOfMessagesNotVisible = approximateNumberOfMessagesNotVisible;
            this.approximateNumberOfMessagesDelayed = approximateNumberOfMessagesDelayed;
            this.createdTimestamp = createdTimestamp;
            this.lastModifiedTimestamp = lastModifiedTimestamp;
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
            this.messageRetentionPeriod = messageRetentionPeriod;
            this.maxReceiveCount = maxReceiveCount;
            this.deadLetterQueueUrl = deadLetterQueueUrl;
        }

        public int getApproximateNumberOfMessages() {
            return approximateNumberOfMessages;
        }

        public int getApproximateNumberOfMessagesNotVisible() {
            return approximateNumberOfMessagesNotVisible;
        }

        public int getApproximateNumberOfMessagesDelayed() {
            return approximateNumberOfMessagesDelayed;
        }

        public long getCreatedTimestamp() {
            return createdTimestamp;
        }

        public long getLastModifiedTimestamp() {
            return lastModifiedTimestamp;
        }

        public int getVisibilityTimeoutSeconds() {
            return visibilityTimeoutSeconds;
        }

        public int getMessageRetentionPeriod() {
            return messageRetentionPeriod;
        }

        public int getMaxReceiveCount() {
            return maxReceiveCount;
        }

        public String getDeadLetterQueueUrl() {
            return deadLetterQueueUrl;
        }

        public boolean hasDeadLetterQueue() {
            return deadLetterQueueUrl != null && !deadLetterQueueUrl.trim().isEmpty();
        }

        public int getTotalMessages() {
            return approximateNumberOfMessages + approximateNumberOfMessagesNotVisible + approximateNumberOfMessagesDelayed;
        }
    }

    /**
     * Represents a visibility timeout change request.
     */
    class VisibilityChange {
        private String receiptHandle;
        private int visibilityTimeoutSeconds;

        public VisibilityChange() {}

        public VisibilityChange(String receiptHandle, int visibilityTimeoutSeconds) {
            this.receiptHandle = receiptHandle;
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        }
        public String getReceiptHandle() { return receiptHandle; }
        public void setReceiptHandle(String receiptHandle) { this.receiptHandle = receiptHandle; }
        public int getVisibilityTimeoutSeconds() { return visibilityTimeoutSeconds; }
        public void setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) { this.visibilityTimeoutSeconds = visibilityTimeoutSeconds; }
    }
}