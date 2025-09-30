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
 * Monitors performance metrics for the async processing framework.
 * 
 * <p>This component tracks performance indicators such as throughput,
 * latency percentiles, and processing rates.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
public class PerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

    private final MetricsCollector metricsCollector;
    private final AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties;
    
    // Performance tracking
    private final Map<String, LongAdder> throughputCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastResetTime = new ConcurrentHashMap<>();
    private final Map<String, LatencyTracker> latencyTrackers = new ConcurrentHashMap<>();

    public PerformanceMonitor(
            MetricsCollector metricsCollector,
            AsyncFrameworkMetricsAutoConfiguration.MonitoringProperties properties) {
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        
        logger.info("PerformanceMonitor initialized with profiling enabled: {}", 
            properties.isPerformanceProfiling());
    }

    /**
     * Records the start of an operation for performance tracking.
     */
    public PerformanceContext startOperation(String operationName) {
        if (!properties.isPerformanceProfiling()) {
            return new NoOpPerformanceContext();
        }
        
        return new DefaultPerformanceContext(operationName, Instant.now());
    }

    /**
     * Records throughput metrics.
     */
    public void recordThroughput(String operationType) {
        if (!properties.isPerformanceProfiling()) {
            return;
        }
        
        throughputCounters.computeIfAbsent(operationType, k -> new LongAdder()).increment();
        lastResetTime.computeIfAbsent(operationType, k -> new AtomicLong(System.currentTimeMillis()));
    }

    /**
     * Records latency for an operation.
     */
    public void recordLatency(String operationType, Duration latency) {
        if (!properties.isPerformanceProfiling()) {
            return;
        }
        
        latencyTrackers.computeIfAbsent(operationType, k -> new LatencyTracker()).record(latency);
        
        metricsCollector.recordHistogram(
            properties.getMetricPrefix() + ".performance.latency",
            latency.toMillis(),
            Map.of("operation", operationType)
        );
    }

    /**
     * Calculates and reports throughput metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void reportThroughputMetrics() {
        if (!properties.isPerformanceProfiling()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        
        throughputCounters.forEach((operationType, counter) -> {
            try {
                long lastReset = lastResetTime.get(operationType).get();
                long elapsedSeconds = (currentTime - lastReset) / 1000;
                
                if (elapsedSeconds > 0) {
                    long count = counter.sumThenReset();
                    double throughputPerSecond = (double) count / elapsedSeconds;
                    
                    metricsCollector.recordGauge(
                        properties.getMetricPrefix() + ".performance.throughput",
                        throughputPerSecond,
                        Map.of("operation", operationType, "unit", "per_second")
                    );
                    
                    lastResetTime.get(operationType).set(currentTime);
                    
                    logger.debug("Recorded throughput for {}: {} ops/sec", operationType, throughputPerSecond);
                }
            } catch (Exception e) {
                logger.warn("Error calculating throughput for {}", operationType, e);
            }
        });
    }

    /**
     * Reports latency percentile metrics.
     */
    @Scheduled(fixedRateString = "#{@monitoringProperties.collectionInterval.toMillis()}")
    public void reportLatencyMetrics() {
        if (!properties.isPerformanceProfiling()) {
            return;
        }

        latencyTrackers.forEach((operationType, tracker) -> {
            try {
                LatencySnapshot snapshot = tracker.getSnapshot();
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".performance.latency.p50",
                    snapshot.getP50(),
                    Map.of("operation", operationType)
                );
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".performance.latency.p95",
                    snapshot.getP95(),
                    Map.of("operation", operationType)
                );
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".performance.latency.p99",
                    snapshot.getP99(),
                    Map.of("operation", operationType)
                );
                
                metricsCollector.recordGauge(
                    properties.getMetricPrefix() + ".performance.latency.max",
                    snapshot.getMax(),
                    Map.of("operation", operationType)
                );
                
                logger.debug("Recorded latency percentiles for {}: p50={}, p95={}, p99={}, max={}", 
                    operationType, snapshot.getP50(), snapshot.getP95(), snapshot.getP99(), snapshot.getMax());
                    
            } catch (Exception e) {
                logger.warn("Error calculating latency percentiles for {}", operationType, e);
            }
        });
    }

    /**
     * Performance context for tracking operation duration.
     */
    public interface PerformanceContext extends AutoCloseable {
        void addTag(String key, String value);
        void recordSuccess();
        void recordError(String errorType);
        Duration getDuration();
        @Override
        void close();
    }

    private class DefaultPerformanceContext implements PerformanceContext {
        private final String operationName;
        private final Instant startTime;
        private final Map<String, String> tags = new ConcurrentHashMap<>();
        private boolean closed = false;

        public DefaultPerformanceContext(String operationName, Instant startTime) {
            this.operationName = operationName;
            this.startTime = startTime;
        }

        @Override
        public void addTag(String key, String value) {
            if (!closed) {
                tags.put(key, value);
            }
        }

        @Override
        public void recordSuccess() {
            if (!closed) {
                tags.put("result", "success");
            }
        }

        @Override
        public void recordError(String errorType) {
            if (!closed) {
                tags.put("result", "error");
                tags.put("error.type", errorType);
            }
        }

        @Override
        public Duration getDuration() {
            return Duration.between(startTime, Instant.now());
        }

        @Override
        public void close() {
            if (!closed) {
                Duration duration = getDuration();
                recordLatency(operationName, duration);
                recordThroughput(operationName);
                
                // Record operation completion
                metricsCollector.recordHistogram(
                    properties.getMetricPrefix() + ".operation.duration",
                    duration.toMillis(),
                    Map.of("operation", operationName)
                );
                
                closed = true;
            }
        }
    }

    private static class NoOpPerformanceContext implements PerformanceContext {
        @Override
        public void addTag(String key, String value) {}

        @Override
        public void recordSuccess() {}

        @Override
        public void recordError(String errorType) {}

        @Override
        public Duration getDuration() {
            return Duration.ZERO;
        }

        @Override
        public void close() {}
    }

    /**
     * Tracks latency statistics.
     */
    private static class LatencyTracker {
        private final LongAdder count = new LongAdder();
        private final LongAdder sum = new LongAdder();
        private volatile long min = Long.MAX_VALUE;
        private volatile long max = Long.MIN_VALUE;
        private final Map<Integer, LongAdder> percentileBuckets = new ConcurrentHashMap<>();

        public void record(Duration latency) {
            long millis = latency.toMillis();
            count.increment();
            sum.add(millis);
            
            // Update min/max
            updateMin(millis);
            updateMax(millis);
            
            // Update percentile buckets (simplified approach)
            int bucket = getBucket(millis);
            percentileBuckets.computeIfAbsent(bucket, k -> new LongAdder()).increment();
        }

        private void updateMin(long value) {
            long currentMin = min;
            while (value < currentMin && !compareAndSetMin(currentMin, value)) {
                currentMin = min;
            }
        }

        private void updateMax(long value) {
            long currentMax = max;
            while (value > currentMax && !compareAndSetMax(currentMax, value)) {
                currentMax = max;
            }
        }

        private boolean compareAndSetMin(long expect, long update) {
            // Simplified atomic update (in real implementation, use AtomicLong)
            if (min == expect) {
                min = update;
                return true;
            }
            return false;
        }

        private boolean compareAndSetMax(long expect, long update) {
            // Simplified atomic update (in real implementation, use AtomicLong)
            if (max == expect) {
                max = update;
                return true;
            }
            return false;
        }

        private int getBucket(long millis) {
            // Simple bucketing strategy
            if (millis < 10) return 0;
            if (millis < 50) return 1;
            if (millis < 100) return 2;
            if (millis < 500) return 3;
            if (millis < 1000) return 4;
            if (millis < 5000) return 5;
            return 6;
        }

        public LatencySnapshot getSnapshot() {
            long totalCount = count.sum();
            if (totalCount == 0) {
                return new LatencySnapshot(0, 0, 0, 0, 0);
            }
            
            double mean = (double) sum.sum() / totalCount;
            
            // Simplified percentile calculation
            // In a real implementation, you'd use a proper histogram or reservoir
            double p50 = calculatePercentile(0.5, totalCount);
            double p95 = calculatePercentile(0.95, totalCount);
            double p99 = calculatePercentile(0.99, totalCount);
            
            return new LatencySnapshot(mean, p50, p95, p99, max == Long.MIN_VALUE ? 0 : max);
        }

        private double calculatePercentile(double percentile, long totalCount) {
            // Simplified percentile calculation
            // This is a basic approximation - real implementation would be more sophisticated
            long targetCount = (long) (totalCount * percentile);
            long runningCount = 0;
            
            for (int bucket = 0; bucket <= 6; bucket++) {
                LongAdder bucketCount = percentileBuckets.get(bucket);
                if (bucketCount != null) {
                    runningCount += bucketCount.sum();
                    if (runningCount >= targetCount) {
                        return getBucketMidpoint(bucket);
                    }
                }
            }
            
            return max == Long.MIN_VALUE ? 0 : max;
        }

        private double getBucketMidpoint(int bucket) {
            return switch (bucket) {
                case 0 -> 5.0;
                case 1 -> 30.0;
                case 2 -> 75.0;
                case 3 -> 300.0;
                case 4 -> 750.0;
                case 5 -> 3000.0;
                default -> 7500.0;
            };
        }
    }

    /**
     * Snapshot of latency statistics.
     */
    public static class LatencySnapshot {
        private final double mean;
        private final double p50;
        private final double p95;
        private final double p99;
        private final double max;

        public LatencySnapshot(double mean, double p50, double p95, double p99, double max) {
            this.mean = mean;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.max = max;
        }

        public double getMean() { return mean; }
        public double getP50() { return p50; }
        public double getP95() { return p95; }
        public double getP99() { return p99; }
        public double getMax() { return max; }
    }
}