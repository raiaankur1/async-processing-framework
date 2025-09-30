package com.pravah.framework.async.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for distributed tracing in the async framework.
 * 
 * @author Async Framework
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "async.framework.tracing")
@Validated
public class TracingConfig {
    
    /**
     * Whether tracing is enabled
     */
    private boolean enabled = true;
    
    /**
     * Service name for tracing
     */
    @NotBlank
    private String serviceName = "async-processing-framework";
    
    /**
     * AWS X-Ray configuration
     */
    @NotNull
    private XRayConfig xray = new XRayConfig();
    
    /**
     * Zipkin configuration
     */
    @NotNull
    private ZipkinConfig zipkin = new ZipkinConfig();
    
    /**
     * Sampling configuration
     */
    @NotNull
    private SamplingConfig sampling = new SamplingConfig();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public XRayConfig getXray() {
        return xray;
    }
    
    public void setXray(XRayConfig xray) {
        this.xray = xray;
    }
    
    public ZipkinConfig getZipkin() {
        return zipkin;
    }
    
    public void setZipkin(ZipkinConfig zipkin) {
        this.zipkin = zipkin;
    }
    
    public SamplingConfig getSampling() {
        return sampling;
    }
    
    public void setSampling(SamplingConfig sampling) {
        this.sampling = sampling;
    }
    
    /**
     * AWS X-Ray specific configuration
     */
    public static class XRayConfig {
        /**
         * Whether AWS X-Ray tracing is enabled
         */
        private boolean enabled = false;
        
        /**
         * AWS region for X-Ray
         */
        private String region;
        
        /**
         * X-Ray daemon address
         */
        private String daemonAddress = "127.0.0.1:2000";
        
        /**
         * Whether to use plugin for AWS service tracing
         */
        private boolean useAwsPlugin = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getRegion() {
            return region;
        }
        
        public void setRegion(String region) {
            this.region = region;
        }
        
        public String getDaemonAddress() {
            return daemonAddress;
        }
        
        public void setDaemonAddress(String daemonAddress) {
            this.daemonAddress = daemonAddress;
        }
        
        public boolean isUseAwsPlugin() {
            return useAwsPlugin;
        }
        
        public void setUseAwsPlugin(boolean useAwsPlugin) {
            this.useAwsPlugin = useAwsPlugin;
        }
    }
    
    /**
     * Zipkin specific configuration
     */
    public static class ZipkinConfig {
        /**
         * Whether Zipkin tracing is enabled
         */
        private boolean enabled = false;
        
        /**
         * Zipkin base URL
         */
        private String baseUrl = "http://localhost:9411";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
    
    /**
     * Sampling configuration
     */
    public static class SamplingConfig {
        /**
         * Sampling probability (0.0 to 1.0)
         */
        private float probability = 0.1f;
        
        /**
         * Rate limiting for traces per second
         */
        private int rateLimit = 100;
        
        public float getProbability() {
            return probability;
        }
        
        public void setProbability(float probability) {
            this.probability = probability;
        }
        
        public int getRateLimit() {
            return rateLimit;
        }
        
        public void setRateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
        }
    }
}