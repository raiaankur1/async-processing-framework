package com.pravah.framework.async.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Tracer for async operations, managing spans across async request lifecycle.
 * 
 * @author Async Framework
 * @since 1.0.0
 */
public class AsyncOperationTracer {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationTracer.class);
    
    private final Tracer tracer;
    private final CorrelationIdManager correlationIdManager;
    private final Map<String, SpanContext> activeSpans = new ConcurrentHashMap<>();
    
    // Span operation names
    public static final String ASYNC_REQUEST_CREATE = "async.request.create";
    public static final String ASYNC_REQUEST_PROCESS = "async.request.process";
    public static final String ASYNC_REQUEST_RETRY = "async.request.retry";
    public static final String ASYNC_REQUEST_COMPLETE = "async.request.complete";
    public static final String ASYNC_REQUEST_FAIL = "async.request.fail";
    public static final String ASYNC_API_CALL = "async.api.call";
    public static final String ASYNC_QUEUE_SEND = "async.queue.send";
    public static final String ASYNC_QUEUE_RECEIVE = "async.queue.receive";
    public static final String ASYNC_STORAGE_STORE = "async.storage.store";
    public static final String ASYNC_STORAGE_RETRIEVE = "async.storage.retrieve";
    
    // Common tag keys
    public static final String TAG_REQUEST_ID = "async.request_id";
    public static final String TAG_REQUEST_TYPE = "async.request_type";
    public static final String TAG_APP_ID = "async.app_id";
    public static final String TAG_API_NAME = "async.api_name";
    public static final String TAG_RETRY_COUNT = "async.retry_count";
    public static final String TAG_QUEUE_TYPE = "async.queue_type";
    public static final String TAG_PAYLOAD_SIZE = "async.payload_size";
    public static final String TAG_STORAGE_TYPE = "async.storage_type";
    public static final String TAG_OPERATION_RESULT = "async.operation_result";
    
    public AsyncOperationTracer(Tracer tracer, CorrelationIdManager correlationIdManager) {
        this.tracer = tracer;
        this.correlationIdManager = correlationIdManager;
    }
    
    /**
     * Starts tracing for async request creation
     */
    public Span startAsyncRequestCreation(String requestId, String requestType, String appId) {
        Span span = tracer.nextSpan()
                .name(ASYNC_REQUEST_CREATE)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_REQUEST_TYPE, requestType)
                .tag(TAG_APP_ID, appId)
                .start();
        
        // Store span context for later use
        activeSpans.put(requestId, new SpanContext(span, Instant.now()));
        
        // Set correlation context
        correlationIdManager.setCorrelationId(correlationIdManager.getCurrentCorrelationId());
        correlationIdManager.setRequestId(requestId);
        
        logger.debug("Started tracing for async request creation: {}", requestId);
        return span;
    }
    
    /**
     * Starts tracing for async request processing
     */
    public Span startAsyncRequestProcessing(String requestId, String requestType, int retryCount) {
        Span span = tracer.nextSpan()
                .name(ASYNC_REQUEST_PROCESS)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_REQUEST_TYPE, requestType)
                .tag(TAG_RETRY_COUNT, String.valueOf(retryCount))
                .start();
        
        // Update span context
        activeSpans.put(requestId + "_process", new SpanContext(span, Instant.now()));
        
        // Set correlation context
        correlationIdManager.setRequestId(requestId);
        
        logger.debug("Started tracing for async request processing: {} (retry: {})", requestId, retryCount);
        return span;
    }
    
    /**
     * Traces an API call within an async request
     */
    public <T> T traceApiCall(String requestId, String apiName, Supplier<T> supplier) {
        Span span = tracer.nextSpan()
                .name(ASYNC_API_CALL)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_API_NAME, apiName)
                .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Instant startTime = Instant.now();
            
            try {
                T result = supplier.get();
                
                Duration duration = Duration.between(startTime, Instant.now());
                span.tag("duration_ms", String.valueOf(duration.toMillis()));
                span.tag(TAG_OPERATION_RESULT, "success");
                
                logger.debug("API call {} for request {} completed successfully in {}ms", 
                    apiName, requestId, duration.toMillis());
                
                return result;
                
            } catch (Exception e) {
                span.tag(TAG_OPERATION_RESULT, "failure");
                span.tag("error.code", e.getClass().getSimpleName());
                span.tag("error.message", e.getMessage());
                span.error(e);
                
                Duration duration = Duration.between(startTime, Instant.now());
                logger.warn("API call {} for request {} failed after {}ms: {}", 
                    apiName, requestId, duration.toMillis(), e.getMessage());
                
                throw e;
            }
        } finally {
            span.end();
        }
    }
    
    /**
     * Traces queue operations
     */
    public <T> T traceQueueOperation(String operation, String requestId, String queueType, Supplier<T> supplier) {
        String spanName = operation.equals("send") ? ASYNC_QUEUE_SEND : ASYNC_QUEUE_RECEIVE;
        
        Span span = tracer.nextSpan()
                .name(spanName)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_QUEUE_TYPE, queueType)
                .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Instant startTime = Instant.now();
            
            try {
                T result = supplier.get();
                
                Duration duration = Duration.between(startTime, Instant.now());
                span.tag("duration_ms", String.valueOf(duration.toMillis()));
                span.tag(TAG_OPERATION_RESULT, "success");
                
                logger.debug("Queue {} operation for request {} completed successfully in {}ms", 
                    operation, requestId, duration.toMillis());
                
                return result;
                
            } catch (Exception e) {
                span.tag(TAG_OPERATION_RESULT, "failure");
                span.tag("error.code", e.getClass().getSimpleName());
                span.tag("error.message", e.getMessage());
                span.error(e);
                
                Duration duration = Duration.between(startTime, Instant.now());
                logger.warn("Queue {} operation for request {} failed after {}ms: {}", 
                    operation, requestId, duration.toMillis(), e.getMessage());
                
                throw e;
            }
        } finally {
            span.end();
        }
    }
    
    /**
     * Traces storage operations
     */
    public <T> T traceStorageOperation(String operation, String requestId, String storageType, 
                                      long payloadSize, Supplier<T> supplier) {
        String spanName = operation.equals("store") ? ASYNC_STORAGE_STORE : ASYNC_STORAGE_RETRIEVE;
        
        Span span = tracer.nextSpan()
                .name(spanName)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_STORAGE_TYPE, storageType)
                .tag(TAG_PAYLOAD_SIZE, String.valueOf(payloadSize))
                .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Instant startTime = Instant.now();
            
            try {
                T result = supplier.get();
                
                Duration duration = Duration.between(startTime, Instant.now());
                span.tag("duration_ms", String.valueOf(duration.toMillis()));
                span.tag(TAG_OPERATION_RESULT, "success");
                
                logger.debug("Storage {} operation for request {} completed successfully in {}ms", 
                    operation, requestId, duration.toMillis());
                
                return result;
                
            } catch (Exception e) {
                span.tag(TAG_OPERATION_RESULT, "failure");
                span.tag("error.code", e.getClass().getSimpleName());
                span.tag("error.message", e.getMessage());
                span.error(e);
                
                Duration duration = Duration.between(startTime, Instant.now());
                logger.warn("Storage {} operation for request {} failed after {}ms: {}", 
                    operation, requestId, duration.toMillis(), e.getMessage());
                
                throw e;
            }
        } finally {
            span.end();
        }
    }
    
    /**
     * Completes async request tracing with success
     */
    public void completeAsyncRequestSuccess(String requestId, String requestType) {
        SpanContext spanContext = activeSpans.remove(requestId);
        if (spanContext != null) {
            Span span = spanContext.span;
            Duration totalDuration = Duration.between(spanContext.startTime, Instant.now());
            
            span.tag("total_duration_ms", String.valueOf(totalDuration.toMillis()));
            span.tag(TAG_OPERATION_RESULT, "success");
            span.end();
            
            logger.debug("Completed async request {} successfully after {}ms", 
                requestId, totalDuration.toMillis());
        }
        
        // Create completion span
        Span completionSpan = tracer.nextSpan()
                .name(ASYNC_REQUEST_COMPLETE)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_REQUEST_TYPE, requestType)
                .tag(TAG_OPERATION_RESULT, "success")
                .start();
        completionSpan.end();
    }
    
    /**
     * Completes async request tracing with failure
     */
    public void completeAsyncRequestFailure(String requestId, String requestType, Throwable error) {
        SpanContext spanContext = activeSpans.remove(requestId);
        if (spanContext != null) {
            Span span = spanContext.span;
            Duration totalDuration = Duration.between(spanContext.startTime, Instant.now());
            
            span.tag("total_duration_ms", String.valueOf(totalDuration.toMillis()));
            span.tag(TAG_OPERATION_RESULT, "failure");
            span.tag("error.code", error.getClass().getSimpleName());
            span.tag("error.message", error.getMessage());
            span.error(error);
            span.end();
            
            logger.debug("Completed async request {} with failure after {}ms: {}", 
                requestId, totalDuration.toMillis(), error.getMessage());
        }
        
        // Create failure span
        Span failureSpan = tracer.nextSpan()
                .name(ASYNC_REQUEST_FAIL)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_REQUEST_TYPE, requestType)
                .tag(TAG_OPERATION_RESULT, "failure")
                .tag("error.code", error.getClass().getSimpleName())
                .tag("error.message", error.getMessage())
                .start();
        failureSpan.error(error);
        failureSpan.end();
    }
    
    /**
     * Traces async request retry
     */
    public void traceAsyncRequestRetry(String requestId, String requestType, int retryCount, String reason) {
        Span retrySpan = tracer.nextSpan()
                .name(ASYNC_REQUEST_RETRY)
                .tag(TAG_REQUEST_ID, requestId)
                .tag(TAG_REQUEST_TYPE, requestType)
                .tag(TAG_RETRY_COUNT, String.valueOf(retryCount))
                .tag("retry.reason", reason)
                .start();
        retrySpan.end();
        
        logger.debug("Traced retry for async request {}: attempt {}, reason: {}", 
            requestId, retryCount, reason);
    }
    
    /**
     * Creates a traced CompletableFuture that propagates tracing context
     */
    public <T> CompletableFuture<T> traceAsync(String operationName, String requestId, 
                                              Supplier<CompletableFuture<T>> supplier) {
        Span span = tracer.nextSpan()
                .name(operationName)
                .tag(TAG_REQUEST_ID, requestId)
                .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            String correlationId = correlationIdManager.getCurrentCorrelationId();
            
            return supplier.get()
                    .whenComplete((result, throwable) -> {
                        correlationIdManager.withCorrelationContext(correlationId, requestId, () -> {
                            if (throwable != null) {
                                span.tag(TAG_OPERATION_RESULT, "failure");
                                span.tag("error.code", throwable.getClass().getSimpleName());
                                span.tag("error.message", throwable.getMessage());
                                span.error(throwable);
                            } else {
                                span.tag(TAG_OPERATION_RESULT, "success");
                            }
                            span.end();
                        });
                    });
        }
    }
    
    /**
     * Cleans up any orphaned spans for a request
     */
    public void cleanupSpans(String requestId) {
        activeSpans.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(requestId)) {
                entry.getValue().span.end();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Context holder for active spans
     */
    private static class SpanContext {
        final Span span;
        final Instant startTime;
        
        SpanContext(Span span, Instant startTime) {
            this.span = span;
            this.startTime = startTime;
        }
    }
}