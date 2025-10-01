package com.pravah.framework.async.monitoring;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for CloudWatch metrics integration.
 * 
 * <p>This configuration provides CloudWatch-specific metrics registry
 * and configuration for AWS-native monitoring capabilities.
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@Configuration
@ConditionalOnClass({CloudWatchMeterRegistry.class, CloudWatchAsyncClient.class})
@ConditionalOnProperty(
    prefix = "async.framework.monitoring.cloudwatch", 
    name = "enabled", 
    havingValue = "true"
)
public class CloudWatchMetricsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchMetricsConfiguration.class);

    /**
     * CloudWatch-specific configuration properties.
     */
    @ConfigurationProperties(prefix = "async.framework.monitoring.cloudwatch")
    public static class CloudWatchMetricsProperties {
        
        /**
         * Whether CloudWatch metrics are enabled.
         */
        private boolean enabled = false;
        
        /**
         * CloudWatch namespace for metrics.
         */
        private String namespace = "AsyncFramework";
        
        /**
         * Step interval for publishing metrics.
         */
        private Duration step = Duration.ofMinutes(1);
        
        /**
         * Batch size for publishing metrics.
         */
        private int batchSize = 20;
        
        /**
         * Common dimensions to add to all metrics.
         */
        private Map<String, String> commonDimensions = Map.of(
            "Service", "AsyncProcessingFramework",
            "Environment", "production"
        );
        
        /**
         * Whether to publish percentile histograms.
         */
        private boolean percentileHistogram = true;
        
        /**
         * Connection timeout for CloudWatch client.
         */
        private Duration connectTimeout = Duration.ofSeconds(10);
        
        /**
         * Read timeout for CloudWatch client.
         */
        private Duration readTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public Duration getStep() {
            return step;
        }

        public void setStep(Duration step) {
            this.step = step;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Map<String, String> getCommonDimensions() {
            return commonDimensions;
        }

        public void setCommonDimensions(Map<String, String> commonDimensions) {
            this.commonDimensions = commonDimensions;
        }

        public boolean isPercentileHistogram() {
            return percentileHistogram;
        }

        public void setPercentileHistogram(boolean percentileHistogram) {
            this.percentileHistogram = percentileHistogram;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    @Bean
    public CloudWatchMetricsProperties cloudWatchMetricsProperties() {
        return new CloudWatchMetricsProperties();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient(CloudWatchMetricsProperties properties) {
        logger.info("Creating CloudWatch async client with connect timeout: {}, read timeout: {}", 
            properties.getConnectTimeout(), properties.getReadTimeout());
            
        return CloudWatchAsyncClient.builder()
            .overrideConfiguration(builder -> builder
                .apiCallTimeout(properties.getReadTimeout())
                .apiCallAttemptTimeout(properties.getConnectTimeout())
            )
            .build();
    }

    @Bean
    @Primary
    public MeterRegistry cloudWatchMeterRegistry(
            CloudWatchAsyncClient cloudWatchAsyncClient,
            CloudWatchMetricsProperties properties) {
        
        logger.info("Creating CloudWatch meter registry with namespace: {}, step: {}", 
            properties.getNamespace(), properties.getStep());

        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use default values
            }

            @Override
            public String namespace() {
                return properties.getNamespace();
            }

            @Override
            public Duration step() {
                return properties.getStep();
            }

            @Override
            public int batchSize() {
                return properties.getBatchSize();
            }

            @Override
            public boolean enabled() {
                return properties.isEnabled();
            }
        };

        CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(
            cloudWatchConfig, 
            io.micrometer.core.instrument.Clock.SYSTEM,
            cloudWatchAsyncClient
        );

        // Add common dimensions to all metrics
        properties.getCommonDimensions().forEach((key, value) -> 
            registry.config().commonTags(key, value)
        );

        logger.info("CloudWatch meter registry created successfully");
        return registry;
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "async.framework.monitoring.cloudwatch", 
        name = "custom-dashboards.enabled", 
        havingValue = "true"
    )
    public CloudWatchDashboardManager cloudWatchDashboardManager(
            CloudWatchAsyncClient cloudWatchAsyncClient,
            CloudWatchMetricsProperties properties) {
        
        return new CloudWatchDashboardManager(cloudWatchAsyncClient, properties);
    }
}