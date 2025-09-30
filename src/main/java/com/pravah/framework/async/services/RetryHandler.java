package com.pravah.framework.async.services;

import com.pravah.framework.async.config.AsyncFrameworkConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.monitoring.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Component responsible for handling retry logic with exponential backoff,
 * circuit breaker pattern, and dead letter queue integration.
 * <br>
 * This component provides configurable retry mechanisms for async request processing
 * with support for different retry strategies based on error types.
 *
 * @author Ankur Rai
 * @version 1.0
 */
@Component
public class RetryHandler {

    private static final Logger logger = LoggerFactory.getLogger(RetryHandler.class);

    private final AsyncFrameworkConfig.RetryConfig retryConfig;
    private final CircuitBreakerManager circuitBreakerManager;
    private final DeadLetterQueueHandler deadLetterQueueHandler;
    private final MetricsCollector metricsCollector;

    /**
     * Constructor for RetryHandler.
     *
     * @param frameworkConfig the framework configuration
     * @param circuitBreakerManager the circuit breaker manager
     * @param deadLetterQueueHandler the dead letter queue handler
     * @param metricsCollector the metrics collector
     */
    public RetryHandler(AsyncFrameworkConfig frameworkConfig,
                       CircuitBreakerManager circuitBreakerManager,
                       DeadLetterQueueHandler deadLetterQueueHandler,
                       MetricsCollector metricsCollector) {
        this.retryConfig = frameworkConfig.getRetry();
        this.circuitBreakerManager = circuitBreakerManager;
        this.deadLetterQueueHandler = deadLetterQueueHandler;
        this.metricsCollector = metricsCollector;
    }

    /**
     * Execute an operation with retry logic and circuit breaker protection.
     *
     * @param request the async request being processed
     * @param operation the operation to execute
     * @param <T> the return type of the operation
     * @return CompletableFuture with the operation result
     */
    public <T> CompletableFuture<T> executeWithRetry(AsyncRequest request, 
                                                    Supplier<CompletableFuture<T>> operation) {
        return executeWithRetry(request, operation, 0);
    }

    /**
     * Execute an operation with retry logic, starting from a specific attempt number.
     *
     * @param request the async request being processed
     * @param operation the operation to execute
     * @param currentAttempt the current attempt number (0-based)
     * @param <T> the return type of the operation
     * @return CompletableFuture with the operation result
     */
    private <T> CompletableFuture<T> executeWithRetry(AsyncRequest request,
                                                     Supplier<CompletableFuture<T>> operation,
                                                     int currentAttempt) {
        String requestId = request.getRequestId();
        String circuitBreakerKey = getCircuitBreakerKey(request);

        // Check circuit breaker state
        if (circuitBreakerManager.isOpen(circuitBreakerKey)) {
            logger.warn("Circuit breaker is open for request: {}, key: {}", requestId, circuitBreakerKey);
            metricsCollector.recordCircuitBreakerOpen(circuitBreakerKey);
            
            AsyncFrameworkException exception = new AsyncFrameworkException(
                ErrorCode.CIRCUIT_BREAKER_OPEN,
                "Circuit breaker is open",
                requestId
            );
            return CompletableFuture.failedFuture(exception);
        }

        logger.debug("Executing operation for request: {}, attempt: {}", requestId, currentAttempt + 1);
        
        return operation.get()
            .handle((result, throwable) -> {
                if (throwable == null) {
                    // Success - record success and reset circuit breaker
                    logger.debug("Operation succeeded for request: {} on attempt: {}", requestId, currentAttempt + 1);
                    circuitBreakerManager.recordSuccess(circuitBreakerKey);
                    metricsCollector.recordRetrySuccess(requestId, currentAttempt);
                    return CompletableFuture.completedFuture(result);
                } else {
                    // Failure - handle retry logic
                    return handleFailure(request, operation, currentAttempt, throwable);
                }
            })
            .thenCompose(future -> future);
    }

