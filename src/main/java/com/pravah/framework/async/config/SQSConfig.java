package com.pravah.framework.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * SQS-specific configuration for the Async Processing Framework.
 * <br>
 * This class contains all SQS-related settings including queue URLs,
 * polling configuration, and message handling parameters.
 *
 * @author Ankur Rai
 * @version 2.0
 */
@ConfigurationProperties(prefix = "async.framework.aws.sqs")
@Validated
public class SQSConfig {

    /**
     * Map of queue names to their URLs.
     */
    private Map<String, String> queueUrls = new HashMap<>();

    /**
     * Default visibility timeout for messages in seconds.
     */
    @Min(0)
    @Max(43200) // 12 hours max
    private int visibilityTimeoutSeconds = 300; // 5 minutes

    /**
     * Message retention period in seconds.
     */
    @Min(60)
    @Max(1209600) // 14 days max
    private int messageRetentionPeriod = 1209600; // 14 days

    /**
     * Maximum number of times a message can be received before being sent to DLQ.
     */
    @Min(1)
    @Max(1000)
    private int maxReceiveCount = 3;

    /**
     * Maximum number of messages to receive in a single poll.
     */
    @Min(1)
    @Max(10)
    private int maxMessages = 10;

    /**
     * Wait time for long polling in seconds.
     */
    @Min(0)
    @Max(20)
    private int waitTimeSeconds = 20;

    /**
     * Delay before processing messages in seconds.
     */
    @Min(0)
    @Max(900) // 15 minutes max
    private int delaySeconds = 0;

    /**
     * Whether to enable long polling.
     */
    private boolean enableLongPolling = true;

    /**
     * Whether to enable message deduplication for FIFO queues.
     */
    private boolean enableDeduplication = false;

    /**
     * Whether to enable content-based deduplication for FIFO queues.
     */
    private boolean enableContentBasedDeduplication = false;

    /**
     * Polling interval when long polling is disabled.
     */
    private Duration pollingInterval = Duration.ofSeconds(5);

    /**
     * Maximum number of concurrent message processors.
     */
    @Min(1)
    private int maxConcurrentProcessors = 10;

    /**
     * Thread pool size for message processing.
     */
    @Min(1)
    private int processingThreadPoolSize = 20;

    /**
     * Timeout for message processing.
     */
    private Duration processingTimeout = Duration.ofMinutes(5);

    /**
     * Whether to delete messages immediately after successful processing.
     */
    private boolean deleteOnSuccess = true;

    /**
     * Whether to enable batch message processing.
     */
    private boolean enableBatchProcessing = true;

    /**
     * Batch size for batch operations.
     */
    @Min(1)
    @Max(10)
    private int batchSize = 10;

    /**
     * Dead letter queue configuration.
     */
    private DeadLetterQueueConfig deadLetterQueue = new DeadLetterQueueConfig();

    /**
     * Default constructor with default queue URLs.
     */
    public SQSConfig() {
        // Initialize with common queue names - URLs should be provided via configuration
        queueUrls.put("default", null);
        queueUrls.put("high-priority", null);
        queueUrls.put("low-priority", null);
        queueUrls.put("dead-letter", null);
    }

    // Getters and setters

    public Map<String, String> getQueueUrls() {
        return queueUrls;
    }

    public SQSConfig setQueueUrls(Map<String, String> queueUrls) {
        this.queueUrls = queueUrls;
        return this;
    }

    public int getVisibilityTimeoutSeconds() {
        return visibilityTimeoutSeconds;
    }

