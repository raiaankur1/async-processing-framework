package com.pravah.framework.async.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration for SQS message listener integration.
 * 
 * <p>This configuration class provides:
 * <ul>
 *   <li>SQS listener polling configuration</li>
 *   <li>Message acknowledgment settings</li>
 *   <li>Concurrency and thread pool configuration</li>
 *   <li>Error handling and retry settings</li>
 *   <li>Dead letter queue configuration</li>
 * </ul>
 * 
 * @author Ankur Rai
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(
    prefix = "async.framework.listener", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class SQSListenerConfig {

    /**
     * SQS listener configuration properties.
     */
    @ConfigurationProperties(prefix = "async.framework.listener")
    @Validated
    public static class SQSListenerProperties {

        /**
         * Whether the SQS listener is enabled.
         */
        private boolean enabled = true;

        /**
         * Message acknowledgment mode.
         */
        @NotNull
        private String acknowledgmentMode = "MANUAL";

        /**
         * Acknowledgment interval for batch acknowledgments.
         */
        private Duration acknowledgmentInterval = Duration.ofSeconds(10);

        /**
         * Acknowledgment threshold for batch acknowledgments.
         */
        @Min(1)
        @Max(10)
        private int acknowledgmentThreshold = 5;

        /**
         * Maximum number of concurrent consumers per queue.
         */
        @Min(1)
        @Max(100)
        private int maxConcurrentConsumers = 10;

        /**
         * Minimum number of concurrent consumers per queue.
         */
        @Min(1)
        private int minConcurrentConsumers = 1;

        /**
         * Maximum number of messages to receive in a single poll.
         */
        @Min(1)
        @Max(10)
        private int maxMessagesPerPoll = 10;

        /**
         * Wait time for long polling in seconds.
         */
        @Min(0)
        @Max(20)
        private int waitTimeSeconds = 20;

        /**
         * Visibility timeout for received messages in seconds.
         */
        @Min(0)
        @Max(43200) // 12 hours
        private int visibilityTimeoutSeconds = 300; // 5 minutes

        /**
         * Whether to auto-start the listener containers.
         */
        private boolean autoStartup = true;

        /**
         * Startup phase for ordered startup.
         */
        private int phase = Integer.MAX_VALUE;

        /**
         * Error handling configuration.
         */
        @Valid
        @NotNull
        private ErrorHandlingConfig errorHandling = new ErrorHandlingConfig();

        /**
         * Dead letter queue configuration.
         */
        @Valid
        @NotNull
        private DeadLetterQueueConfig deadLetterQueue = new DeadLetterQueueConfig();

        // Getters and setters

        public boolean isEnabled() {
            return enabled;
        }

        public SQSListenerProperties setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getAcknowledgmentMode() {
            return acknowledgmentMode;
        }

        public SQSListenerProperties setAcknowledgmentMode(String acknowledgmentMode) {
            this.acknowledgmentMode = acknowledgmentMode;
            return this;
        }

        public Duration getAcknowledgmentInterval() {
            return acknowledgmentInterval;
        }

        public SQSListenerProperties setAcknowledgmentInterval(Duration acknowledgmentInterval) {
            this.acknowledgmentInterval = acknowledgmentInterval;
            return this;
        }

        public int getAcknowledgmentThreshold() {
            return acknowledgmentThreshold;
        }

        public SQSListenerProperties setAcknowledgmentThreshold(int acknowledgmentThreshold) {
            this.acknowledgmentThreshold = acknowledgmentThreshold;
            return this;
        }

        public int getMaxConcurrentConsumers() {
            return maxConcurrentConsumers;
        }

        public SQSListenerProperties setMaxConcurrentConsumers(int maxConcurrentConsumers) {
            this.maxConcurrentConsumers = maxConcurrentConsumers;
            return this;
        }

        public int getMinConcurrentConsumers() {
            return minConcurrentConsumers;
        }

        public SQSListenerProperties setMinConcurrentConsumers(int minConcurrentConsumers) {
            this.minConcurrentConsumers = minConcurrentConsumers;
            return this;
        }

        public int getMaxMessagesPerPoll() {
            return maxMessagesPerPoll;
        }

        public SQSListenerProperties setMaxMessagesPerPoll(int maxMessagesPerPoll) {
            this.maxMessagesPerPoll = maxMessagesPerPoll;
            return this;
        }

        public int getWaitTimeSeconds() {
            return waitTimeSeconds;
        }

        public SQSListenerProperties setWaitTimeSeconds(int waitTimeSeconds) {
            this.waitTimeSeconds = waitTimeSeconds;
            return this;
        }

        public int getVisibilityTimeoutSeconds() {
            return visibilityTimeoutSeconds;
        }

        public SQSListenerProperties setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
            return this;
        }

        public boolean isAutoStartup() {
            return autoStartup;
        }

        public SQSListenerProperties setAutoStartup(boolean autoStartup) {
            this.autoStartup = autoStartup;
            return this;
        }

        public int getPhase() {
            return phase;
        }

        public SQSListenerProperties setPhase(int phase) {
            this.phase = phase;
            return this;
        }

        public ErrorHandlingConfig getErrorHandling() {
            return errorHandling;
        }

        public SQSListenerProperties setErrorHandling(ErrorHandlingConfig errorHandling) {
            this.errorHandling = errorHandling;
            return this;
        }

        public DeadLetterQueueConfig getDeadLetterQueue() {
            return deadLetterQueue;
        }

        public SQSListenerProperties setDeadLetterQueue(DeadLetterQueueConfig deadLetterQueue) {
            this.deadLetterQueue = deadLetterQueue;
            return this;
        }

        /**
         * Validates the configuration.
         */
        public void validate() {
            if (minConcurrentConsumers > maxConcurrentConsumers) {
                throw new IllegalArgumentException("Min concurrent consumers cannot exceed max concurrent consumers");
            }
            if (acknowledgmentThreshold > maxMessagesPerPoll) {
                throw new IllegalArgumentException("Acknowledgment threshold cannot exceed max messages per poll");
            }
            if (errorHandling != null) {
                errorHandling.validate();
            }
            if (deadLetterQueue != null) {
                deadLetterQueue.validate();
            }
        }

        /**
         * Error handling configuration nested class.
         */
        public static class ErrorHandlingConfig {
            
            /**
             * Maximum number of retry attempts before sending to DLQ.
             */
            @Min(0)
            @Max(100)
            private int maxRetryAttempts = 3;

            /**
             * Initial retry delay.
             */
            private Duration initialRetryDelay = Duration.ofSeconds(30);

            /**
             * Maximum retry delay.
             */
            private Duration maxRetryDelay = Duration.ofMinutes(10);

            /**
             * Retry delay multiplier for exponential backoff.
             */
            @DecimalMin("1.0")
            @DecimalMax("10.0")
            private double retryDelayMultiplier = 2.0;

            /**
             * Whether to enable exponential backoff for retries.
             */
            private boolean enableExponentialBackoff = true;

            /**
             * Whether to log error details.
             */
            private boolean logErrors = true;

            /**
             * Whether to include stack traces in error logs.
             */
            private boolean includeStackTrace = false;

            // Getters and setters

            public int getMaxRetryAttempts() {
                return maxRetryAttempts;
            }

            public ErrorHandlingConfig setMaxRetryAttempts(int maxRetryAttempts) {
                this.maxRetryAttempts = maxRetryAttempts;
                return this;
            }

            public Duration getInitialRetryDelay() {
                return initialRetryDelay;
            }

            public ErrorHandlingConfig setInitialRetryDelay(Duration initialRetryDelay) {
                this.initialRetryDelay = initialRetryDelay;
                return this;
            }

            public Duration getMaxRetryDelay() {
                return maxRetryDelay;
            }

            public ErrorHandlingConfig setMaxRetryDelay(Duration maxRetryDelay) {
                this.maxRetryDelay = maxRetryDelay;
                return this;
            }

            public double getRetryDelayMultiplier() {
                return retryDelayMultiplier;
            }

            public ErrorHandlingConfig setRetryDelayMultiplier(double retryDelayMultiplier) {
                this.retryDelayMultiplier = retryDelayMultiplier;
                return this;
            }

            public boolean isEnableExponentialBackoff() {
                return enableExponentialBackoff;
            }

            public ErrorHandlingConfig setEnableExponentialBackoff(boolean enableExponentialBackoff) {
                this.enableExponentialBackoff = enableExponentialBackoff;
                return this;
            }

            public boolean isLogErrors() {
                return logErrors;
            }

            public ErrorHandlingConfig setLogErrors(boolean logErrors) {
                this.logErrors = logErrors;
                return this;
            }

            public boolean isIncludeStackTrace() {
                return includeStackTrace;
            }

            public ErrorHandlingConfig setIncludeStackTrace(boolean includeStackTrace) {
                this.includeStackTrace = includeStackTrace;
                return this;
            }

            public void validate() {
                if (initialRetryDelay == null || initialRetryDelay.isNegative()) {
                    throw new IllegalArgumentException("Initial retry delay must be positive");
                }
                if (maxRetryDelay == null || maxRetryDelay.isNegative()) {
                    throw new IllegalArgumentException("Max retry delay must be positive");
                }
                if (initialRetryDelay.compareTo(maxRetryDelay) > 0) {
                    throw new IllegalArgumentException("Initial retry delay cannot exceed max retry delay");
                }
            }
        }

        /**
         * Dead letter queue configuration nested class.
         */
        public static class DeadLetterQueueConfig {
            
            /**
             * Whether dead letter queue processing is enabled.
             */
            private boolean enabled = true;

            /**
             * Maximum number of concurrent consumers for DLQ.
             */
            @Min(1)
            @Max(10)
            private int maxConcurrentConsumers = 2;

            /**
             * Whether to automatically reprocess DLQ messages.
             */
            private boolean autoReprocess = false;

            /**
             * Reprocessing interval for automatic DLQ reprocessing.
             */
            private Duration reprocessingInterval = Duration.ofHours(1);

            /**
             * Maximum age of messages to reprocess from DLQ.
             */
            private Duration maxMessageAge = Duration.ofDays(7);

            // Getters and setters

            public boolean isEnabled() {
                return enabled;
            }

            public DeadLetterQueueConfig setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public int getMaxConcurrentConsumers() {
                return maxConcurrentConsumers;
            }

            public DeadLetterQueueConfig setMaxConcurrentConsumers(int maxConcurrentConsumers) {
                this.maxConcurrentConsumers = maxConcurrentConsumers;
                return this;
            }

            public boolean isAutoReprocess() {
                return autoReprocess;
            }

            public DeadLetterQueueConfig setAutoReprocess(boolean autoReprocess) {
                this.autoReprocess = autoReprocess;
                return this;
            }

            public Duration getReprocessingInterval() {
                return reprocessingInterval;
            }

            public DeadLetterQueueConfig setReprocessingInterval(Duration reprocessingInterval) {
                this.reprocessingInterval = reprocessingInterval;
                return this;
            }

            public Duration getMaxMessageAge() {
                return maxMessageAge;
            }

            public DeadLetterQueueConfig setMaxMessageAge(Duration maxMessageAge) {
                this.maxMessageAge = maxMessageAge;
                return this;
            }

            public void validate() {
                if (reprocessingInterval == null || reprocessingInterval.isNegative()) {
                    throw new IllegalArgumentException("Reprocessing interval must be positive");
                }
                if (maxMessageAge == null || maxMessageAge.isNegative()) {
                    throw new IllegalArgumentException("Max message age must be positive");
                }
            }
        }
    }
}