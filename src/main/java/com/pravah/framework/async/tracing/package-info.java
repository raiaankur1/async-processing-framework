/**
 * Distributed tracing support for the async processing framework.
 * 
 * <p>This package provides comprehensive distributed tracing capabilities including:
 * <ul>
 *   <li>Integration with Spring Cloud Sleuth and Micrometer Tracing</li>
 *   <li>AWS X-Ray support for AWS-native tracing</li>
 *   <li>Correlation ID propagation across async operations</li>
 *   <li>Custom spans for AWS service operations (DynamoDB, SQS, S3)</li>
 *   <li>Async request lifecycle tracing</li>
 *   <li>Automatic context propagation in CompletableFuture chains</li>
 * </ul>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.pravah.framework.async.tracing.TracingAutoConfiguration} - Auto-configuration for tracing</li>
 *   <li>{@link com.pravah.framework.async.tracing.CorrelationIdManager} - Manages correlation IDs across async boundaries</li>
 *   <li>{@link com.pravah.framework.async.tracing.AwsServiceTracer} - Traces AWS service operations</li>
 *   <li>{@link com.pravah.framework.async.tracing.AsyncOperationTracer} - Traces async request lifecycle</li>
 * </ul>
 * 
 * <h2>Configuration</h2>
 * <p>Tracing can be configured via application properties:
 * <pre>
 * async.framework.tracing.enabled=true
 * async.framework.tracing.service-name=my-service
 * async.framework.tracing.xray.enabled=true
 * async.framework.tracing.zipkin.enabled=false
 * async.framework.tracing.sampling.probability=0.1
 * </pre>
 * 
 * @author Async Framework
 * @since 1.0.0
 */
package com.pravah.framework.async.tracing;