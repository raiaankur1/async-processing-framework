package com.pravah.framework.async.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks and analyzes error patterns in the async processing framework.
 * 
 * <p>This component provides detailed error tracking, categorization,
 * and trend analysis to help identify and resolve issues.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
public class ErrorTracker {

    private static final Logger logger = LoggerFactory.getLogger(ErrorTracker.class);

    private final MetricsCollector metricsCollector;
    private final AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties;
    
    // Error tracking
    private final Map<String, LongAdder> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastErrorTime = new ConcurrentHashMap<>();
    private final Map<String, ErrorRateTracker> errorRateTrackers = new ConcurrentHashMap<>();
    private final Map<String, String> lastErrorMessages = new ConcurrentHashMap<>();

    public ErrorTracker(
            MetricsCollector metricsCollector,
            AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties) {
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        
        logger.info("ErrorTracker initialized with detailed tracking enabled: {}", 
            properties.isDetailedErrorTracking());
    }

    /**
     * Records an error occurrence.
     */
    public void recordError(String errorType, String errorMessage, Throwable throwable) {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        try {
            String normalizedErrorType = normalizeErrorType(errorType);
            
            // Update counters
            errorCounters.computeIfAbsent(normalizedErrorType, k -> new LongAdder()).increment();
            lastErrorTime.put(normalizedErrorType, new AtomicLong(System.currentTimeMillis()));
            lastErrorMessages.put(normalizedErrorType, errorMessage);
            
            // Update error rate tracker
            errorRateTrackers.computeIfAbsent(normalizedErrorType, k -> new ErrorRateTracker())
                .recordError(Instant.now());
            
            // Record in metrics collector
            metricsCollector.incrementCounter(
                properties.getMetricPrefix() + ".errors.count",
                Map.of(
                    "error.type", normalizedErrorType,
                    "error.category", categorizeError(throwable)
                )
            );
            
            // Record error details if available
            if (throwable != null) {
                recordErrorDetails(normalizedErrorType, throwable);
            }
            
            logger.debug("Recorded error: type={}, message={}", normalizedErrorType, errorMessage);
            
        } catch (Exception e) {
            logger.warn("Error recording error metrics", e);
        }
    }

    /**
     * Records a business logic error.
     */
    public void recordBusinessError(String operation, String errorCode, String errorMessage) {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        metricsCollector.incrementCounter(
            properties.getMetricPrefix() + ".errors.business",
            Map.of(
                "operation", operation,
                "error.code", errorCode
            )
        );
        
        logger.debug("Recorded business error: operation={}, code={}, message={}", 
            operation, errorCode, errorMessage);
    }

    /**
     * Records a validation error.
     */
    public void recordValidationError(String field, String validationType, String errorMessage) {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        metricsCollector.incrementCounter(
            properties.getMetricPrefix() + ".errors.validation",
            Map.of(
                "field", field,
                "validation.type", validationType
            )
        );
        
        logger.debug("Recorded validation error: field={}, type={}, message={}", 
            field, validationType, errorMessage);
    }

    /**
     * Records a timeout error.
     */
    public void recordTimeoutError(String operation, Duration timeout, Duration actualDuration) {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        metricsCollector.incrementCounter(
            properties.getMetricPrefix() + ".errors.timeout",
            Map.of("operation", operation)
        );
        
        metricsCollector.recordHistogram(
            properties.getMetricPrefix() + ".timeout.duration",
            actualDuration.toMillis(),
            Map.of(
                "operation", operation,
                "timeout.configured", String.valueOf(timeout.toMillis())
            )
        );
        
        logger.debug("Recorded timeout error: operation={}, configured={}ms, actual={}ms", 
            operation, timeout.toMillis(), actualDuration.toMillis());
    }

    /**
     * Records a circuit breaker error.
     */
    public void recordCircuitBreakerError(String serviceName, String state) {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        metricsCollector.incrementCounter(
            properties.getMetricPrefix() + ".errors.circuit.breaker",
            Map.of(
                "service", serviceName,
                "state", state
            )
        );
        
        logger.debug("Recorded circuit breaker error: service={}, state={}", serviceName, state);
    }

