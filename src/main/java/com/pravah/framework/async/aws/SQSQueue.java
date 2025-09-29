package com.pravah.framework.async.aws;

import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQS implementation of Queue interface with batch operations support,
 * message attributes, dead letter queue support, long polling configuration,
 * and comprehensive error handling.
 *
 * @author Ankur Rai
 * @version 1.0
 */
public class SQSQueue implements Queue {

    private static final Logger logger = LoggerFactory.getLogger(SQSQueue.class);

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final String deadLetterQueueUrl;
    private final int defaultVisibilityTimeoutSeconds;

    // SQS limits
    private static final int MAX_BATCH_SIZE = 10;
    private static final int MAX_MESSAGE_SIZE = 262144; // 256 KB
    private static final int MAX_WAIT_TIME_SECONDS = 20;
    private static final int MAX_VISIBILITY_TIMEOUT_SECONDS = 43200; // 12 hours

    public SQSQueue(SqsClient sqsClient, String queueUrl) {
        this(sqsClient, queueUrl, null, 300); // Default 5 minutes visibility timeout
    }

    public SQSQueue(SqsClient sqsClient, String queueUrl, String deadLetterQueueUrl, int defaultVisibilityTimeoutSeconds) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.deadLetterQueueUrl = deadLetterQueueUrl;
        this.defaultVisibilityTimeoutSeconds = defaultVisibilityTimeoutSeconds;
    }

    @Override
    public void sendMessage(String requestId, int delaySeconds) {
        sendMessage(requestId, delaySeconds, null);
    }

    @Override
    public void sendMessage(String requestId, int delaySeconds, Map<String, String> attributes) {
        sendMessage(requestId, delaySeconds, attributes);
    }

    @Override
    public void sendMessageWithBody(String messageBody, int delaySeconds, Map<String, String> attributes) {
        try {
            validateMessageBody(messageBody);
            validateDelaySeconds(delaySeconds);

            SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delaySeconds);

            if (attributes != null && !attributes.isEmpty()) {
                Map<String, MessageAttributeValue> messageAttributes = convertToMessageAttributes(attributes);
                requestBuilder.messageAttributes(messageAttributes);
            }

            SendMessageResponse response = sqsClient.sendMessage(requestBuilder.build());

            logger.debug("Sent message to queue: messageId={}, delaySeconds={}",
                    response.messageId(), delaySeconds);

        } catch (Exception e) {
            logger.error("Failed to send message to queue: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to send message to SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void sendMessages(List<QueueMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            // Split into batches of 10 (SQS limit)
            List<List<QueueMessage>> batches = partitionList(messages, MAX_BATCH_SIZE);

            for (List<QueueMessage> batch : batches) {
                sendMessageBatch(batch);
            }

            logger.debug("Sent {} messages to queue in {} batches", messages.size(), batches.size());

        } catch (Exception e) {
            logger.error("Failed to send message batch to queue: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to send message batch to SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<QueueMessage> receiveMessages(int maxMessages, int waitTimeSeconds) {
        return receiveMessages(maxMessages, waitTimeSeconds, defaultVisibilityTimeoutSeconds);
    }

    @Override
    public List<QueueMessage> receiveMessages(int maxMessages, int waitTimeSeconds, int visibilityTimeoutSeconds) {
        try {
            validateReceiveParameters(maxMessages, waitTimeSeconds, visibilityTimeoutSeconds);

            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(waitTimeSeconds)
                    .visibilityTimeout(visibilityTimeoutSeconds)
                    .attributeNames(QueueAttributeName.ALL)
                    .messageAttributeNames("All")
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);

            List<QueueMessage> queueMessages = response.messages().stream()
                    .map(item -> convertToQueueMessage(item))
                    .collect(Collectors.toList());

            logger.debug("Received {} messages from queue", queueMessages.size());
            return queueMessages;

        } catch (Exception e) {
            logger.error("Failed to receive messages from queue: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to receive messages from SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void deleteMessage(String receiptHandle) {
        try {
            if (receiptHandle == null || receiptHandle.trim().isEmpty()) {
                throw new IllegalArgumentException("Receipt handle cannot be null or empty");
            }

            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            logger.debug("Deleted message from queue: receiptHandle={}", receiptHandle);

        } catch (Exception e) {
            logger.error("Failed to delete message from queue: receiptHandle={}", receiptHandle, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to delete message from SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void deleteMessages(List<String> receiptHandles) {
        if (receiptHandles == null || receiptHandles.isEmpty()) {
            return;
        }

        try {
            // Split into batches of 10 (SQS limit)
            List<List<String>> batches = partitionList(receiptHandles, MAX_BATCH_SIZE);

            for (List<String> batch : batches) {
                deleteMessageBatch(batch);
            }

            logger.debug("Deleted {} messages from queue in {} batches", receiptHandles.size(), batches.size());

        } catch (Exception e) {
            logger.error("Failed to delete message batch from queue: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to delete message batch from SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void changeMessageVisibility(String receiptHandle, int visibilityTimeoutSeconds) {
        try {
            if (receiptHandle == null || receiptHandle.trim().isEmpty()) {
                throw new IllegalArgumentException("Receipt handle cannot be null or empty");
            }

            validateVisibilityTimeout(visibilityTimeoutSeconds);

            ChangeMessageVisibilityRequest changeRequest = ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(visibilityTimeoutSeconds)
                    .build();

            sqsClient.changeMessageVisibility(changeRequest);
            logger.debug("Changed message visibility: receiptHandle={}, visibilityTimeout={}",
                    receiptHandle, visibilityTimeoutSeconds);

        } catch (Exception e) {
            logger.error("Failed to change message visibility: receiptHandle={}", receiptHandle, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to change message visibility in SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void changeMessagesVisibility(List<VisibilityChange> visibilityChanges) {
        if (visibilityChanges == null || visibilityChanges.isEmpty()) {
            return;
        }

        try {
            // Split into batches of 10 (SQS limit)
            List<List<VisibilityChange>> batches = partitionList(visibilityChanges, MAX_BATCH_SIZE);

            for (List<VisibilityChange> batch : batches) {
                changeMessageVisibilityBatch(batch);
            }

            logger.debug("Changed visibility for {} messages in {} batches",
                    visibilityChanges.size(), batches.size());

        } catch (Exception e) {
            logger.error("Failed to change message visibility batch: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to change message visibility batch in SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public int getApproximateMessageCount() {
        Map<String, String> attributes = getQueueAttributes();
        String countStr = attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString());
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    @Override
    public int getApproximateInFlightMessageCount() {
        Map<String, String> attributes = getQueueAttributes();
        String countStr = attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString());
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    @Override
    public Map<String, String> getQueueAttributes() {
        try {
            GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.ALL)
                    .build();

            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(attributesRequest);

            Map<String, String> attributes = new HashMap<>();
            response.attributes().forEach((key, value) -> attributes.put(key.toString(), value));

            return attributes;

        } catch (Exception e) {
            logger.error("Failed to get queue attributes: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to get SQS queue attributes: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void purgeQueue() {
        try {
            PurgeQueueRequest purgeRequest = PurgeQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.purgeQueue(purgeRequest);
            logger.info("Purged queue: {}", queueUrl);

        } catch (Exception e) {
            logger.error("Failed to purge queue: {}", queueUrl, e);
            throw new AsyncFrameworkException(
                    ErrorCode.QUEUE_ERROR,
                    null,
                    "Failed to purge SQS queue: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public String getQueueUrl() {
        return queueUrl;
    }

    @Override
    public String getDeadLetterQueueUrl() {
        return deadLetterQueueUrl;
    }

    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple get queue attributes operation to check connectivity
            getQueueAttributes();
            return true;
        } catch (Exception e) {
            logger.warn("SQS queue health check failed for queue: {}", queueUrl, e);
            return false;
        }
    }

    // Helper methods

    private void sendMessageBatch(List<QueueMessage> messages) {
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            QueueMessage message = messages.get(i);
            validateMessageBody(message.getBody());

            SendMessageBatchRequestEntry.Builder entryBuilder = SendMessageBatchRequestEntry.builder()
                    .id(String.valueOf(i))
                    .messageBody(message.getBody())
                    .delaySeconds(message.getDelaySeconds());

            if (message.getMessageAttributes() != null && !message.getMessageAttributes().isEmpty()) {
                Map<String, MessageAttributeValue> messageAttributes = convertToMessageAttributes(message.getMessageAttributes());
                entryBuilder.messageAttributes(messageAttributes);
            }

            entries.add(entryBuilder.build());
        }

        SendMessageBatchRequest batchRequest = SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build();

        SendMessageBatchResponse response = sqsClient.sendMessageBatch(batchRequest);

        // Handle failed messages
        if (!response.failed().isEmpty()) {
            logger.warn("Failed to send {} messages in batch", response.failed().size());
            for (BatchResultErrorEntry error : response.failed()) {
                logger.warn("Failed message ID {}: {} - {}", error.id(), error.code(), error.message());
            }
        }
    }

    private void deleteMessageBatch(List<String> receiptHandles) {
        List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();

        for (int i = 0; i < receiptHandles.size(); i++) {
            entries.add(DeleteMessageBatchRequestEntry.builder()
                    .id(String.valueOf(i))
                    .receiptHandle(receiptHandles.get(i))
                    .build());
        }

        DeleteMessageBatchRequest batchRequest = DeleteMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build();

        DeleteMessageBatchResponse response = sqsClient.deleteMessageBatch(batchRequest);

        // Handle failed deletions
        if (!response.failed().isEmpty()) {
            logger.warn("Failed to delete {} messages in batch", response.failed().size());
            for (BatchResultErrorEntry error : response.failed()) {
                logger.warn("Failed delete ID {}: {} - {}", error.id(), error.code(), error.message());
            }
        }
    }

    private void changeMessageVisibilityBatch(List<VisibilityChange> visibilityChanges) {
        List<ChangeMessageVisibilityBatchRequestEntry> entries = new ArrayList<>();

        for (int i = 0; i < visibilityChanges.size(); i++) {
            VisibilityChange change = visibilityChanges.get(i);
            validateVisibilityTimeout(change.getVisibilityTimeoutSeconds());

            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(String.valueOf(i))
                    .receiptHandle(change.getReceiptHandle())
                    .visibilityTimeout(change.getVisibilityTimeoutSeconds())
                    .build());
        }

        ChangeMessageVisibilityBatchRequest batchRequest = ChangeMessageVisibilityBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(entries)
                .build();

        ChangeMessageVisibilityBatchResponse response = sqsClient.changeMessageVisibilityBatch(batchRequest);

        // Handle failed visibility changes
        if (!response.failed().isEmpty()) {
            logger.warn("Failed to change visibility for {} messages in batch", response.failed().size());
            for (BatchResultErrorEntry error : response.failed()) {
                logger.warn("Failed visibility change ID {}: {} - {}", error.id(), error.code(), error.message());
            }
        }
    }

    private QueueMessage convertToQueueMessage(Message sqsMessage) {
        QueueMessage queueMessage = new QueueMessage();
        queueMessage.setMessageId(sqsMessage.messageId());
        queueMessage.setReceiptHandle(sqsMessage.receiptHandle());
        queueMessage.setBody(sqsMessage.body());
        queueMessage.setMd5OfBody(sqsMessage.md5OfBody());
        queueMessage.setMd5OfMessageAttributes(sqsMessage.md5OfMessageAttributes());

        // Convert attributes
        if (sqsMessage.hasAttributes()) {
            Map<String, String> attributes = new HashMap<>();
            sqsMessage.attributes().forEach((key, value) -> attributes.put(key.toString(), value));
            queueMessage.setAttributes(attributes);

            // Extract receive count
            String receiveCountStr = attributes.get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT.toString());
            if (receiveCountStr != null) {
                queueMessage.setReceiveCount(Integer.parseInt(receiveCountStr));
            }
        }

        // Convert message attributes
        if (sqsMessage.hasMessageAttributes()) {
            Map<String, String> messageAttributes = new HashMap<>();
            sqsMessage.messageAttributes().forEach((key, value) -> {
                if (value.stringValue() != null) {
                    messageAttributes.put(key, value.stringValue());
                }
            });
            queueMessage.setMessageAttributes(messageAttributes);
        }

        return queueMessage;
    }

    private Map<String, MessageAttributeValue> convertToMessageAttributes(Map<String, String> attributes) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        attributes.forEach((key, value) -> {
            messageAttributes.put(key, MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(value)
                    .build());
        });
        return messageAttributes;
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private void validateMessageBody(String messageBody) {
        if (messageBody == null) {
            throw new IllegalArgumentException("Message body cannot be null");
        }
        if (messageBody.getBytes().length > MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message body exceeds maximum size of " + MAX_MESSAGE_SIZE + " bytes");
        }
    }

    private void validateDelaySeconds(int delaySeconds) {
        if (delaySeconds < 0 || delaySeconds > 900) { // 15 minutes max
            throw new IllegalArgumentException("Delay seconds must be between 0 and 900");
        }
    }

    private void validateReceiveParameters(int maxMessages, int waitTimeSeconds, int visibilityTimeoutSeconds) {
        if (maxMessages < 1 || maxMessages > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Max messages must be between 1 and " + MAX_BATCH_SIZE);
        }
        if (waitTimeSeconds < 0 || waitTimeSeconds > MAX_WAIT_TIME_SECONDS) {
            throw new IllegalArgumentException("Wait time seconds must be between 0 and " + MAX_WAIT_TIME_SECONDS);
        }
        validateVisibilityTimeout(visibilityTimeoutSeconds);
    }

    private void validateVisibilityTimeout(int visibilityTimeoutSeconds) {
        if (visibilityTimeoutSeconds < 0 || visibilityTimeoutSeconds > MAX_VISIBILITY_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("Visibility timeout must be between 0 and " + MAX_VISIBILITY_TIMEOUT_SECONDS);
        }
    }
}