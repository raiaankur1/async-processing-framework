package com.pravah.framework.async.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Tracer for AWS service operations, adding custom spans and tags.
 * 
 * @author Async Framework
 * @since 1.0.0
 */
public class AwsServiceTracer {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsServiceTracer.class);
    
    private final Tracer tracer;
    private final TracingConfig tracingConfig;
    
    // AWS service operation names
    public static final String DYNAMODB_PUT_ITEM = "dynamodb.put_item";
    public static final String DYNAMODB_GET_ITEM = "dynamodb.get_item";
    public static final String DYNAMODB_UPDATE_ITEM = "dynamodb.update_item";
    public static final String DYNAMODB_QUERY = "dynamodb.query";
    public static final String DYNAMODB_SCAN = "dynamodb.scan";
    public static final String DYNAMODB_BATCH_GET_ITEM = "dynamodb.batch_get_item";
    public static final String DYNAMODB_BATCH_WRITE_ITEM = "dynamodb.batch_write_item";
    
    public static final String SQS_SEND_MESSAGE = "sqs.send_message";
    public static final String SQS_RECEIVE_MESSAGE = "sqs.receive_message";
    public static final String SQS_DELETE_MESSAGE = "sqs.delete_message";
    public static final String SQS_SEND_MESSAGE_BATCH = "sqs.send_message_batch";
    public static final String SQS_DELETE_MESSAGE_BATCH = "sqs.delete_message_batch";
    
    public static final String S3_PUT_OBJECT = "s3.put_object";
    public static final String S3_GET_OBJECT = "s3.get_object";
    public static final String S3_DELETE_OBJECT = "s3.delete_object";
    public static final String S3_HEAD_OBJECT = "s3.head_object";
    public static final String S3_LIST_OBJECTS = "s3.list_objects";
    
    // Common tag keys
    public static final String TAG_AWS_SERVICE = "aws.service";
    public static final String TAG_AWS_OPERATION = "aws.operation";
    public static final String TAG_AWS_REGION = "aws.region";
    public static final String TAG_AWS_REQUEST_ID = "aws.request_id";
    public static final String TAG_TABLE_NAME = "dynamodb.table_name";
    public static final String TAG_QUEUE_URL = "sqs.queue_url";
    public static final String TAG_QUEUE_NAME = "sqs.queue_name";
    public static final String TAG_BUCKET_NAME = "s3.bucket_name";
    public static final String TAG_OBJECT_KEY = "s3.object_key";
    public static final String TAG_ERROR_CODE = "error.code";
    public static final String TAG_ERROR_MESSAGE = "error.message";
    
    public AwsServiceTracer(Tracer tracer, TracingConfig tracingConfig) {
        this.tracer = tracer;
        this.tracingConfig = tracingConfig;
    }
    
    /**
     * Traces a DynamoDB operation
     */
    public <T> T traceDynamoDbOperation(String operation, String tableName, Supplier<T> supplier) {
        return traceAwsOperation("DynamoDB", operation, supplier, span -> {
            span.tag(TAG_AWS_SERVICE, "DynamoDB");
            span.tag(TAG_TABLE_NAME, tableName);
        });
    }
    
    /**
     * Traces an SQS operation
     */
    public <T> T traceSqsOperation(String operation, String queueUrl, Supplier<T> supplier) {
        return traceAwsOperation("SQS", operation, supplier, span -> {
            span.tag(TAG_AWS_SERVICE, "SQS");
            span.tag(TAG_QUEUE_URL, queueUrl);
            
            // Extract queue name from URL
            String queueName = extractQueueNameFromUrl(queueUrl);
            if (queueName != null) {
                span.tag(TAG_QUEUE_NAME, queueName);
            }
        });
    }
    
    /**
     * Traces an S3 operation
     */
    public <T> T traceS3Operation(String operation, String bucketName, String objectKey, Supplier<T> supplier) {
        return traceAwsOperation("S3", operation, supplier, span -> {
            span.tag(TAG_AWS_SERVICE, "S3");
            span.tag(TAG_BUCKET_NAME, bucketName);
            if (objectKey != null) {
                span.tag(TAG_OBJECT_KEY, objectKey);
            }
        });
    }
    
    /**
     * Generic AWS operation tracing
     */
    public <T> T traceAwsOperation(String serviceName, String operation, Supplier<T> supplier, 
                                  SpanCustomizer customizer) {
        if (!tracingConfig.isEnabled()) {
            return supplier.get();
        }
        
        String spanName = serviceName.toLowerCase() + "." + operation.toLowerCase().replace("_", "-");
        Span span = tracer.nextSpan().name(spanName);
        
        span.start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Add common AWS tags
            span.tag(TAG_AWS_SERVICE, serviceName);
            span.tag(TAG_AWS_OPERATION, operation);
            
            // Add custom tags
            if (customizer != null) {
                customizer.customize(span);
            }
            
            Instant startTime = Instant.now();
            
            try {
                T result = supplier.get();
                
                // Add success metrics
                Duration duration = Duration.between(startTime, Instant.now());
                span.tag("duration_ms", String.valueOf(duration.toMillis()));
                
                logger.debug("AWS {} operation {} completed successfully in {}ms", 
                    serviceName, operation, duration.toMillis());
                
                return result;
                
            } catch (Exception e) {
                // Add error information
                span.tag(TAG_ERROR_CODE, e.getClass().getSimpleName());
                span.tag(TAG_ERROR_MESSAGE, e.getMessage());
                span.error(e);
                
                Duration duration = Duration.between(startTime, Instant.now());
                logger.warn("AWS {} operation {} failed after {}ms: {}", 
                    serviceName, operation, duration.toMillis(), e.getMessage());
                
                throw e;
            }
        } finally {
            span.end();
        }
    }
    
    /**
     * Traces an async AWS operation
     */
    public <T> T traceAsyncAwsOperation(String serviceName, String operation, String requestId, 
                                       Supplier<T> supplier, SpanCustomizer customizer) {
        return traceAwsOperation(serviceName, operation, supplier, span -> {
            span.tag("async.request_id", requestId);
            span.tag("operation.type", "async");
            
            if (customizer != null) {
                customizer.customize(span);
            }
        });
    }
    
    /**
     * Adds AWS request ID to current span if available
     */
    public void addAwsRequestId(String requestId) {
        if (requestId != null && tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(TAG_AWS_REQUEST_ID, requestId);
            }
        }
    }
    
    /**
     * Adds error information to current span
     */
    public void addError(Throwable error) {
        if (error != null && tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                currentSpan.tag(TAG_ERROR_CODE, error.getClass().getSimpleName());
                currentSpan.tag(TAG_ERROR_MESSAGE, error.getMessage());
                currentSpan.error(error);
            }
        }
    }
    
    /**
     * Adds custom tags to current span
     */
    public void addTags(Map<String, String> tags) {
        if (tags != null && !tags.isEmpty() && tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                tags.forEach(currentSpan::tag);
            }
        }
    }
    
    private String extractQueueNameFromUrl(String queueUrl) {
        if (queueUrl == null) {
            return null;
        }
        
        try {
            // Extract queue name from SQS URL format: https://sqs.region.amazonaws.com/account/queueName
            String[] parts = queueUrl.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            logger.debug("Failed to extract queue name from URL: {}", queueUrl);
            return null;
        }
    }
    
    /**
     * Functional interface for customizing spans
     */
    @FunctionalInterface
    public interface SpanCustomizer {
        void customize(Span span);
    }
}