    /**
     * Calculates and reports error rate metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void reportErrorRateMetrics() {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        errorRateTrackers.forEach((errorType, tracker) -> {
            try {
                ErrorRateSnapshot snapshot = tracker.getSnapshot();
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".errors.rate.1min",
                    snapshot.getRate1Min(),
                    Map.of("error.type", errorType)
                );
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".errors.rate.5min",
                    snapshot.getRate5Min(),
                    Map.of("error.type", errorType)
                );
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".errors.rate.15min",
                    snapshot.getRate15Min(),
                    Map.of("error.type", errorType)
                );
                
                logger.debug("Recorded error rates for {}: 1min={}, 5min={}, 15min={}", 
                    errorType, snapshot.getRate1Min(), snapshot.getRate5Min(), snapshot.getRate15Min());
                    
            } catch (Exception e) {
                logger.warn("Error calculating error rates for {}", errorType, e);
            }
        });
    }

    /**
     * Reports error summary metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void reportErrorSummaryMetrics() {
        if (!properties.isDetailedErrorTracking()) {
            return;
        }

        try {
            long totalErrors = errorCounters.values().stream()
                .mapToLong(LongAdder::sum)
                .sum();
                
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".errors.total",
                totalErrors,
                Map.of()
            );
            
            int uniqueErrorTypes = errorCounters.size();
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".errors.types.unique",
                uniqueErrorTypes,
                Map.of()
            );
            
            // Find most frequent error type
            String mostFrequentErrorType = errorCounters.entrySet().stream()
                .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.sum(), b.sum())))
                .map(Map.Entry::getKey)
                .orElse("none");
                
            metricsCollector.recordGauge(
                properties.getMetricPrefix() + ".errors.most.frequent.count",
                errorCounters.getOrDefault(mostFrequentErrorType, new LongAdder()).sum(),
                Map.of("error.type", mostFrequentErrorType)
            );
            
            logger.debug("Recorded error summary: total={}, unique_types={}, most_frequent={}", 
                totalErrors, uniqueErrorTypes, mostFrequentErrorType);
                
        } catch (Exception e) {
            logger.warn("Error calculating error summary metrics", e);
        }
    }

    /**
     * Gets error statistics for a specific error type.
     */
    public ErrorStatistics getErrorStatistics(String errorType) {
        String normalizedErrorType = normalizeErrorType(errorType);
        
        long count = errorCounters.getOrDefault(normalizedErrorType, new LongAdder()).sum();
        Long lastOccurrence = lastErrorTime.containsKey(normalizedErrorType) 
            ? lastErrorTime.get(normalizedErrorType).get() 
            : null;
        String lastMessage = lastErrorMessages.get(normalizedErrorType);
        
        ErrorRateSnapshot rateSnapshot = errorRateTrackers.containsKey(normalizedErrorType)
            ? errorRateTrackers.get(normalizedErrorType).getSnapshot()
            : new ErrorRateSnapshot(0, 0, 0);
            
        return new ErrorStatistics(count, lastOccurrence, lastMessage, rateSnapshot);
    }

