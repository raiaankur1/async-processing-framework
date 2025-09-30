package com.pravah.framework.async.monitoring;

import com.pravah.framework.async.model.AsyncRequestStatus;
import com.pravah.framework.async.model.AsyncRequestType;

import java.time.Duration;

/**
 * Metrics collection interface for async processing framework.
 * <br>
 * This interface provides comprehensive metrics collection capabilities
 * for monitoring async request processing performance and health.
 *
 * @author Ankur Rai
 * @version 1.0
 */
public interface MetricsCollector {

    /**
     * Record that a request has been started.
     *
     * @param requestType the type of request
     */
    void recordRequestStarted(AsyncRequestType requestType);

    /**
     * Record that a request has been queued.
     *
     * @param requestType the type of request
     */
    void recordRequestQueued(AsyncRequestType requestType);

    /**
     * Record that a request has been loaded from storage.
     *
     * @param requestType the type of request
     */
    void recordRequestLoaded(AsyncRequestType requestType);

    /**
     * Record that the service has been initialized.
     */
    void recordServiceInitialized();

    /**
     * Record a status update operation.
     *
     * @param status the new status
     */
    void recordStatusUpdate(AsyncRequestStatus status);

    /**
     * Record that a request has completed successfully.
     *
     * @param requestType the type of request
     * @param processingTime the total processing time
     */
    void recordRequestCompleted(AsyncRequestType requestType, Duration processingTime);

    /**
     * Record that a request has completed successfully (without duration).
     *
     * @param requestType the type of request
     */
    void recordRequestCompleted(AsyncRequestType requestType);

    /**
     * Record that a request has failed.
     *
     * @param requestType the type of request
     * @param errorCode the error code
     */
    void recordRequestFailed(AsyncRequestType requestType, String errorCode);

    /**
     * Record a retry attempt.
     *
     * @param requestId the request ID
     * @param attemptNumber the retry attempt number
     */
    void recordRetryAttempt(String requestId, int attemptNumber);

    /**
     * Record that retry attempts have been exhausted.
     *
     * @param requestId the request ID
     * @param requestType the type of request
     */
    void recordRetryExhausted(String requestId, AsyncRequestType requestType);

    /**
     * Record queue depth metrics.
     *
     * @param queueName the queue name
     * @param depth the current queue depth
     */
    void recordQueueDepth(String queueName, int depth);

    /**
     * Record API call metrics.
     *
     * @param apiName the API name
     * @param success whether the call was successful
     * @param responseTime the response time
     */
    void recordApiCall(String apiName, boolean success, Duration responseTime);

    /**
     * Record storage operation metrics.
     *
     * @param operation the operation type (GET, PUT, DELETE)
     * @param success whether the operation was successful
     * @param size the data size in bytes (for PUT operations)
     * @param duration the operation duration
     */
    void recordStorageOperation(String operation, boolean success, long size, Duration duration);

    /**
     * Record database operation metrics.
     *
     * @param operation the operation type (CREATE, UPDATE, DELETE, QUERY)
     * @param success whether the operation was successful
     * @param duration the operation duration
     */
    void recordDatabaseOperation(String operation, boolean success, Duration duration);

    /**
     * Record circuit breaker state changes.
     *
     * @param serviceName the service name
     * @param state the new state (OPEN, CLOSED, HALF_OPEN)
     */
    void recordCircuitBreakerState(String serviceName, String state);

    /**
     * Record payload size metrics.
     *
     * @param requestType the request type
     * @param payloadSize the payload size in bytes
     * @param isInline whether the payload is stored inline
     */
    void recordPayloadSize(AsyncRequestType requestType, long payloadSize, boolean isInline);

    /**
     * Record thread pool metrics.
     *
     * @param poolName the thread pool name
     * @param activeThreads number of active threads
     * @param queueSize queue size
     * @param completedTasks number of completed tasks
     */
    void recordThreadPoolMetrics(String poolName, int activeThreads, int queueSize, long completedTasks);

