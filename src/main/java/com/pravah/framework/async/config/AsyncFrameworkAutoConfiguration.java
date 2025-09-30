package com.pravah.framework.async.config;

import com.pravah.framework.async.api.AsyncDAO;
import com.pravah.framework.async.api.Queue;
import com.pravah.framework.async.api.StorageClient;
import com.pravah.framework.async.aws.AWSClientFactory;
import com.pravah.framework.async.aws.DynamoDBAsyncDAO;
import com.pravah.framework.async.aws.S3StorageClient;
import com.pravah.framework.async.aws.SQSQueue;
import com.pravah.framework.async.model.AsyncRequestQueueType;
import com.pravah.framework.async.monitoring.MetricsCollector;
import com.pravah.framework.async.monitoring.MicrometerMetricsCollector;
import com.pravah.framework.async.services.AsyncRequestService;
import com.pravah.framework.async.services.CircuitBreakerManager;
import com.pravah.framework.async.services.DeadLetterQueueHandler;
import com.pravah.framework.async.services.RetryHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main auto-configuration class for the Async Processing Framework.
 * 
 * <p>This configuration class provides automatic setup for:
 * <ul>
 *   <li>Core framework components (DAO, Queue, Storage)</li>
 *   <li>AWS service clients and integrations</li>
 *   <li>Configuration property validation and binding</li>
 *   <li>Conditional bean creation based on available dependencies</li>
 *   <li>Proper bean ordering and dependency resolution</li>
 *   <li>Health checks and monitoring integration</li>
 * </ul>
 * 
 * <p>The configuration is activated when:
 * <ul>
 *   <li>The property {@code async.framework.enabled} is {@code true} (default)</li>
 *   <li>Required AWS SDK classes are present on the classpath</li>
 *   <li>Spring Boot auto-configuration is enabled</li>
 * </ul>
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "async.framework", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
@ConditionalOnClass({
    software.amazon.awssdk.services.dynamodb.DynamoDbClient.class,
    software.amazon.awssdk.services.sqs.SqsClient.class,
    software.amazon.awssdk.services.s3.S3Client.class
})
@EnableConfigurationProperties({
    AsyncFrameworkConfig.class,
    AWSConfig.class,
    DynamoDBConfig.class,
    SQSConfig.class,
    S3Config.class
})
@Import({
    SQSListenerAutoConfiguration.class,
    com.pravah.framework.async.monitoring.AsyncFrameworkMetricsAutoConfiguration.class,
    com.pravah.framework.async.tracing.TracingAutoConfiguration.class
})
@Validated
public class AsyncFrameworkAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AsyncFrameworkAutoConfiguration.class);

    /**
     * Post-construct validation and logging.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Async Processing Framework Auto-Configuration");
    }

    // ========================================
    // Configuration Properties Beans
    // ========================================

    // Configuration properties are already defined in their respective classes
    // with @ConfigurationProperties annotations, so we don't need to redefine them here

    // ========================================
    // AWS Client Factory and Infrastructure
    // ========================================

    /**
     * AWS client factory for creating and managing AWS service clients.
     *
     * @return AWS client factory
     */
    @Bean
    @ConditionalOnMissingBean(AWSClientFactory.class)
    @Order(1)
    public AWSClientFactory awsClientFactory() {
        logger.info("Creating AWSClientFactory bean");
        return new AWSClientFactory();
    }

    // ========================================
    // Metrics and Monitoring
    // ========================================

    /**
     * Fallback meter registry if none is provided.
     *
     * @return simple meter registry
     */
    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry simpleMeterRegistry() {
        logger.info("No MeterRegistry found, creating SimpleMeterRegistry as fallback");
        return new SimpleMeterRegistry();
    }

    /**
     * Metrics collector for framework monitoring.
     *
     * @param meterRegistry the meter registry
     * @return metrics collector
     */
    @Bean
    @ConditionalOnMissingBean(MetricsCollector.class)
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry) {
        logger.info("Creating MicrometerMetricsCollector with registry: {}", 
            meterRegistry.getClass().getSimpleName());
        return new MicrometerMetricsCollector(meterRegistry);
    }

    /**
     * No-op metrics collector when monitoring is disabled.
     *
     * @return no-op metrics collector
     */
    @Bean
    @ConditionalOnMissingBean(MetricsCollector.class)
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "enabled", 
        havingValue = "false"
    )
    public MetricsCollector noOpMetricsCollector() {
        logger.info("Creating NoOpMetricsCollector (monitoring disabled)");
        return new NoOpMetricsCollector();
    }

    // ========================================
    // Core Framework Components
    // ========================================

    /**
     * DynamoDB-based AsyncDAO implementation.
     *
     * @param awsClientFactory AWS client factory
     * @param awsConfig AWS configuration
     * @param dynamoDBConfig DynamoDB configuration
     * @param metricsCollector metrics collector
     * @return DynamoDB AsyncDAO implementation
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AsyncDAO.class)
    @DependsOn("awsClientFactory")
    @Order(2)
    public AsyncDAO asyncDAO(
            AWSClientFactory awsClientFactory,
            AsyncFrameworkConfig frameworkConfig,
            MetricsCollector metricsCollector) {
        
        AWSConfig awsConfig = frameworkConfig.getAws();
        DynamoDBConfig dynamoDBConfig = awsConfig.getDynamodb();
        
        logger.info("Creating DynamoDBAsyncDAO for table: {}", dynamoDBConfig.getTableName());
        return new DynamoDBAsyncDAO(awsClientFactory, awsConfig, dynamoDBConfig, metricsCollector);
    }

    /**
     * S3-based StorageClient implementation.
     *
     * @param awsClientFactory AWS client factory
     * @param frameworkConfig framework configuration
     * @param metricsCollector metrics collector
     * @return S3 StorageClient implementation
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(StorageClient.class)
    @ConditionalOnProperty(
        prefix = "async.framework.storage", 
        name = "enable-external-storage", 
        havingValue = "true", 
        matchIfMissing = true
    )
    @DependsOn("awsClientFactory")
    @Order(2)
    public StorageClient storageClient(
            AWSClientFactory awsClientFactory,
            AsyncFrameworkConfig frameworkConfig,
            MetricsCollector metricsCollector) {
        
        AWSConfig awsConfig = frameworkConfig.getAws();
        S3Config s3Config = awsConfig.getS3();
        
        logger.info("Creating S3StorageClient for bucket: {}", s3Config.getBucketName());
        return new S3StorageClient(awsClientFactory, awsConfig, s3Config, metricsCollector);
    }

    /**
     * Map of queue types to SQS queue implementations.
     *
     * @param awsClientFactory AWS client factory
     * @param frameworkConfig framework configuration
     * @param metricsCollector metrics collector
     * @return map of queue implementations
     */
    @Bean
    @ConditionalOnMissingBean(name = "queueMap")
    @DependsOn("awsClientFactory")
    @Order(2)
    public Map<AsyncRequestQueueType, Queue> queueMap(
            AWSClientFactory awsClientFactory,
            AsyncFrameworkConfig frameworkConfig,
            MetricsCollector metricsCollector) {
        
        AWSConfig awsConfig = frameworkConfig.getAws();
        SQSConfig sqsConfig = awsConfig.getSqs();
        
        logger.info("Creating SQS queue map with {} configured queues", 
            sqsConfig.getQueueUrls().size());
        
        Map<AsyncRequestQueueType, Queue> queueMap = new HashMap<>();
        
        // Create SQS queues for each configured queue type
        for (AsyncRequestQueueType queueType : AsyncRequestQueueType.values()) {
            String queueUrl = sqsConfig.getQueueUrl(queueType.name().toLowerCase().replace('_', '-'));
            if (queueUrl != null && !queueUrl.trim().isEmpty()) {
                logger.debug("Creating SQS queue for type: {} with URL: {}", queueType, queueUrl);
                SQSQueue sqsQueue = new SQSQueue(awsClientFactory, awsConfig, sqsConfig, 
                    queueUrl, metricsCollector);
                queueMap.put(queueType, sqsQueue);
            } else {
                logger.warn("No queue URL configured for queue type: {}", queueType);
            }
        }
        
        // Ensure at least a default queue exists
        if (queueMap.isEmpty()) {
            logger.warn("No queues configured, creating default queue");
            String defaultQueueUrl = sqsConfig.getQueueUrl("default");
            if (defaultQueueUrl != null) {
                SQSQueue defaultQueue = new SQSQueue(awsClientFactory, awsConfig, sqsConfig, 
                    defaultQueueUrl, metricsCollector);
                queueMap.put(AsyncRequestQueueType.DEFAULT, defaultQueue);
            }
        }
        
        logger.info("Created {} SQS queues", queueMap.size());
        return queueMap;
    }

    // ========================================
    // Service Layer Components
    // ========================================

    /**
     * Retry handler for managing retry logic and circuit breaker patterns.
     *
     * @param frameworkConfig framework configuration
     * @param metricsCollector metrics collector
     * @return retry handler
     */
    @Bean
    @ConditionalOnMissingBean(RetryHandler.class)
    @Order(3)
    public RetryHandler retryHandler(
            AsyncFrameworkConfig frameworkConfig,
            MetricsCollector metricsCollector) {
        
        logger.info("Creating RetryHandler with max attempts: {}", 
            frameworkConfig.getRetry().getMaxAttempts());
        return new RetryHandler(frameworkConfig.getRetry(), metricsCollector);
    }

    /**
     * Circuit breaker manager for managing circuit breaker states.
     *
     * @param frameworkConfig framework configuration
     * @param metricsCollector metrics collector
     * @return circuit breaker manager
     */
    @Bean
    @ConditionalOnMissingBean(CircuitBreakerManager.class)
    @Order(3)
    public CircuitBreakerManager circuitBreakerManager(
            AsyncFrameworkConfig frameworkConfig,
            MetricsCollector metricsCollector) {
        
        logger.info("Creating CircuitBreakerManager");
        return new CircuitBreakerManager(frameworkConfig, metricsCollector);
    }

    /**
     * Dead letter queue handler for managing failed message processing.
     *
     * @param queueMap map of queue implementations
     * @param asyncDAO async DAO
     * @param metricsCollector metrics collector
     * @param frameworkConfig framework configuration
     * @return dead letter queue handler
     */
    @Bean
    @ConditionalOnMissingBean(DeadLetterQueueHandler.class)
    @ConditionalOnProperty(
        prefix = "async.framework.retry", 
        name = "enable-dead-letter-queue", 
        havingValue = "true", 
        matchIfMissing = true
    )
    @DependsOn({"queueMap", "asyncDAO"})
    @Order(4)
    public DeadLetterQueueHandler deadLetterQueueHandler(
            Map<AsyncRequestQueueType, Queue> queueMap,
            AsyncDAO asyncDAO,
            MetricsCollector metricsCollector,
            AsyncFrameworkConfig frameworkConfig) {
        
        logger.info("Creating DeadLetterQueueHandler");
        return new DeadLetterQueueHandler(queueMap, asyncDAO, metricsCollector, frameworkConfig);
    }

    /**
     * Main async request service.
     *
     * @param asyncDAO async DAO
     * @param storageClient storage client (optional)
     * @param queueMap map of queue implementations
     * @param metricsCollector metrics collector
     * @param frameworkConfig framework configuration
     * @return async request service
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AsyncRequestService.class)
    @DependsOn({"asyncDAO", "queueMap"})
    @Order(5)
    public AsyncRequestService asyncRequestService(
            AsyncDAO asyncDAO,
            StorageClient storageClient,
            Map<AsyncRequestQueueType, Queue> queueMap,
            MetricsCollector metricsCollector,
            AsyncFrameworkConfig frameworkConfig) {
        
        logger.info("Creating AsyncRequestService with {} queue types", queueMap.size());
        return new AsyncRequestService(asyncDAO, storageClient, queueMap, metricsCollector, frameworkConfig);
    }

    // ========================================
    // Thread Pool and Executor Configuration
    // ========================================

    /**
     * Scheduled executor service for framework background tasks.
     *
     * @param frameworkConfig framework configuration
     * @return scheduled executor service
     */
    @Bean(name = "asyncFrameworkScheduledExecutor")
    @ConditionalOnMissingBean(name = "asyncFrameworkScheduledExecutor")
    public ScheduledExecutorService asyncFrameworkScheduledExecutor(AsyncFrameworkConfig frameworkConfig) {
        int poolSize = Math.max(2, frameworkConfig.getProcessing().getCorePoolSize() / 4);
        logger.info("Creating scheduled executor service with pool size: {}", poolSize);
        return Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "async-framework-scheduled-");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Task executor for async request processing.
     *
     * @param frameworkConfig framework configuration
     * @return task executor
     */
    @Bean(name = "asyncFrameworkTaskExecutor")
    @ConditionalOnMissingBean(name = "asyncFrameworkTaskExecutor")
    public Executor asyncFrameworkTaskExecutor(AsyncFrameworkConfig frameworkConfig) {
        int poolSize = frameworkConfig.getProcessing().getCorePoolSize();
        logger.info("Creating task executor with pool size: {}", poolSize);
        return Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "async-framework-task-");
            t.setDaemon(false);
            return t;
        });
    }

    // ========================================
    // Health Indicators
    // ========================================

    /**
     * Health indicator for the async framework.
     *
     * @param awsClientFactory AWS client factory
     * @param asyncRequestService async request service
     * @return health indicator
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "enable-health-checks", 
        havingValue = "true", 
        matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "asyncFrameworkHealthIndicator")
    public HealthIndicator asyncFrameworkHealthIndicator(
            AWSClientFactory awsClientFactory,
            AsyncRequestService asyncRequestService) {
        
        logger.info("Creating AsyncFrameworkHealthIndicator");
        return new AsyncFrameworkHealthIndicator(awsClientFactory, asyncRequestService);
    }

    // ========================================
    // Helper Classes
    // ========================================

    /**
     * No-op metrics collector implementation.
     */
    private static class NoOpMetricsCollector implements MetricsCollector {
        
        @Override
        public void recordRequestStarted(com.pravah.framework.async.model.AsyncRequestType requestType) {}

        @Override
        public void recordRequestQueued(com.pravah.framework.async.model.AsyncRequestType requestType) {}

        @Override
        public void recordRequestLoaded(com.pravah.framework.async.model.AsyncRequestType requestType) {}

        @Override
        public void recordServiceInitialized() {}

        @Override
        public void recordListenerInitialized() {}

        @Override
        public void recordMessageReceived(com.pravah.framework.async.model.AsyncRequestQueueType queueType) {}

        @Override
        public void recordMessageProcessed(com.pravah.framework.async.model.AsyncRequestQueueType queueType, java.time.Duration processingTime) {}

        @Override
        public void recordMessageFailed(com.pravah.framework.async.model.AsyncRequestQueueType queueType, String errorType, java.time.Duration processingTime) {}

        @Override
        public void recordDeadLetterMessage(String requestId) {}

        @Override
        public void recordDeadLetterProcessingFailed(String requestId, String errorType) {}

        @Override
        public void recordStatusUpdate(com.pravah.framework.async.model.AsyncRequestStatus status) {}

        @Override
        public void recordRequestCompleted(com.pravah.framework.async.model.AsyncRequestType requestType, java.time.Duration processingTime) {}

        @Override
        public void recordRequestCompleted(com.pravah.framework.async.model.AsyncRequestType requestType) {}

        @Override
        public void recordRequestFailed(com.pravah.framework.async.model.AsyncRequestType requestType, String errorCode) {}

        @Override
        public void recordRetryAttempt(String requestId, int attemptNumber) {}

        @Override
        public void recordRetryExhausted(String requestId, com.pravah.framework.async.model.AsyncRequestType requestType) {}

        @Override
        public void recordQueueDepth(String queueName, int depth) {}

        @Override
        public void recordApiCall(String apiName, boolean success, java.time.Duration responseTime) {}

        @Override
        public void recordStorageOperation(String operation, boolean success, long size, java.time.Duration duration) {}

        @Override
        public void recordDatabaseOperation(String operation, boolean success, java.time.Duration duration) {}

        @Override
        public void recordCircuitBreakerState(String serviceName, String state) {}

        @Override
        public void recordPayloadSize(com.pravah.framework.async.model.AsyncRequestType requestType, long payloadSize, boolean isInline) {}

        @Override
        public void recordThreadPoolMetrics(String poolName, int activeThreads, int queueSize, long completedTasks) {}

        @Override
        public void recordMemoryUsage(long usedMemory, long maxMemory) {}

        @Override
        public void recordCustomMetric(String metricName, double value, java.util.Map<String, String> tags) {}

        @Override
        public void incrementCounter(String counterName, java.util.Map<String, String> tags) {}

        @Override
        public void recordGauge(String gaugeName, double value, java.util.Map<String, String> tags) {}

        @Override
        public void recordHistogram(String histogramName, double value, java.util.Map<String, String> tags) {}

        @Override
        public TimerSample startTimer(String timerName, java.util.Map<String, String> tags) {
            return new NoOpTimerSample();
        }

        @Override
        public void recordHealthCheck(String componentName, boolean healthy, java.time.Duration responseTime) {}

        @Override
        public MetricsSnapshot getMetricsSnapshot() {
            return new MetricsSnapshot(System.currentTimeMillis(), 
                java.util.Collections.emptyMap(), 
                java.util.Collections.emptyMap(), 
                java.util.Collections.emptyMap());
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setEnabled(boolean enabled) {}

        @Override
        public String getRegistryType() {
            return "NoOp";
        }

        // Additional methods that might be missing from the interface
        @Override
        public void recordCircuitBreakerOpen(String serviceName) {}

        @Override
        public void recordCircuitBreakerFailure(String serviceName) {}

        @Override
        public void recordCircuitBreakerSuccess(String serviceName) {}

        @Override
        public void recordDeadLetterQueueReprocessed(String requestId) {}

        @Override
        public void recordDeadLetterQueueReprocessError(String requestId, String errorType) {}

        @Override
        public void recordRetryExhausted(String requestId, int attemptNumber) {}

        @Override
        public void recordDeadLetterQueueSent(String requestId, String reason) {}

        @Override
        public void recordRetrySuccess(String requestId, int attemptNumber) {}

        @Override
        public void recordCircuitBreakerStateChange(String serviceName, com.pravah.framework.async.services.CircuitBreakerManager.CircuitBreakerState state) {}

        @Override
        public void recordDeadLetterQueueError(String requestId, String errorType) {}

        @Override
        public void recordRetryFailure(String requestId, int attemptNumber, com.pravah.framework.async.exception.ErrorCode errorCode) {}

        private static class NoOpTimerSample implements TimerSample {
            @Override
            public java.time.Duration stop() {
                return java.time.Duration.ZERO;
            }

            @Override
            public java.time.Duration stop(java.util.Map<String, String> additionalTags) {
                return java.time.Duration.ZERO;
            }
        }
    }

    /**
     * Health indicator implementation for the async framework.
     */
    private static class AsyncFrameworkHealthIndicator implements HealthIndicator {
        
        private final AWSClientFactory awsClientFactory;
        private final AsyncRequestService asyncRequestService;

        public AsyncFrameworkHealthIndicator(AWSClientFactory awsClientFactory, 
                                           AsyncRequestService asyncRequestService) {
            this.awsClientFactory = awsClientFactory;
            this.asyncRequestService = asyncRequestService;
        }

        @Override
        public org.springframework.boot.actuate.health.Health health() {
            try {
                boolean awsHealthy = awsClientFactory.isHealthy();
                boolean serviceHealthy = asyncRequestService.isHealthy();
                
                if (awsHealthy && serviceHealthy) {
                    return org.springframework.boot.actuate.health.Health.up()
                        .withDetail("aws", "UP")
                        .withDetail("service", "UP")
                        .withDetail("activeRequests", asyncRequestService.getActiveRequestCount())
                        .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("aws", awsHealthy ? "UP" : "DOWN")
                        .withDetail("service", serviceHealthy ? "UP" : "DOWN")
                        .build();
                }
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}