    private String normalizeErrorType(String errorType) {
        if (errorType == null) {
            return "unknown";
        }
        return errorType.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    private String categorizeError(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        
        String className = throwable.getClass().getSimpleName().toLowerCase();
        
        if (className.contains("timeout")) {
            return "timeout";
        } else if (className.contains("connection") || className.contains("network")) {
            return "network";
        } else if (className.contains("validation") || className.contains("illegal")) {
            return "validation";
        } else if (className.contains("security") || className.contains("auth")) {
            return "security";
        } else if (className.contains("runtime")) {
            return "runtime";
        } else {
            return "application";
        }
    }

    private void recordErrorDetails(String errorType, Throwable throwable) {
        String stackTraceHash = String.valueOf(getStackTraceHash(throwable));
        
        metricsCollector.incrementCounter(
            properties.getMetricPrefix() + ".errors.stacktrace",
            Map.of(
                "error.type", errorType,
                "stacktrace.hash", stackTraceHash
            )
        );
        
        // Record error depth (number of caused-by exceptions)
        int depth = getExceptionDepth(throwable);
        metricsCollector.recordHistogram(
            properties.getMetricPrefix() + ".errors.depth",
            depth,
            Map.of("error.type", errorType)
        );
    }

    private int getStackTraceHash(Throwable throwable) {
        if (throwable.getStackTrace().length == 0) {
            return 0;
        }
        
        // Use first few stack trace elements for hash
        StringBuilder sb = new StringBuilder();
        int elementsToHash = Math.min(3, throwable.getStackTrace().length);
        
        for (int i = 0; i < elementsToHash; i++) {
            StackTraceElement element = throwable.getStackTrace()[i];
            sb.append(element.getClassName())
              .append(element.getMethodName())
              .append(element.getLineNumber());
        }
        
        return sb.toString().hashCode();
    }

    private int getExceptionDepth(Throwable throwable) {
        int depth = 0;
        Throwable current = throwable;
        
        while (current != null && depth < 10) { // Limit depth to prevent infinite loops
            depth++;
            current = current.getCause();
        }
        
        return depth;
    }

    /**
     * Tracks error rates over different time windows.
     */
    private static class ErrorRateTracker {
        private final LongAdder count1Min = new LongAdder();
        private final LongAdder count5Min = new LongAdder();
        private final LongAdder count15Min = new LongAdder();
        private volatile long lastReset1Min = System.currentTimeMillis();
        private volatile long lastReset5Min = System.currentTimeMillis();
        private volatile long lastReset15Min = System.currentTimeMillis();

        public void recordError(Instant timestamp) {
            long now = timestamp.toEpochMilli();
            
            // Reset counters if time windows have passed
            resetIfNeeded(now);
            
            count1Min.increment();
            count5Min.increment();
            count15Min.increment();
        }

        private void resetIfNeeded(long now) {
            if (now - lastReset1Min > 60_000) { // 1 minute
                count1Min.reset();
                lastReset1Min = now;
            }
            
            if (now - lastReset5Min > 300_000) { // 5 minutes
                count5Min.reset();
                lastReset5Min = now;
            }
            
            if (now - lastReset15Min > 900_000) { // 15 minutes
                count15Min.reset();
                lastReset15Min = now;
            }
        }

        public ErrorRateSnapshot getSnapshot() {
            long now = System.currentTimeMillis();
            resetIfNeeded(now);
            
            // Calculate rates per minute
            double rate1Min = count1Min.sum();
            double rate5Min = count5Min.sum() / 5.0;
            double rate15Min = count15Min.sum() / 15.0;
            
            return new ErrorRateSnapshot(rate1Min, rate5Min, rate15Min);
        }
    }

    /**
     * Snapshot of error rates.
     */
    public static class ErrorRateSnapshot {
        private final double rate1Min;
        private final double rate5Min;
        private final double rate15Min;

        public ErrorRateSnapshot(double rate1Min, double rate5Min, double rate15Min) {
            this.rate1Min = rate1Min;
            this.rate5Min = rate5Min;
            this.rate15Min = rate15Min;
        }

        public double getRate1Min() { return rate1Min; }
        public double getRate5Min() { return rate5Min; }
        public double getRate15Min() { return rate15Min; }
    }

    /**
     * Error statistics for a specific error type.
     */
    public static class ErrorStatistics {
        private final long totalCount;
        private final Long lastOccurrence;
        private final String lastMessage;
        private final ErrorRateSnapshot rateSnapshot;

        public ErrorStatistics(long totalCount, Long lastOccurrence, String lastMessage, ErrorRateSnapshot rateSnapshot) {
            this.totalCount = totalCount;
            this.lastOccurrence = lastOccurrence;
            this.lastMessage = lastMessage;
            this.rateSnapshot = rateSnapshot;
        }

        public long getTotalCount() { return totalCount; }
        public Long getLastOccurrence() { return lastOccurrence; }
        public String getLastMessage() { return lastMessage; }
        public ErrorRateSnapshot getRateSnapshot() { return rateSnapshot; }
    }
}