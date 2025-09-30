package com.pravah.framework.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Main configuration class for the Async Processing Framework.
 * <br>
 * This class serves as the root configuration that binds to Spring Boot
 * configuration properties with the prefix "async.framework".
 *
 * @author Ankur Rai
 * @version 2.0
 */
@ConfigurationProperties(prefix = "async.framework")
@Validated
public class AsyncFrameworkConfig {

    /**
     * Whether the async framework is enabled.
     */
    private boolean enabled = true;

    /**
     * AWS-specific configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private AWSConfig aws = new AWSConfig();

    /**
     * Retry configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private RetryConfig retry = new RetryConfig();

    /**
     * Monitoring configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * Queue configurations mapped by queue name.
     */
    @NestedConfigurationProperty
    private Map<String, QueueConfig> queues = new HashMap<>();

    /**
     * Storage configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private StorageConfig storage = new StorageConfig();

    /**
     * Processing configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private ProcessingConfig processing = new ProcessingConfig();

    /**
     * Default constructor.
     */
    public AsyncFrameworkConfig() {
        // Initialize default queue configurations
        queues.put("default", new QueueConfig());
        queues.put("high-priority", new QueueConfig().setPriority(10));
        queues.put("low-priority", new QueueConfig().setPriority(1));
    }

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public AsyncFrameworkConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AWSConfig getAws() {
        return aws;
    }