    /**
     * Record memory usage metrics.
     *
     * @param usedMemory used memory in bytes
     * @param maxMemory maximum memory in bytes
     */
    void recordMemoryUsage(long usedMemory, long maxMemory);

    /**
     * Record custom business metrics.
     *
     * @param metricName the metric name
     * @param value the metric value
     * @param tags additional tags for the metric
     */
    void recordCustomMetric(String metricName, double value, java.util.Map<String, String> tags);

    /**
     * Increment a counter metric.
     *
     * @param counterName the counter name
     * @param tags additional tags
     */
    void incrementCounter(String counterName, java.util.Map<String, String> tags);

    /**
     * Record a gauge metric.
     *
     * @param gaugeName the gauge name
     * @param value the gauge value
     * @param tags additional tags
     */
    void recordGauge(String gaugeName, double value, java.util.Map<String, String> tags);

    /**
     * Record a histogram metric.
     *
     * @param histogramName the histogram name
     * @param value the value to record
     * @param tags additional tags
     */
    void recordHistogram(String histogramName, double value, java.util.Map<String, String> tags);

    /**
     * Start a timer for measuring duration.
     *
     * @param timerName the timer name
     * @param tags additional tags
     * @return timer sample that can be stopped
     */
    TimerSample startTimer(String timerName, java.util.Map<String, String> tags);

    /**
     * Record health check results.
     *
     * @param componentName the component name
     * @param healthy whether the component is healthy
     * @param responseTime the health check response time
     */
    void recordHealthCheck(String componentName, boolean healthy, Duration responseTime);

    /**
     * Get current metrics snapshot.
     *
     * @return metrics snapshot
     */
    MetricsSnapshot getMetricsSnapshot();

    /**
     * Check if metrics collection is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Enable or disable metrics collection.
     *
     * @param enabled whether to enable metrics collection
     */
    void setEnabled(boolean enabled);

    /**
     * Get the metrics registry type (e.g., "Micrometer", "Dropwizard").
     *
     * @return registry type
     */
    String getRegistryType();

    /**
     * Timer sample for measuring durations.
     */
    interface TimerSample {
        /**
         * Stop the timer and record the duration.
         *
         * @return the measured duration
         */
        Duration stop();

        /**
         * Stop the timer with additional tags.
         *
         * @param additionalTags additional tags to add
         * @return the measured duration
         */
        Duration stop(java.util.Map<String, String> additionalTags);
    }

    /**
     * Snapshot of current metrics.
     */
    class MetricsSnapshot {
        private final long timestamp;
        private final java.util.Map<String, Double> counters;
        private final java.util.Map<String, Double> gauges;
        private final java.util.Map<String, HistogramSnapshot> histograms;

        public MetricsSnapshot(long timestamp,
                               java.util.Map<String, Double> counters,
                               java.util.Map<String, Double> gauges,
                               java.util.Map<String, HistogramSnapshot> histograms) {
            this.timestamp = timestamp;
            this.counters = counters;
            this.gauges = gauges;
            this.histograms = histograms;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public java.util.Map<String, Double> getCounters() {
            return counters;
        }

        public java.util.Map<String, Double> getGauges() {
            return gauges;
        }

        public java.util.Map<String, HistogramSnapshot> getHistograms() {
            return histograms;
        }

        public Double getCounter(String name) {
            return counters.get(name);
        }

        public Double getGauge(String name) {
            return gauges.get(name);
        }

        public HistogramSnapshot getHistogram(String name) {
            return histograms.get(name);
        }
    }

    /**
     * Histogram snapshot with percentiles.
     */
    class HistogramSnapshot {
        private final long count;
        private final double mean;
        private final double min;
        private final double max;
        private final double p50;
        private final double p95;
        private final double p99;

        public HistogramSnapshot(long count, double mean, double min, double max,
                                 double p50, double p95, double p99) {
            this.count = count;
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
        }

        public long getCount() {
            return count;
        }

        public double getMean() {
            return mean;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getP50() {
            return p50;
        }

        public double getP95() {
            return p95;
        }

        public double getP99() {
            return p99;
        }
    }
}