package com.pravah.framework.async.tracing;

import brave.Tracing;
import brave.sampler.RateLimitingSampler;
import brave.sampler.Sampler;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;
import zipkin2.reporter.AsyncReporter;

/**
 * Auto-configuration for distributed tracing in the async framework.
 * 
 * @author Async Framework
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "async.framework.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TracingConfig.class)
public class TracingAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(TracingAutoConfiguration.class);
    
    /**
     * Creates a Brave tracer for Micrometer tracing integration
     */
    @Bean
    @Primary
    public BraveTracer braveTracer(TracingConfig tracingConfig) {
        logger.info("Configuring Brave tracer for service: {}", tracingConfig.getServiceName());
        
        Tracing.Builder tracingBuilder = Tracing.newBuilder()
                .localServiceName(tracingConfig.getServiceName())
                .sampler(createSampler(tracingConfig.getSampling()));
        
        // Add Zipkin span handler if enabled
        if (tracingConfig.getZipkin().isEnabled()) {
            logger.info("Enabling Zipkin tracing to: {}", tracingConfig.getZipkin().getBaseUrl());
            try {
                OkHttpSender sender = OkHttpSender.create(tracingConfig.getZipkin().getBaseUrl() + "/api/v2/spans");
                AsyncReporter<zipkin2.Span> reporter = AsyncReporter.create(sender);
                tracingBuilder.addSpanHandler(ZipkinSpanHandler.create(reporter));
                logger.info("Successfully configured Zipkin span handler");
            } catch (Exception e) {
                logger.warn("Failed to configure Zipkin sender, continuing without Zipkin: {}", e.getMessage());
            }
        }
        
        Tracing tracing = tracingBuilder.build();
        
        return new BraveTracer(
                tracing.tracer(),
                new BraveCurrentTraceContext(tracing.currentTraceContext()),
                new BraveBaggageManager()
        );
    }
    
    /**
     * Creates AWS X-Ray recorder if X-Ray is enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "async.framework.tracing.xray", name = "enabled", havingValue = "true")
    @ConditionalOnClass(AWSXRayRecorder.class)
    public AWSXRayRecorder awsXRayRecorder(TracingConfig tracingConfig) {
        logger.info("Configuring AWS X-Ray recorder");
        
        TracingConfig.XRayConfig xrayConfig = tracingConfig.getXray();
        
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
        
        // Add AWS plugins for service detection
        if (xrayConfig.isUseAwsPlugin()) {
            try {
                builder.withPlugin(new EC2Plugin())
                       .withPlugin(new ECSPlugin())
                       .withPlugin(new EKSPlugin());
            } catch (Exception e) {
                logger.warn("Failed to configure AWS X-Ray plugins: {}", e.getMessage());
            }
        }
        
        // Configure sampling strategy
        builder.withSamplingStrategy(new LocalizedSamplingStrategy());
        
        AWSXRayRecorder recorder = builder.build();
        
        // Set as global recorder
        AWSXRay.setGlobalRecorder(recorder);
        
        return recorder;
    }
    
    /**
     * Creates Zipkin sender if Zipkin is enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "async.framework.tracing.zipkin", name = "enabled", havingValue = "true")
    @ConditionalOnClass(OkHttpSender.class)
    public OkHttpSender zipkinSender(TracingConfig tracingConfig) {
        logger.info("Creating Zipkin OkHttp sender for: {}", tracingConfig.getZipkin().getBaseUrl());
        return OkHttpSender.create(tracingConfig.getZipkin().getBaseUrl() + "/api/v2/spans");
    }

    /**
     * Creates correlation ID manager for async operations
     */
    @Bean
    public CorrelationIdManager correlationIdManager() {
        return new CorrelationIdManager();
    }
    
    /**
     * Creates AWS service tracer for custom spans
     */
    @Bean
    public AwsServiceTracer awsServiceTracer(BraveTracer tracer, TracingConfig tracingConfig) {
        return new AwsServiceTracer(tracer, tracingConfig);
    }
    
    /**
     * Creates async operation tracer
     */
    @Bean
    public AsyncOperationTracer asyncOperationTracer(BraveTracer tracer, CorrelationIdManager correlationIdManager) {
        return new AsyncOperationTracer(tracer, correlationIdManager);
    }
    
    private Sampler createSampler(TracingConfig.SamplingConfig samplingConfig) {
        if (samplingConfig.getRateLimit() > 0) {
            return RateLimitingSampler.create(samplingConfig.getRateLimit());
        } else {
            return Sampler.create(samplingConfig.getProbability());
        }
    }
}