    public AsyncFrameworkConfig setAws(AWSConfig aws) {
        this.aws = aws;
        return this;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public AsyncFrameworkConfig setRetry(RetryConfig retry) {
        this.retry = retry;
        return this;
    }

    public MonitoringConfig getMonitoring() {
        return monitoring;
    }

    public AsyncFrameworkConfig setMonitoring(MonitoringConfig monitoring) {
        this.monitoring = monitoring;
        return this;
    }

    public Map<String, QueueConfig> getQueues() {
        return queues;
    }

    public AsyncFrameworkConfig setQueues(Map<String, QueueConfig> queues) {
        this.queues = queues;
        return this;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public AsyncFrameworkConfig setStorage(StorageConfig storage) {
        this.storage = storage;
        return this;
    }

    public ProcessingConfig getProcessing() {
        return processing;
    }

    public AsyncFrameworkConfig setProcessing(ProcessingConfig processing) {
        this.processing = processing;
        return this;
    }

    /**
     * Validate the entire configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (aws != null) {
            aws.validate();
        }
        if (retry != null) {
            retry.validate();
        }
        if (monitoring != null) {
            monitoring.validate();
        }
        if (storage != null) {
            storage.validate();
        }
        if (processing != null) {
            processing.validate();
        }
        if (queues != null) {
            queues.values().forEach(QueueConfig::validate);
        }
    }

    /**
     * Retry configuration nested class.
     */
    public static class RetryConfig {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(30);
        private Duration maxDelay = Duration.ofMinutes(10);
        private double backoffMultiplier = 2.0;
        private boolean enableDeadLetterQueue = true;
        private String deadLetterQueueUrl;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public RetryConfig setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public RetryConfig setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public RetryConfig setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public RetryConfig setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public boolean isEnableDeadLetterQueue() {
            return enableDeadLetterQueue;
        }

        public RetryConfig setEnableDeadLetterQueue(boolean enableDeadLetterQueue) {
            this.enableDeadLetterQueue = enableDeadLetterQueue;
            return this;
        }

        public String getDeadLetterQueueUrl() {
            return deadLetterQueueUrl;
        }

        public RetryConfig setDeadLetterQueueUrl(String deadLetterQueueUrl) {
            this.deadLetterQueueUrl = deadLetterQueueUrl;
            return this;
        }

        public void validate() {
            if (maxAttempts < 0) {
                throw new IllegalArgumentException("Max attempts cannot be negative");
            }
            if (initialDelay == null || initialDelay.isNegative()) {
                throw new IllegalArgumentException("Initial delay must be positive");
            }
            if (maxDelay == null || maxDelay.isNegative()) {
                throw new IllegalArgumentException("Max delay must be positive");
            }
            if (backoffMultiplier <= 1.0) {
                throw new IllegalArgumentException("Backoff multiplier must be greater than 1.0");
            }
        }
    }

    /**
     * Monitoring configuration nested class.
     */
    public static class MonitoringConfig {
        private boolean enableMetrics = true;
        private boolean enableTracing = true;
        private boolean enableHealthChecks = true;
        private Duration metricsPublishInterval = Duration.ofMinutes(1);
        private String metricsNamespace = "AsyncFramework";

        public boolean isEnableMetrics() {
            return enableMetrics;
        }

        public MonitoringConfig setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }

        public boolean isEnableTracing() {
            return enableTracing;
        }

        public MonitoringConfig setEnableTracing(boolean enableTracing) {
            this.enableTracing = enableTracing;
            return this;
        }

        public boolean isEnableHealthChecks() {
            return enableHealthChecks;
        }

        public MonitoringConfig setEnableHealthChecks(boolean enableHealthChecks) {
            this.enableHealthChecks = enableHealthChecks;
            return this;
        }

        public Duration getMetricsPublishInterval() {
            return metricsPublishInterval;
        }

        public MonitoringConfig setMetricsPublishInterval(Duration metricsPublishInterval) {
            this.metricsPublishInterval = metricsPublishInterval;
            return this;
        }

        public String getMetricsNamespace() {
            return metricsNamespace;
        }

        public MonitoringConfig setMetricsNamespace(String metricsNamespace) {
            this.metricsNamespace = metricsNamespace;
            return this;
        }

        public void validate() {
            if (metricsPublishInterval == null || metricsPublishInterval.isNegative()) {
                throw new IllegalArgumentException("Metrics publish interval must be positive");
            }
        }
    }

    /**
     * Queue configuration nested class.
     */
    public static class QueueConfig {
        private String url;
        private int priority = 5;
        private int maxConcurrency = 10;
        private Duration visibilityTimeout = Duration.ofMinutes(5);
        private int maxReceiveCount = 3;

        public String getUrl() {
            return url;
        }

        public QueueConfig setUrl(String url) {
            this.url = url;
            return this;
        }

        public int getPriority() {
            return priority;
        }

        public QueueConfig setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public QueueConfig setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Duration getVisibilityTimeout() {
            return visibilityTimeout;
        }

        public QueueConfig setVisibilityTimeout(Duration visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        public int getMaxReceiveCount() {
            return maxReceiveCount;
        }

        public QueueConfig setMaxReceiveCount(int maxReceiveCount) {
            this.maxReceiveCount = maxReceiveCount;
            return this;
        }

        public void validate() {
            if (priority < 1 || priority > 10) {
                throw new IllegalArgumentException("Priority must be between 1 and 10");
            }
            if (maxConcurrency <= 0) {
                throw new IllegalArgumentException("Max concurrency must be positive");
            }
            if (visibilityTimeout == null || visibilityTimeout.isNegative()) {
                throw new IllegalArgumentException("Visibility timeout must be positive");
            }
            if (maxReceiveCount <= 0) {
                throw new IllegalArgumentException("Max receive count must be positive");
            }
        }
    }

    /**
     * Storage configuration nested class.
     */
    public static class StorageConfig {
        private boolean enableExternalStorage = true;
        private int inlinePayloadThreshold = 1024; // 1KB
        private int maxPayloadSize = 10 * 1024 * 1024; // 10MB
        private boolean compressPayloads = false;
        private Duration payloadTtl = Duration.ofDays(30);

        public boolean isEnableExternalStorage() {
            return enableExternalStorage;
        }

        public StorageConfig setEnableExternalStorage(boolean enableExternalStorage) {
            this.enableExternalStorage = enableExternalStorage;
            return this;
        }

        public int getInlinePayloadThreshold() {
            return inlinePayloadThreshold;
        }

        public StorageConfig setInlinePayloadThreshold(int inlinePayloadThreshold) {
            this.inlinePayloadThreshold = inlinePayloadThreshold;
            return this;
        }

        public int getMaxPayloadSize() {
            return maxPayloadSize;
        }

        public StorageConfig setMaxPayloadSize(int maxPayloadSize) {
            this.maxPayloadSize = maxPayloadSize;
            return this;
        }

        public boolean isCompressPayloads() {
            return compressPayloads;
        }

        public StorageConfig setCompressPayloads(boolean compressPayloads) {
            this.compressPayloads = compressPayloads;
            return this;
        }

        public Duration getPayloadTtl() {
            return payloadTtl;
        }

        public StorageConfig setPayloadTtl(Duration payloadTtl) {
            this.payloadTtl = payloadTtl;
            return this;
        }

        public void validate() {
            if (inlinePayloadThreshold < 0) {
                throw new IllegalArgumentException("Inline payload threshold cannot be negative");
            }
            if (maxPayloadSize <= 0) {
                throw new IllegalArgumentException("Max payload size must be positive");
            }
            if (inlinePayloadThreshold > maxPayloadSize) {
                throw new IllegalArgumentException("Inline payload threshold cannot exceed max payload size");
            }
            if (payloadTtl == null || payloadTtl.isNegative()) {
                throw new IllegalArgumentException("Payload TTL must be positive");
            }
        }
    }

    /**
     * Processing configuration nested class.
     */
    public static class ProcessingConfig {
        private int corePoolSize = 4;
        private int maxPoolSize = 20;
        private int queueCapacity = 100;
        private Duration keepAliveTime = Duration.ofMinutes(1);
        private boolean allowCoreThreadTimeOut = false;
        private String threadNamePrefix = "async-processing-";

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public ProcessingConfig setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public ProcessingConfig setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public ProcessingConfig setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Duration getKeepAliveTime() {
            return keepAliveTime;
        }

        public ProcessingConfig setKeepAliveTime(Duration keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public boolean isAllowCoreThreadTimeOut() {
            return allowCoreThreadTimeOut;
        }

        public ProcessingConfig setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
            return this;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public ProcessingConfig setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public void validate() {
            if (corePoolSize < 0) {
                throw new IllegalArgumentException("Core pool size cannot be negative");
            }
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("Max pool size must be positive");
            }
            if (corePoolSize > maxPoolSize) {
                throw new IllegalArgumentException("Core pool size cannot exceed max pool size");
            }
            if (queueCapacity < 0) {
                throw new IllegalArgumentException("Queue capacity cannot be negative");
            }
            if (keepAliveTime == null || keepAliveTime.isNegative()) {
                throw new IllegalArgumentException("Keep alive time must be positive");
            }
        }
    }
}