    public SQSConfig setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
        this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        return this;
    }

    public int getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public SQSConfig setMessageRetentionPeriod(int messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
        return this;
    }

    public int getMaxReceiveCount() {
        return maxReceiveCount;
    }

    public SQSConfig setMaxReceiveCount(int maxReceiveCount) {
        this.maxReceiveCount = maxReceiveCount;
        return this;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public SQSConfig setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
        return this;
    }

    public int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    public SQSConfig setWaitTimeSeconds(int waitTimeSeconds) {
        this.waitTimeSeconds = waitTimeSeconds;
        return this;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public SQSConfig setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
        return this;
    }

    public boolean isEnableLongPolling() {
        return enableLongPolling;
    }

    public SQSConfig setEnableLongPolling(boolean enableLongPolling) {
        this.enableLongPolling = enableLongPolling;
        return this;
    }

    public boolean isEnableDeduplication() {
        return enableDeduplication;
    }

    public SQSConfig setEnableDeduplication(boolean enableDeduplication) {
        this.enableDeduplication = enableDeduplication;
        return this;
    }

    public boolean isEnableContentBasedDeduplication() {
        return enableContentBasedDeduplication;
    }

    public SQSConfig setEnableContentBasedDeduplication(boolean enableContentBasedDeduplication) {
        this.enableContentBasedDeduplication = enableContentBasedDeduplication;
        return this;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public SQSConfig setPollingInterval(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
        return this;
    }

    public int getMaxConcurrentProcessors() {
        return maxConcurrentProcessors;
    }

    public SQSConfig setMaxConcurrentProcessors(int maxConcurrentProcessors) {
        this.maxConcurrentProcessors = maxConcurrentProcessors;
        return this;
    }

    public int getProcessingThreadPoolSize() {
        return processingThreadPoolSize;
    }

    public SQSConfig setProcessingThreadPoolSize(int processingThreadPoolSize) {
        this.processingThreadPoolSize = processingThreadPoolSize;
        return this;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public SQSConfig setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
        return this;
    }

    public boolean isDeleteOnSuccess() {
        return deleteOnSuccess;
    }

    public SQSConfig setDeleteOnSuccess(boolean deleteOnSuccess) {
        this.deleteOnSuccess = deleteOnSuccess;
        return this;
    }

    public boolean isEnableBatchProcessing() {
        return enableBatchProcessing;
    }

    public SQSConfig setEnableBatchProcessing(boolean enableBatchProcessing) {
        this.enableBatchProcessing = enableBatchProcessing;
        return this;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public SQSConfig setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public DeadLetterQueueConfig getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public SQSConfig setDeadLetterQueue(DeadLetterQueueConfig deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
        return this;
    }

    /**
     * Get queue URL by name.
     *
     * @param queueName the queue name
     * @return the queue URL or null if not found
     */
    public String getQueueUrl(String queueName) {
        return queueUrls.get(queueName);
    }

    /**
     * Set queue URL for a specific queue name.
     *
     * @param queueName the queue name
     * @param queueUrl the queue URL
     * @return this config instance for chaining
     */
    public SQSConfig setQueueUrl(String queueName, String queueUrl) {
        this.queueUrls.put(queueName, queueUrl);
        return this;
    }

    /**
     * Validate the SQS configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (visibilityTimeoutSeconds < 0 || visibilityTimeoutSeconds > 43200) {
            throw new IllegalArgumentException("Visibility timeout must be between 0 and 43200 seconds");
        }

        if (messageRetentionPeriod < 60 || messageRetentionPeriod > 1209600) {
            throw new IllegalArgumentException("Message retention period must be between 60 and 1209600 seconds");
        }

        if (maxReceiveCount < 1 || maxReceiveCount > 1000) {
            throw new IllegalArgumentException("Max receive count must be between 1 and 1000");
        }

        if (maxMessages < 1 || maxMessages > 10) {
            throw new IllegalArgumentException("Max messages must be between 1 and 10");
        }

        if (waitTimeSeconds < 0 || waitTimeSeconds > 20) {
            throw new IllegalArgumentException("Wait time seconds must be between 0 and 20");
        }

        if (delaySeconds < 0 || delaySeconds > 900) {
            throw new IllegalArgumentException("Delay seconds must be between 0 and 900");
        }

        if (maxConcurrentProcessors <= 0) {
            throw new IllegalArgumentException("Max concurrent processors must be positive");
        }

        if (processingThreadPoolSize <= 0) {
            throw new IllegalArgumentException("Processing thread pool size must be positive");
        }

        if (processingTimeout == null || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("Processing timeout must be positive");
        }

        if (pollingInterval == null || pollingInterval.isNegative()) {
            throw new IllegalArgumentException("Polling interval must be positive");
        }

        if (batchSize < 1 || batchSize > 10) {
            throw new IllegalArgumentException("Batch size must be between 1 and 10");
        }

        if (deadLetterQueue != null) {
            deadLetterQueue.validate();
        }
    }

    /**
     * Dead Letter Queue configuration.
     */
    public static class DeadLetterQueueConfig {
        private boolean enabled = true;
        private String queueUrl;
        private int maxReceiveCount = 3;
        private Duration messageRetentionPeriod = Duration.ofDays(14);

        public boolean isEnabled() {
            return enabled;
        }

        public DeadLetterQueueConfig setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getQueueUrl() {
            return queueUrl;
        }

        public DeadLetterQueueConfig setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
            return this;
        }

        public int getMaxReceiveCount() {
            return maxReceiveCount;
        }

        public DeadLetterQueueConfig setMaxReceiveCount(int maxReceiveCount) {
            this.maxReceiveCount = maxReceiveCount;
            return this;
        }

        public Duration getMessageRetentionPeriod() {
            return messageRetentionPeriod;
        }

        public DeadLetterQueueConfig setMessageRetentionPeriod(Duration messageRetentionPeriod) {
            this.messageRetentionPeriod = messageRetentionPeriod;
            return this;
        }

        public void validate() {
            if (enabled && (queueUrl == null || queueUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("Dead letter queue URL must be specified when enabled");
            }

            if (maxReceiveCount <= 0) {
                throw new IllegalArgumentException("Dead letter queue max receive count must be positive");
            }

            if (messageRetentionPeriod == null || messageRetentionPeriod.isNegative()) {
                throw new IllegalArgumentException("Dead letter queue message retention period must be positive");
            }
        }
    }

    /**
     * Create a configuration optimized for high-throughput scenarios.
     *
     * @return high-throughput configuration
     */
    public static SQSConfig forHighThroughput() {
        return new SQSConfig()
                .setMaxMessages(10)
                .setWaitTimeSeconds(20)
                .setEnableLongPolling(true)
                .setMaxConcurrentProcessors(20)
                .setProcessingThreadPoolSize(50)
                .setEnableBatchProcessing(true)
                .setBatchSize(10)
                .setVisibilityTimeoutSeconds(180); // 3 minutes
    }

    /**
     * Create a configuration optimized for development.
     *
     * @return development configuration
     */
    public static SQSConfig forDevelopment() {
        return new SQSConfig()
                .setMaxMessages(1)
                .setWaitTimeSeconds(5)
                .setEnableLongPolling(false)
                .setMaxConcurrentProcessors(2)
                .setProcessingThreadPoolSize(5)
                .setEnableBatchProcessing(false)
                .setVisibilityTimeoutSeconds(60)
                .setPollingInterval(Duration.ofSeconds(10));
    }

    /**
     * Create a configuration optimized for reliability.
     *
     * @return reliability-focused configuration
     */
    public static SQSConfig forReliability() {
        return new SQSConfig()
                .setMaxMessages(5)
                .setWaitTimeSeconds(20)
                .setEnableLongPolling(true)
                .setMaxReceiveCount(5)
                .setVisibilityTimeoutSeconds(600) // 10 minutes
                .setProcessingTimeout(Duration.ofMinutes(8))
                .setMaxConcurrentProcessors(5);
    }

    @Override
    public String toString() {
        return String.format("SQSConfig{" +
                        "queueCount=%d, " +
                        "visibilityTimeoutSeconds=%d, " +
                        "maxMessages=%d, " +
                        "enableLongPolling=%s, " +
                        "maxConcurrentProcessors=%d, " +
                        "enableBatchProcessing=%s" +
                        "}",
                queueUrls.size(), visibilityTimeoutSeconds, maxMessages,
                enableLongPolling, maxConcurrentProcessors, enableBatchProcessing);
    }
}