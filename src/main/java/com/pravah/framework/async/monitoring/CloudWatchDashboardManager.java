package com.pravah.framework.async.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages CloudWatch dashboards for the async processing framework.
 * 
 * <p>This component automatically creates and updates CloudWatch dashboards
 * with relevant metrics for monitoring the async processing framework.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
public class CloudWatchDashboardManager {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchDashboardManager.class);

    private final CloudWatchAsyncClient cloudWatchClient;
    private final CloudWatchMetricsConfiguration.CloudWatchMetricsProperties properties;
    private final ObjectMapper objectMapper;

    public CloudWatchDashboardManager(
            CloudWatchAsyncClient cloudWatchClient,
            CloudWatchMetricsConfiguration.CloudWatchMetricsProperties properties) {
        this.cloudWatchClient = cloudWatchClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates the main async framework dashboard after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void createAsyncFrameworkDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                String dashboardBody = createDashboardBody();
                
                PutDashboardRequest request = PutDashboardRequest.builder()
                    .dashboardName("AsyncProcessingFramework-Overview")
                    .dashboardBody(dashboardBody)
                    .build();

                cloudWatchClient.putDashboard(request)
                    .thenAccept(response -> 
                        logger.info("Successfully created/updated AsyncProcessingFramework dashboard"))
                    .exceptionally(throwable -> {
                        logger.error("Failed to create AsyncProcessingFramework dashboard", throwable);
                        return null;
                    });
                    
            } catch (Exception e) {
                logger.error("Error creating dashboard", e);
            }
        });
    }

    /**
     * Creates custom alarms for critical metrics.
     */
    public void createAlarms() {
        CompletableFuture.runAsync(() -> {
            try {
                createRequestFailureAlarm();
                createQueueDepthAlarm();
                createRetryExhaustedAlarm();
                createHealthCheckAlarm();
                
                logger.info("Successfully created CloudWatch alarms");
            } catch (Exception e) {
                logger.error("Error creating CloudWatch alarms", e);
            }
        });
    }

    private void createRequestFailureAlarm() {
        PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
            .alarmName("AsyncFramework-HighFailureRate")
            .alarmDescription("High failure rate in async request processing")
            .metricName("async.framework.request.failed")
            .namespace(properties.getNamespace())
            .statistic(Statistic.SUM)
            .period(300) // 5 minutes
            .evaluationPeriods(2)
            .threshold(10.0) // More than 10 failures in 5 minutes
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .treatMissingData("notBreaching")
            .build();

        cloudWatchClient.putMetricAlarm(request)
            .thenAccept(response -> 
                logger.debug("Created failure rate alarm"))
            .exceptionally(throwable -> {
                logger.warn("Failed to create failure rate alarm", throwable);
                return null;
            });
    }

    private void createQueueDepthAlarm() {
        PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
            .alarmName("AsyncFramework-HighQueueDepth")
            .alarmDescription("High queue depth indicating processing backlog")
            .metricName("async.framework.queue.depth")
            .namespace(properties.getNamespace())
            .statistic(Statistic.MAXIMUM)
            .period(300) // 5 minutes
            .evaluationPeriods(3)
            .threshold(100.0) // More than 100 messages in queue
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .treatMissingData("notBreaching")
            .build();

        cloudWatchClient.putMetricAlarm(request)
            .thenAccept(response -> 
                logger.debug("Created queue depth alarm"))
            .exceptionally(throwable -> {
                logger.warn("Failed to create queue depth alarm", throwable);
                return null;
            });
    }

    private void createRetryExhaustedAlarm() {
        PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
            .alarmName("AsyncFramework-RetryExhausted")
            .alarmDescription("Requests exhausting retry attempts")
            .metricName("async.framework.retry.exhausted")
            .namespace(properties.getNamespace())
            .statistic(Statistic.SUM)
            .period(300) // 5 minutes
            .evaluationPeriods(1)
            .threshold(1.0) // Any retry exhaustion
            .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
            .treatMissingData("notBreaching")
            .build();

        cloudWatchClient.putMetricAlarm(request)
            .thenAccept(response -> 
                logger.debug("Created retry exhausted alarm"))
            .exceptionally(throwable -> {
                logger.warn("Failed to create retry exhausted alarm", throwable);
                return null;
            });
    }

    private void createHealthCheckAlarm() {
        PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
            .alarmName("AsyncFramework-HealthCheckFailure")
            .alarmDescription("Health check failures for framework components")
            .metricName("async.framework.health.check")
            .namespace(properties.getNamespace())
            .statistic(Statistic.AVERAGE)
            .period(60) // 1 minute
            .evaluationPeriods(2)
            .threshold(0.8) // Less than 80% healthy
            .comparisonOperator(ComparisonOperator.LESS_THAN_THRESHOLD)
            .treatMissingData("breaching")
            .build();

        cloudWatchClient.putMetricAlarm(request)
            .thenAccept(response -> 
                logger.debug("Created health check alarm"))
            .exceptionally(throwable -> {
                logger.warn("Failed to create health check alarm", throwable);
                return null;
            });
    }

    private String createDashboardBody() {
        try {
            Map<String, Object> dashboard = Map.of(
                "widgets", List.of(
                    createRequestMetricsWidget(),
                    createQueueMetricsWidget(),
                    createErrorMetricsWidget(),
                    createPerformanceMetricsWidget(),
                    createHealthMetricsWidget(),
                    createResourceMetricsWidget()
                )
            );
            
            return objectMapper.writeValueAsString(dashboard);
        } catch (Exception e) {
            logger.error("Error creating dashboard body", e);
            return "{}";
        }
    }

    private Map<String, Object> createRequestMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 0, "y", 0, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.request.started"),
                    List.of(".", "async.framework.request.completed"),
                    List.of(".", "async.framework.request.failed")
                ),
                "period", 300,
                "stat", "Sum",
                "region", "us-east-1",
                "title", "Request Processing Overview",
                "yAxis", Map.of("left", Map.of("min", 0))
            )
        );
    }

    private Map<String, Object> createQueueMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 12, "y", 0, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.queue.depth"),
                    List.of(".", "async.framework.message.received"),
                    List.of(".", "async.framework.message.processed")
                ),
                "period", 300,
                "stat", "Average",
                "region", "us-east-1",
                "title", "Queue Metrics",
                "yAxis", Map.of("left", Map.of("min", 0))
            )
        );
    }

    private Map<String, Object> createErrorMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 0, "y", 6, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.request.failed"),
                    List.of(".", "async.framework.retry.attempt"),
                    List.of(".", "async.framework.retry.exhausted"),
                    List.of(".", "async.framework.dead.letter.message")
                ),
                "period", 300,
                "stat", "Sum",
                "region", "us-east-1",
                "title", "Error and Retry Metrics",
                "yAxis", Map.of("left", Map.of("min", 0))
            )
        );
    }

    private Map<String, Object> createPerformanceMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 12, "y", 6, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.request.processing.time"),
                    List.of(".", "async.framework.message.processing.time"),
                    List.of(".", "async.framework.api.call.time")
                ),
                "period", 300,
                "stat", "Average",
                "region", "us-east-1",
                "title", "Performance Metrics (ms)",
                "yAxis", Map.of("left", Map.of("min", 0))
            )
        );
    }

    private Map<String, Object> createHealthMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 0, "y", 12, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.health.check"),
                    List.of(".", "async.framework.circuit.breaker.state")
                ),
                "period", 300,
                "stat", "Average",
                "region", "us-east-1",
                "title", "Health and Circuit Breaker Metrics",
                "yAxis", Map.of("left", Map.of("min", 0, "max", 1))
            )
        );
    }

    private Map<String, Object> createResourceMetricsWidget() {
        return Map.of(
            "type", "metric",
            "x", 12, "y", 12, "width", 12, "height", 6,
            "properties", Map.of(
                "metrics", List.of(
                    List.of(properties.getNamespace(), "async.framework.memory.used"),
                    List.of(".", "async.framework.thread.pool.active"),
                    List.of(".", "async.framework.thread.pool.queue.size")
                ),
                "period", 300,
                "stat", "Average",
                "region", "us-east-1",
                "title", "Resource Utilization",
                "yAxis", Map.of("left", Map.of("min", 0))
            )
        );
    }
}