    /**
     * Handle operation failure and determine retry strategy.
     *
     * @param request the async request being processed
     * @param operation the operation to potentially retry
     * @param currentAttempt the current attempt number
     * @param throwable the exception that occurred
     * @param <T> the return type of the operation
     * @return CompletableFuture with retry result or failure
     */
    private <T> CompletableFuture<T> handleFailure(AsyncRequest request,
                                                  Supplier<CompletableFuture<T>> operation,
                                                  int currentAttempt,
                                                  Throwable throwable) {
        String requestId = request.getRequestId();
        String circuitBreakerKey = getCircuitBreakerKey(request);
        
        // Convert to AsyncFrameworkException if needed
        AsyncFrameworkException exception = (throwable instanceof AsyncFrameworkException) 
            ? (AsyncFrameworkException) throwable
            : AsyncFrameworkException.fromCause(throwable, requestId);

        logger.warn("Operation failed for request: {} on attempt: {}, error: {}", 
                   requestId, currentAttempt + 1, exception.getFormattedMessage());

        // Record failure in circuit breaker
        circuitBreakerManager.recordFailure(circuitBreakerKey);
        metricsCollector.recordRetryFailure(requestId, currentAttempt, exception.getErrorCode());

        // Check if we should retry
        if (!shouldRetry(exception, currentAttempt)) {
            logger.error("Retry exhausted for request: {} after {} attempts", requestId, currentAttempt + 1);
            
            // Send to dead letter queue if enabled
            if (retryConfig.isEnableDeadLetterQueue()) {
                deadLetterQueueHandler.sendToDeadLetterQueue(request, exception);
            }
            
            metricsCollector.recordRetryExhausted(requestId, currentAttempt);
            
            AsyncFrameworkException retryExhaustedException = new AsyncFrameworkException(
                ErrorCode.RETRY_EXHAUSTED,
                String.format("All retry attempts exhausted after %d attempts", currentAttempt + 1),
                requestId,
                exception
            );
            
            return CompletableFuture.failedFuture(retryExhaustedException);
        }

        // Calculate delay for next retry
        Duration delay = calculateRetryDelay(exception, currentAttempt);
        
        logger.info("Retrying request: {} in {} seconds (attempt {} of {})", 
                   requestId, delay.getSeconds(), currentAttempt + 2, retryConfig.getMaxAttempts());

        // Schedule retry after delay
        return CompletableFuture
            .supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS))
            .thenCompose(v -> executeWithRetry(request, operation, currentAttempt + 1));
    }

    /**
     * Determine if an operation should be retried based on the exception and attempt count.
     *
     * @param exception the exception that occurred
     * @param currentAttempt the current attempt number (0-based)
     * @return true if should retry, false otherwise
     */
    private boolean shouldRetry(AsyncFrameworkException exception, int currentAttempt) {
        // Check if we've exceeded max attempts
        if (currentAttempt >= retryConfig.getMaxAttempts() - 1) {
            return false;
        }

        // Check if the error is retryable
        if (!exception.isRetryable()) {
            logger.debug("Error is not retryable: {}", exception.getErrorCode());
            return false;
        }

        // Special handling for specific error types
        ErrorCode errorCode = exception.getErrorCode();

        return switch (errorCode) {
            case AUTHENTICATION_ERROR, AUTHORIZATION_ERROR, VALIDATION_ERROR, CONFIGURATION_ERROR,
                 SERIALIZATION_ERROR ->
                // These errors are not retryable
                    false;
            case RATE_LIMIT_EXCEEDED ->
                // Always retry rate limit errors with longer delays
                    true;
            case CIRCUIT_BREAKER_OPEN ->
                // Don't retry if circuit breaker is open
                    false;
            default ->
                // For other retryable errors, use the default retry logic
                    true;
        };
    }

    /**
     * Calculate the delay for the next retry attempt using exponential backoff with jitter.
     *
     * @param exception the exception that occurred
     * @param currentAttempt the current attempt number (0-based)
     * @return the delay duration for the next retry
     */
    private Duration calculateRetryDelay(AsyncFrameworkException exception, int currentAttempt) {
        // Start with the error-specific recommended delay or initial delay
        Duration baseDelay = exception.getErrorCode() != null 
            ? Duration.ofSeconds(exception.getRecommendedRetryDelaySeconds())
            : retryConfig.getInitialDelay();

        // Apply exponential backoff
        double multiplier = Math.pow(retryConfig.getBackoffMultiplier(), currentAttempt);
        long delayMillis = (long) (baseDelay.toMillis() * multiplier);
        
        // Apply jitter (±25% randomization)
        double jitterFactor = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.5); // 0.75 to 1.25
        delayMillis = (long) (delayMillis * jitterFactor);
        
        Duration calculatedDelay = Duration.ofMillis(delayMillis);
        
        // Ensure we don't exceed the maximum delay
        if (calculatedDelay.compareTo(retryConfig.getMaxDelay()) > 0) {
            calculatedDelay = retryConfig.getMaxDelay();
        }

        logger.debug("Calculated retry delay: {} seconds for attempt: {}", 
                    calculatedDelay.getSeconds(), currentAttempt + 1);
        
        return calculatedDelay;
    }

    /**
     * Get the circuit breaker key for a request.
     * This groups requests by type and API for circuit breaker isolation.
     *
     * @param request the async request
     * @return circuit breaker key
     */
    private String getCircuitBreakerKey(AsyncRequest request) {
        return String.format("%s:%s", 
                           request.getType().name(), 
                           request.getClass().getSimpleName());
    }

    /**
     * Check if retry is enabled for the framework.
     *
     * @return true if retry is enabled
     */
    public boolean isRetryEnabled() {
        return retryConfig.getMaxAttempts() > 1;
    }

    /**
     * Get the maximum number of retry attempts configured.
     *
     * @return maximum retry attempts
     */
    public int getMaxRetryAttempts() {
        return retryConfig.getMaxAttempts();
    }

    /**
     * Get the initial retry delay configured.
     *
     * @return initial retry delay
     */
    public Duration getInitialRetryDelay() {
        return retryConfig.getInitialDelay();
    }

    /**
     * Get the maximum retry delay configured.
     *
     * @return maximum retry delay
     */
    public Duration getMaxRetryDelay() {
        return retryConfig.getMaxDelay();
    }

    /**
     * Get the backoff multiplier configured.
     *
     * @return backoff multiplier
     */
    public double getBackoffMultiplier() {
        return retryConfig.getBackoffMultiplier();
    }
}