package com.pravah.framework.async.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.Map;

/**
 * Auto-configuration for async framework metrics collection.
 * 
 * <p>This configuration automatically sets up metrics collection based on
 * available dependencies and configuration properties.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty(
    prefix = "async.framework.monitoring", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
@Import({CloudWatchMetricsConfiguration.class})
public class AsyncFrameworkMetricsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AsyncFrameworkMetricsAutoConfiguration.class);

    /**
     * General monitoring configuration properties.
     */
    @ConfigurationProperties(prefix = "async.framework.monitoring")
    public static class MonitoringProperties {
        
        /**
         * Whether monitoring is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Metrics collection interval.
         */
        private Duration collectionInterval = Duration.ofSeconds(30);
        
        /**
         * Whether to collect JVM metrics.
         */
        private boolean jvmMetrics = true;
        
        /**
         * Whether to collect system metrics.
         */
        private boolean systemMetrics = true;
        
        /**
         * Whether to collect custom business metrics.
         */
        private boolean businessMetrics = true;
        
        /**
         * Metric name prefix.
         */
        private String metricPrefix = "async.framework";
        
        /**
         * Common tags to add to all metrics.
         */
        private Map<String, String> commonTags = Map.of(
            "application", "async-processing-framework",
            "version", "1.0.0"
        );
        
        /**
         * Whether to enable detailed error tracking.
         */
        private boolean detailedErrorTracking = true;
        
        /**
         * Whether to enable performance profiling.
         */
        private boolean performanceProfiling = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getCollectionInterval() {
            return collectionInterval;
        }

        public void setCollectionInterval(Duration collectionInterval) {
            this.collectionInterval = collectionInterval;
        }

        public boolean isJvmMetrics() {
            return jvmMetrics;
        }

        public void setJvmMetrics(boolean jvmMetrics) {
            this.jvmMetrics = jvmMetrics;
        }

        public boolean isSystemMetrics() {
            return systemMetrics;
        }

        public void setSystemMetrics(boolean systemMetrics) {
            this.systemMetrics = systemMetrics;
        }

        public boolean isBusinessMetrics() {
            return businessMetrics;
        }

        public void setBusinessMetrics(boolean businessMetrics) {
            this.businessMetrics = businessMetrics;
        }

        public String getMetricPrefix() {
            return metricPrefix;
        }

        public void setMetricPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
        }

        public Map<String, String> getCommonTags() {
            return commonTags;
        }

        public void setCommonTags(Map<String, String> commonTags) {
            this.commonTags = commonTags;
        }

        public boolean isDetailedErrorTracking() {
            return detailedErrorTracking;
        }

        public void setDetailedErrorTracking(boolean detailedErrorTracking) {
            this.detailedErrorTracking = detailedErrorTracking;
        }

        public boolean isPerformanceProfiling() {
            return performanceProfiling;
        }

        public void setPerformanceProfiling(boolean performanceProfiling) {
            this.performanceProfiling = performanceProfiling;
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "async.framework.monitoring")
    public MonitoringProperties monitoringProperties() {
        return new MonitoringProperties();
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry simpleMeterRegistry() {
        logger.info("No MeterRegistry found, creating SimpleMeterRegistry as fallback");
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(MetricsCollector.class)
    public MetricsCollector micrometerMetricsCollector(MeterRegistry meterRegistry) {
        logger.info("Creating MicrometerMetricsCollector with registry: {}", 
            meterRegistry.getClass().getSimpleName());
        return new MicrometerMetricsCollector(meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "system-metrics-collector.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public SystemMetricsCollector systemMetricsCollector(
            MetricsCollector metricsCollector,
            MonitoringProperties properties) {
        
        return new SystemMetricsCollector(metricsCollector, properties);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "performance-monitor.enabled", 
        havingValue = "true"
    )
    public PerformanceMonitor performanceMonitor(
            MetricsCollector metricsCollector,
            MonitoringProperties properties) {
        
        return new PerformanceMonitor(metricsCollector, properties);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring", 
        name = "error-tracker.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public ErrorTracker errorTracker(
            MetricsCollector metricsCollector,
            MonitoringProperties properties) {
        
        return new ErrorTracker(metricsCollector, properties);
    }
}