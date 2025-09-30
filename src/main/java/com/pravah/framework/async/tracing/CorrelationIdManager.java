package com.pravah.framework.async.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Manages correlation IDs for distributed tracing across async operations.
 * Ensures that correlation IDs are properly propagated through async boundaries.
 * 
 * @author Async Framework
 * @since 1.0.0
 */
public class CorrelationIdManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdManager.class);
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    
    @Autowired(required = false)
    private Tracer tracer;
    
    /**
     * Generates a new correlation ID
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Gets the current correlation ID from MDC or generates a new one
     */
    public String getCurrentCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = generateCorrelationId();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }
    
    /**
     * Sets the correlation ID in MDC and tracing context
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
        
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(CORRELATION_ID_KEY, correlationId);
            }
        }
        
        logger.debug("Set correlation ID: {}", correlationId);
    }
    
    /**
     * Sets the request ID in MDC and tracing context
     */
    public void setRequestId(String requestId) {
        MDC.put(REQUEST_ID_KEY, requestId);
        
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(REQUEST_ID_KEY, requestId);
            }
        }
        
        logger.debug("Set request ID: {}", requestId);
    }
    
    /**
     * Updates MDC with current tracing information
     */
    public void updateTracingContext() {
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                MDC.put(TRACE_ID_KEY, context.traceId());
                MDC.put(SPAN_ID_KEY, context.spanId());
            }
        }
    }
    
    /**
     * Clears all correlation context from MDC
     */
    public void clearContext() {
        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        logger.debug("Cleared correlation context");
    }
    
    /**
     * Executes a supplier with correlation context propagation
     */
    public <T> T withCorrelationContext(String correlationId, String requestId, Supplier<T> supplier) {
        String previousCorrelationId = MDC.get(CORRELATION_ID_KEY);
        String previousRequestId = MDC.get(REQUEST_ID_KEY);
        
        try {
            setCorrelationId(correlationId);
            if (requestId != null) {
                setRequestId(requestId);
            }
            updateTracingContext();
            
            return supplier.get();
        } finally {
            // Restore previous context
            if (previousCorrelationId != null) {
                MDC.put(CORRELATION_ID_KEY, previousCorrelationId);
            } else {
                MDC.remove(CORRELATION_ID_KEY);
            }
            
            if (previousRequestId != null) {
                MDC.put(REQUEST_ID_KEY, previousRequestId);
            } else {
                MDC.remove(REQUEST_ID_KEY);
            }
        }
    }
    
    /**
     * Executes a runnable with correlation context propagation
     */
    public void withCorrelationContext(String correlationId, String requestId, Runnable runnable) {
        withCorrelationContext(correlationId, requestId, () -> {
            runnable.run();
            return null;
        });
    }
    
    /**
     * Creates a CompletableFuture that propagates correlation context
     */
    public <T> CompletableFuture<T> supplyAsync(String correlationId, String requestId, 
                                               Supplier<T> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(() -> 
            withCorrelationContext(correlationId, requestId, supplier), executor);
    }
    
    /**
     * Creates a CompletableFuture that propagates correlation context
     */
    public <T> CompletableFuture<T> supplyAsync(String correlationId, String requestId, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> 
            withCorrelationContext(correlationId, requestId, supplier));
    }
    
    /**
     * Creates a CompletableFuture that propagates current correlation context
     */
    public <T> CompletableFuture<T> supplyAsyncWithCurrentContext(Supplier<T> supplier) {
        String correlationId = getCurrentCorrelationId();
        String requestId = MDC.get(REQUEST_ID_KEY);
        return supplyAsync(correlationId, requestId, supplier);
    }
    
    /**
     * Creates a CompletableFuture that propagates current correlation context
     */
    public <T> CompletableFuture<T> supplyAsyncWithCurrentContext(Supplier<T> supplier, Executor executor) {
        String correlationId = getCurrentCorrelationId();
        String requestId = MDC.get(REQUEST_ID_KEY);
        return supplyAsync(correlationId, requestId, supplier, executor);
    }
    
    /**
     * Wraps an executor to propagate correlation context
     */
    public Executor wrapExecutor(Executor executor) {
        return new CorrelationContextPropagatingExecutor(executor, this);
    }
    
    /**
     * Executor wrapper that propagates correlation context
     */
    private static class CorrelationContextPropagatingExecutor implements Executor {
        private final Executor delegate;
        private final CorrelationIdManager correlationIdManager;
        
        public CorrelationContextPropagatingExecutor(Executor delegate, CorrelationIdManager correlationIdManager) {
            this.delegate = delegate;
            this.correlationIdManager = correlationIdManager;
        }
        
        @Override
        public void execute(Runnable command) {
            String correlationId = correlationIdManager.getCurrentCorrelationId();
            String requestId = MDC.get(REQUEST_ID_KEY);
            
            delegate.execute(() -> 
                correlationIdManager.withCorrelationContext(correlationId, requestId, command));
        }
    }
}