package com.pravah.framework.async.config;

import com.pravah.framework.async.monitoring.MetricsCollector;
import com.pravah.framework.async.monitoring.MicrometerMetricsCollector;
import com.pravah.framework.async.services.SQSMessageListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for SQS message listener integration.
 * 
 * <p>This configuration class provides automatic setup for:
 * <ul>
 *   <li>SQS message listener components</li>
 *   <li>Polling-based SQS message processing</li>
 *   <li>Metrics collection for message processing</li>
 *   <li>Error handling and retry mechanisms</li>
 * </ul>
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(SQSMessageListener.class)
@ConditionalOnProperty(
    prefix = "async.framework.listener", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
@EnableConfigurationProperties({
    AsyncFrameworkConfig.class,
    SQSListenerConfig.SQSListenerProperties.class
})
@EnableScheduling
public class SQSListenerAutoConfiguration {

    /**
     * Creates SQS listener properties bean if not already present.
     *
     * @return SQS listener properties
     */
    @Bean
    @ConditionalOnMissingBean
    public SQSListenerConfig.SQSListenerProperties sqsListenerProperties() {
        return new SQSListenerConfig.SQSListenerProperties();
    }



    /**
     * Creates Micrometer-based metrics collector if Micrometer is available.
     *
     * @param meterRegistry the meter registry
     * @return metrics collector
     */
    @Bean
    @ConditionalOnMissingBean(MetricsCollector.class)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public MetricsCollector micrometerMetricsCollector(MeterRegistry meterRegistry) {
        return new MicrometerMetricsCollector(meterRegistry);
    }

    /**
     * Creates a no-op metrics collector if Micrometer is not available.
     *
     * @return no-op metrics collector
     */
    @Bean
    @ConditionalOnMissingBean(MetricsCollector.class)
    public MetricsCollector noOpMetricsCollector() {
        return new NoOpMetricsCollector();
    }

    /**
     * No-op implementation of MetricsCollector for when metrics are disabled.
     */
    public static class NoOpMetricsCollector implements MetricsCollector {
        
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
}