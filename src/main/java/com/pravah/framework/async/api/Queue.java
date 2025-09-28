package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Queue interface for async request message handling.
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
     * @return message ID
     * @throws AsyncFrameworkException if send fails
     */
    String sendMessageWithBody(String messageBody, int delaySeconds, Map<String, String> attributes);

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
     * Receive a single message from the queue.
     *
     * @param waitTimeSeconds long polling wait time
     * @return received message, or null if none available
     * @throws AsyncFrameworkException if receive fails
     */
    QueueMessage receiveMessage(int waitTimeSeconds);

    /**
     * Delete a message from the queue.
     *
     * @param receiptHandle the receipt handle from received message
     * @throws AsyncFrameworkException if delete fails
     */
    void deleteMessage(String receiptHandle);

    /**
     * Batch send multiple messages.
     *
     * @param messages list of messages to send
     * @return list of successful message IDs
     * @throws AsyncFrameworkException if batch send fails
     */
    List<String> sendMessages(List<BatchMessage> messages);

    /**
     * Batch delete multiple messages.
     *
     * @param receiptHandles list of receipt handles to delete
     * @return number of successfully deleted messages
     * @throws AsyncFrameworkException if batch delete fails
     */
    int deleteMessages(List<String> receiptHandles);

    /**
     * Change the visibility timeout of a message.
     *
     * @param receiptHandle the receipt handle
     * @param visibilityTimeoutSeconds new visibility timeout
     * @throws AsyncFrameworkException if change fails
     */
    void changeMessageVisibility(String receiptHandle, int visibilityTimeoutSeconds);

    /**
     * Get queue attributes and statistics.
     *
     * @return queue attributes
     * @throws AsyncFrameworkException if query fails
     */
    QueueAttributes getQueueAttributes();

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
     * Get the queue type (e.g., "SQS", "RabbitMQ").
     *
     * @return queue type
     */
    String getQueueType();

    /**
     * Represents a message received from the queue.
     */
    class QueueMessage {
        private final String messageId;
        private final String receiptHandle;
        private final String body;
        private final Map<String, String> attributes;
        private final int receiveCount;
        private final long receivedTimestamp;

        public QueueMessage(String messageId, String receiptHandle, String body,
                            Map<String, String> attributes, int receiveCount) {
            this.messageId = messageId;
            this.receiptHandle = receiptHandle;
            this.body = body;
            this.attributes = attributes;
            this.receiveCount = receiveCount;
            this.receivedTimestamp = System.currentTimeMillis();
        }

        public String getMessageId() {
            return messageId;
        }

        public String getReceiptHandle() {
            return receiptHandle;
        }

        public String getBody() {
            return body;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String getAttribute(String key) {
            return attributes != null ? attributes.get(key) : null;
        }

        public int getReceiveCount() {
            return receiveCount;
        }

        public long getReceivedTimestamp() {
            return receivedTimestamp;
        }

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
}