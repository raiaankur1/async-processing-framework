package com.pravah.framework.async.monitoring;

import com.pravah.framework.async.model.AsyncRequestQueueType;
import com.pravah.framework.async.model.AsyncRequestStatus;
import com.pravah.framework.async.model.AsyncRequestType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Micrometer-based implementation of MetricsCollector.
 * 
 * <p>This implementation provides comprehensive metrics collection using
 * Micrometer framework, supporting various metric registries including
 * CloudWatch, Prometheus, and others.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(
    prefix = "async.framework.monitoring", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class MicrometerMetricsCollector implements MetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(MicrometerMetricsCollector.class);

    private final MeterRegistry meterRegistry;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    
    // Metric name constants
    private static final String METRIC_PREFIX = "async.framework";
    private static final String REQUEST_STARTED = METRIC_PREFIX + ".request.started";
    private static final String REQUEST_QUEUED = METRIC_PREFIX + ".request.queued";
    private static final String REQUEST_LOADED = METRIC_PREFIX + ".request.loaded";
    private static final String REQUEST_COMPLETED = METRIC_PREFIX + ".request.completed";
    private static final String REQUEST_FAILED = METRIC_PREFIX + ".request.failed";
    private static final String REQUEST_PROCESSING_TIME = METRIC_PREFIX + ".request.processing.time";
    private static final String SERVICE_INITIALIZED = METRIC_PREFIX + ".service.initialized";
    private static final String LISTENER_INITIALIZED = METRIC_PREFIX + ".listener.initialized";
    private static final String MESSAGE_RECEIVED = METRIC_PREFIX + ".message.received";
    private static final String MESSAGE_PROCESSED = METRIC_PREFIX + ".message.processed";
    private static final String MESSAGE_FAILED = METRIC_PREFIX + ".message.failed";
    private static final String MESSAGE_PROCESSING_TIME = METRIC_PREFIX + ".message.processing.time";
    private static final String DEAD_LETTER_MESSAGE = METRIC_PREFIX + ".dead.letter.message";
    private static final String DEAD_LETTER_PROCESSING_FAILED = METRIC_PREFIX + ".dead.letter.processing.failed";
    private static final String STATUS_UPDATE = METRIC_PREFIX + ".status.update";
    private static final String RETRY_ATTEMPT = METRIC_PREFIX + ".retry.attempt";
    private static final String RETRY_EXHAUSTED = METRIC_PREFIX + ".retry.exhausted";
    private static final String QUEUE_DEPTH = METRIC_PREFIX + ".queue.depth";
    private static final String API_CALL = METRIC_PREFIX + ".api.call";
    private static final String API_CALL_TIME = METRIC_PREFIX + ".api.call.time";
    private static final String STORAGE_OPERATION = METRIC_PREFIX + ".storage.operation";
    private static final String STORAGE_OPERATION_TIME = METRIC_PREFIX + ".storage.operation.time";
    private static final String DATABASE_OPERATION = METRIC_PREFIX + ".database.operation";
    private static final String DATABASE_OPERATION_TIME = METRIC_PREFIX + ".database.operation.time";
    private static final String CIRCUIT_BREAKER_STATE = METRIC_PREFIX + ".circuit.breaker.state";
    private static final String PAYLOAD_SIZE = METRIC_PREFIX + ".payload.size";
    private static final String THREAD_POOL_ACTIVE = METRIC_PREFIX + ".thread.pool.active";
    private static final String THREAD_POOL_QUEUE_SIZE = METRIC_PREFIX + ".thread.pool.queue.size";
    private static final String THREAD_POOL_COMPLETED = METRIC_PREFIX + ".thread.pool.completed";
    private static final String MEMORY_USED = METRIC_PREFIX + ".memory.used";
    private static final String MEMORY_MAX = METRIC_PREFIX + ".memory.max";
    private static final String HEALTH_CHECK = METRIC_PREFIX + ".health.check";
    private static final String HEALTH_CHECK_TIME = METRIC_PREFIX + ".health.check.time";

    public MicrometerMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        logger.info("MicrometerMetricsCollector initialized with registry: {}", 
            meterRegistry.getClass().getSimpleName());
    }

    @Override
    public void recordRequestStarted(AsyncRequestType requestType) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_STARTED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRequestQueued(AsyncRequestType requestType) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_QUEUED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRequestLoaded(AsyncRequestType requestType) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_LOADED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordServiceInitialized() {
        if (!enabled.get()) return;
        
        Counter.builder(SERVICE_INITIALIZED)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordListenerInitialized() {
        if (!enabled.get()) return;
        
        Counter.builder(LISTENER_INITIALIZED)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordMessageReceived(AsyncRequestQueueType queueType) {
        if (!enabled.get()) return;
        
        Counter.builder(MESSAGE_RECEIVED)
            .tag("queue.type", queueType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordMessageProcessed(AsyncRequestQueueType queueType, Duration processingTime) {
        if (!enabled.get()) return;
        
        Counter.builder(MESSAGE_PROCESSED)
            .tag("queue.type", queueType.toString())
            .register(meterRegistry)
            .increment();
            
        Timer.builder(MESSAGE_PROCESSING_TIME)
            .tag("queue.type", queueType.toString())
            .register(meterRegistry)
            .record(processingTime);
    }

    @Override
    public void recordMessageFailed(AsyncRequestQueueType queueType, String errorType, Duration processingTime) {
        if (!enabled.get()) return;
        
        Counter.builder(MESSAGE_FAILED)
            .tag("queue.type", queueType.toString())
            .tag("error.type", errorType)
            .register(meterRegistry)
            .increment();
            
        Timer.builder(MESSAGE_PROCESSING_TIME)
            .tag("queue.type", queueType.toString())
            .tag("result", "failed")
            .register(meterRegistry)
            .record(processingTime);
    }

    @Override
    public void recordDeadLetterMessage(String requestId) {
        if (!enabled.get()) return;
        
        Counter.builder(DEAD_LETTER_MESSAGE)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordDeadLetterProcessingFailed(String requestId, String errorType) {
        if (!enabled.get()) return;
        
        Counter.builder(DEAD_LETTER_PROCESSING_FAILED)
            .tag("error.type", errorType)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordStatusUpdate(AsyncRequestStatus status) {
        if (!enabled.get()) return;
        
        Counter.builder(STATUS_UPDATE)
            .tag("status", status.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRequestCompleted(AsyncRequestType requestType, Duration processingTime) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_COMPLETED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
            
        Timer.builder(REQUEST_PROCESSING_TIME)
            .tag("type", requestType.toString())
            .tag("result", "success")
            .register(meterRegistry)
            .record(processingTime);
    }

    @Override
    public void recordRequestCompleted(AsyncRequestType requestType) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_COMPLETED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRequestFailed(AsyncRequestType requestType, String errorCode) {
        if (!enabled.get()) return;
        
        Counter.builder(REQUEST_FAILED)
            .tag("type", requestType.toString())
            .tag("error.code", errorCode)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRetryAttempt(String requestId, int attemptNumber) {
        if (!enabled.get()) return;
        
        Counter.builder(RETRY_ATTEMPT)
            .tag("attempt", String.valueOf(attemptNumber))
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRetryExhausted(String requestId, AsyncRequestType requestType) {
        if (!enabled.get()) return;
        
        Counter.builder(RETRY_EXHAUSTED)
            .tag("type", requestType.toString())
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordQueueDepth(String queueName, int depth) {
        if (!enabled.get()) return;
        
        Gauge.builder(QUEUE_DEPTH, () -> depth)
            .tag("queue.name", queueName)
            .register(meterRegistry);
    }

    @Override
    public void recordApiCall(String apiName, boolean success, Duration responseTime) {
        if (!enabled.get()) return;
        
        Counter.builder(API_CALL)
            .tag("api.name", apiName)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
            
        Timer.builder(API_CALL_TIME)
            .tag("api.name", apiName)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(responseTime);
    }

    @Override
    public void recordStorageOperation(String operation, boolean success, long size, Duration duration) {
        if (!enabled.get()) return;
        
        Counter.builder(STORAGE_OPERATION)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
            
        Timer.builder(STORAGE_OPERATION_TIME)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(duration);
            
        if (size > 0) {
            recordHistogram(METRIC_PREFIX + ".storage.size", size, 
                Map.of("operation", operation, "success", String.valueOf(success)));
        }
    }

    @Override
    public void recordDatabaseOperation(String operation, boolean success, Duration duration) {
        if (!enabled.get()) return;
        
        Counter.builder(DATABASE_OPERATION)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
            
        Timer.builder(DATABASE_OPERATION_TIME)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(duration);
    }

    @Override
    public void recordCircuitBreakerState(String serviceName, String state) {
        if (!enabled.get()) return;
        
        Counter.builder(CIRCUIT_BREAKER_STATE)
            .tag("service.name", serviceName)
            .tag("state", state)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordPayloadSize(AsyncRequestType requestType, long payloadSize, boolean isInline) {
        if (!enabled.get()) return;
        
        recordHistogram(PAYLOAD_SIZE, payloadSize, 
            Map.of("type", requestType.toString(), "inline", String.valueOf(isInline)));
    }

    @Override
    public void recordThreadPoolMetrics(String poolName, int activeThreads, int queueSize, long completedTasks) {
        if (!enabled.get()) return;
        
        Gauge.builder(THREAD_POOL_ACTIVE, () -> activeThreads)
            .tag("pool.name", poolName)
            .register(meterRegistry);
            
        Gauge.builder(THREAD_POOL_QUEUE_SIZE, () -> queueSize)
            .tag("pool.name", poolName)
            .register(meterRegistry);
            
        Gauge.builder(THREAD_POOL_COMPLETED, () -> completedTasks)
            .tag("pool.name", poolName)
            .register(meterRegistry);
    }

    @Override
    public void recordMemoryUsage(long usedMemory, long maxMemory) {
        if (!enabled.get()) return;
        
        Gauge.builder(MEMORY_USED, () -> usedMemory)
            .register(meterRegistry);
            
        Gauge.builder(MEMORY_MAX, () -> maxMemory)
            .register(meterRegistry);
    }

    @Override
    public void recordCustomMetric(String metricName, double value, Map<String, String> tags) {
        if (!enabled.get()) return;
        
        recordGauge(metricName, value, tags);
    }

    @Override
    public void incrementCounter(String counterName, Map<String, String> tags) {
        if (!enabled.get()) return;
        
        Counter.Builder builder = Counter.builder(counterName);
        if (tags != null) {
            tags.forEach(builder::tag);
        }
        builder.register(meterRegistry).increment();
    }

    @Override
    public void recordGauge(String gaugeName, double value, Map<String, String> tags) {
        if (!enabled.get()) return;
        
        Gauge.Builder<?> builder = Gauge.builder(gaugeName, () -> value);
        if (tags != null) {
            tags.forEach(builder::tag);
        }
        builder.register(meterRegistry);
    }

    @Override
    public void recordHistogram(String histogramName, double value, Map<String, String> tags) {
        if (!enabled.get()) return;
        
        Timer.Builder builder = Timer.builder(histogramName);
        if (tags != null) {
            tags.forEach(builder::tag);
        }
        builder.register(meterRegistry).record(Duration.ofMillis((long) value));
    }

    @Override
    public TimerSample startTimer(String timerName, Map<String, String> tags) {
        if (!enabled.get()) {
            return new NoOpTimerSample();
        }
        
        Timer.Builder builder = Timer.builder(timerName);
        if (tags != null) {
            tags.forEach(builder::tag);
        }
        Timer timer = builder.register(meterRegistry);
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return new MicrometerTimerSample(sample, timer);
    }

    @Override
    public void recordHealthCheck(String componentName, boolean healthy, Duration responseTime) {
        if (!enabled.get()) return;
        
        Counter.builder(HEALTH_CHECK)
            .tag("component", componentName)
            .tag("healthy", String.valueOf(healthy))
            .register(meterRegistry)
            .increment();
            
        Timer.builder(HEALTH_CHECK_TIME)
            .tag("component", componentName)
            .tag("healthy", String.valueOf(healthy))
            .register(meterRegistry)
            .record(responseTime);
    }

    @Override
    public MetricsSnapshot getMetricsSnapshot() {
        // Implementation would require collecting all current metric values
        // This is a simplified version
        Map<String, Double> counters = new ConcurrentHashMap<>();
        Map<String, Double> gauges = new ConcurrentHashMap<>();
        Map<String, HistogramSnapshot> histograms = new ConcurrentHashMap<>();
        
        // Collect metrics from registry (simplified)
        meterRegistry.getMeters().forEach(meter -> {
            String name = meter.getId().getName();
            if (meter instanceof Counter) {
                counters.put(name, ((Counter) meter).count());
            } else if (meter instanceof Gauge) {
                gauges.put(name, ((Gauge) meter).value());
            }
            // Add histogram collection logic as needed
        });
        
        return new MetricsSnapshot(System.currentTimeMillis(), counters, gauges, histograms);
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        logger.info("Metrics collection {}", enabled ? "enabled" : "disabled");
    }

    @Override
    public String getRegistryType() {
        return "Micrometer-" + meterRegistry.getClass().getSimpleName();
    }

    // Inner classes for timer implementation

    private static class MicrometerTimerSample implements TimerSample {
        private final Timer.Sample sample;
        private final Timer timer;

        public MicrometerTimerSample(Timer.Sample sample, Timer timer) {
            this.sample = sample;
            this.timer = timer;
        }

        @Override
        public Duration stop() {
            return Duration.ofNanos(sample.stop(timer));
        }

        @Override
        public Duration stop(Map<String, String> additionalTags) {
            // For additional tags, we'd need to create a new timer with those tags
            // This is a simplified implementation
            return stop();
        }
    }

    private static class NoOpTimerSample implements TimerSample {
        @Override
        public Duration stop() {
            return Duration.ZERO;
        }

        @Override
        public Duration stop(Map<String, String> additionalTags) {
            return Duration.ZERO;
        }
    }
}