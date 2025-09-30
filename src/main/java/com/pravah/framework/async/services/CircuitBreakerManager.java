package com.pravah.framework.async.services;

import com.pravah.framework.async.config.AsyncFrameworkConfig;
import com.pravah.framework.async.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker manager that implements the circuit breaker pattern
 * for external service failures.
 * <br>
 * The circuit breaker has three states:
 * - CLOSED: Normal operation, requests are allowed through
 * - OPEN: Circuit is open, requests are rejected immediately
 * - HALF_OPEN: Testing state, limited requests are allowed to test service recovery
 *
 * @author Ankur Rai
 * @version 1.0
 */
@Component
public class CircuitBreakerManager {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerManager.class);

    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final CircuitBreakerConfig config;
    private final MetricsCollector metricsCollector;

    /**
     * Constructor for CircuitBreakerManager.
     *
     * @param frameworkConfig the framework configuration
     * @param metricsCollector the metrics collector
     */
    public CircuitBreakerManager(AsyncFrameworkConfig frameworkConfig, MetricsCollector metricsCollector) {
        this.config = new CircuitBreakerConfig(frameworkConfig);
        this.metricsCollector = metricsCollector;
    }

    /**
     * Check if the circuit breaker is open for the given key.
     *
     * @param key the circuit breaker key
     * @return true if circuit is open, false otherwise
     */
    public boolean isOpen(String key) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(key);
        return circuitBreaker.isOpen();
    }

    /**
     * Record a successful operation for the circuit breaker.
     *
     * @param key the circuit breaker key
     */
    public void recordSuccess(String key) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(key);
        circuitBreaker.recordSuccess();
        metricsCollector.recordCircuitBreakerSuccess(key);
    }

    /**
     * Record a failed operation for the circuit breaker.
     *
     * @param key the circuit breaker key
     */
    public void recordFailure(String key) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(key);
        circuitBreaker.recordFailure();
        metricsCollector.recordCircuitBreakerFailure(key);
    }

    /**
     * Get the current state of the circuit breaker.
     *
     * @param key the circuit breaker key
     * @return the current state
     */
    public CircuitBreakerState getState(String key) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(key);
        return circuitBreaker != null ? circuitBreaker.getState() : CircuitBreakerState.CLOSED;
    }

    /**
     * Get metrics for the circuit breaker.
     *
     * @param key the circuit breaker key
     * @return circuit breaker metrics
     */
    public CircuitBreakerMetrics getMetrics(String key) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(key);
        return circuitBreaker != null ? circuitBreaker.getMetrics() : new CircuitBreakerMetrics();
    }

    /**
     * Reset the circuit breaker to closed state.
     *
     * @param key the circuit breaker key
     */
    public void reset(String key) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(key);
        if (circuitBreaker != null) {
            circuitBreaker.reset();
            logger.info("Circuit breaker reset for key: {}", key);
        }
    }

    /**
     * Get or create a circuit breaker for the given key.
     *
     * @param key the circuit breaker key
     * @return the circuit breaker instance
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String key) {
        return circuitBreakers.computeIfAbsent(key, k -> {
            logger.debug("Creating new circuit breaker for key: {}", k);
            return new CircuitBreaker(k, config, metricsCollector);
        });
    }

    /**
     * Circuit breaker states.
     */
    public enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, rejecting requests
        HALF_OPEN  // Testing recovery
    }

    /**
     * Circuit breaker implementation.
     */
    private static class CircuitBreaker {
        private final String key;
        private final CircuitBreakerConfig config;
        private final MetricsCollector metricsCollector;
        
        private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile Instant lastFailureTime = Instant.now();
        private volatile Instant stateChangeTime = Instant.now();

        CircuitBreaker(String key, CircuitBreakerConfig config, MetricsCollector metricsCollector) {
            this.key = key;
            this.config = config;
            this.metricsCollector = metricsCollector;
        }

        boolean isOpen() {
            CircuitBreakerState currentState = state.get();
            
            switch (currentState) {
                case CLOSED:
                    return false;
                    
                case OPEN:
                    // Check if we should transition to half-open
                    if (shouldTransitionToHalfOpen()) {
                        if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                            stateChangeTime = Instant.now();
                            logger.info("Circuit breaker transitioning to HALF_OPEN for key: {}", key);
                            metricsCollector.recordCircuitBreakerStateChange(key, CircuitBreakerState.HALF_OPEN);
                        }
                        return false; // Allow request in half-open state
                    }
                    return true;
                    
                case HALF_OPEN:
                    return false; // Allow limited requests in half-open state
                    
                default:
                    return false;
            }
        }

        void recordSuccess() {
            successCount.incrementAndGet();
            requestCount.incrementAndGet();
            
            CircuitBreakerState currentState = state.get();
            
            if (currentState == CircuitBreakerState.HALF_OPEN) {
                // In half-open state, successful requests can close the circuit
                if (successCount.get() >= config.getHalfOpenSuccessThreshold()) {
                    if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
                        reset();
                        logger.info("Circuit breaker closed after successful recovery for key: {}", key);
                        metricsCollector.recordCircuitBreakerStateChange(key, CircuitBreakerState.CLOSED);
                    }
                }
            } else if (currentState == CircuitBreakerState.CLOSED) {
                // Reset failure count on successful requests in closed state
                if (successCount.get() % config.getSuccessResetThreshold() == 0) {
                    failureCount.set(0);
                }
            }
        }

        void recordFailure() {
            int failures = failureCount.incrementAndGet();
            requestCount.incrementAndGet();
            lastFailureTime = Instant.now();
            
            CircuitBreakerState currentState = state.get();
            
            if (currentState == CircuitBreakerState.CLOSED) {
                // Check if we should open the circuit
                if (failures >= config.getFailureThreshold() && 
                    shouldConsiderFailureRate()) {
                    if (state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
                        stateChangeTime = Instant.now();
                        logger.warn("Circuit breaker opened due to {} failures for key: {}", failures, key);
                        metricsCollector.recordCircuitBreakerStateChange(key, CircuitBreakerState.OPEN);
                    }
                }
            } else if (currentState == CircuitBreakerState.HALF_OPEN) {
                // Any failure in half-open state should open the circuit
                if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
                    stateChangeTime = Instant.now();
                    logger.warn("Circuit breaker reopened due to failure in half-open state for key: {}", key);
                    metricsCollector.recordCircuitBreakerStateChange(key, CircuitBreakerState.OPEN);
                }
            }
        }

        CircuitBreakerState getState() {
            return state.get();
        }

        CircuitBreakerMetrics getMetrics() {
            return new CircuitBreakerMetrics(
                state.get(),
                failureCount.get(),
                successCount.get(),
                requestCount.get(),
                stateChangeTime,
                lastFailureTime
            );
        }

        void reset() {
            failureCount.set(0);
            successCount.set(0);
            requestCount.set(0);
            stateChangeTime = Instant.now();
        }

        private boolean shouldTransitionToHalfOpen() {
            Instant now = Instant.now();
            Duration timeSinceOpen = Duration.between(stateChangeTime, now);
            return timeSinceOpen.compareTo(config.getOpenTimeout()) >= 0;
        }

        private boolean shouldConsiderFailureRate() {
            // Only consider failure rate if we have enough requests
            int requests = requestCount.get();
            if (requests < config.getMinimumRequestThreshold()) {
                return false;
            }
            
            // Check failure rate
            double failureRate = (double) failureCount.get() / requests;
            return failureRate >= config.getFailureRateThreshold();
        }
    }

    /**
     * Circuit breaker configuration.
     */
    private static class CircuitBreakerConfig {
        private final int failureThreshold;
        private final double failureRateThreshold;
        private final int minimumRequestThreshold;
        private final Duration openTimeout;
        private final int halfOpenSuccessThreshold;
        private final int successResetThreshold;

        CircuitBreakerConfig(AsyncFrameworkConfig frameworkConfig) {
            // Default circuit breaker configuration
            this.failureThreshold = 5; // Open after 5 failures
            this.failureRateThreshold = 0.5; // 50% failure rate
            this.minimumRequestThreshold = 10; // Minimum 10 requests to consider failure rate
            this.openTimeout = Duration.ofMinutes(1); // Stay open for 1 minute
            this.halfOpenSuccessThreshold = 3; // Need 3 successes to close from half-open
            this.successResetThreshold = 10; // Reset failure count after 10 successes
        }

        int getFailureThreshold() { return failureThreshold; }
        double getFailureRateThreshold() { return failureRateThreshold; }
        int getMinimumRequestThreshold() { return minimumRequestThreshold; }
        Duration getOpenTimeout() { return openTimeout; }
        int getHalfOpenSuccessThreshold() { return halfOpenSuccessThreshold; }
        int getSuccessResetThreshold() { return successResetThreshold; }
    }

    /**
     * Circuit breaker metrics.
     */
    public static class CircuitBreakerMetrics {
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final int requestCount;
        private final Instant stateChangeTime;
        private final Instant lastFailureTime;

        public CircuitBreakerMetrics() {
            this(CircuitBreakerState.CLOSED, 0, 0, 0, Instant.now(), Instant.now());
        }

        public CircuitBreakerMetrics(CircuitBreakerState state, int failureCount, int successCount,
                                   int requestCount, Instant stateChangeTime, Instant lastFailureTime) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.requestCount = requestCount;
            this.stateChangeTime = stateChangeTime;
            this.lastFailureTime = lastFailureTime;
        }

        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public int getRequestCount() { return requestCount; }
        public Instant getStateChangeTime() { return stateChangeTime; }
        public Instant getLastFailureTime() { return lastFailureTime; }
        
        public double getFailureRate() {
            return requestCount > 0 ? (double) failureCount / requestCount : 0.0;
        }
    }
}