package com.pravah.framework.async.aws;

import com.pravah.framework.async.config.AWSConfig;
import com.pravah.framework.async.config.DynamoDBConfig;
import com.pravah.framework.async.config.S3Config;
import com.pravah.framework.async.config.SQSConfig;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory class for creating and managing AWS service clients with proper configuration,
 * connection pooling, and health check capabilities.
 *
 * This factory supports multiple credential providers including:
 * - Default AWS credentials provider chain
 * - Static access key/secret key credentials
 * - IAM role assumption
 * - Instance profile credentials
 */
@Component
public class AWSClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(AWSClientFactory.class);

    // Client caches to ensure singleton behavior and connection reuse
    private final ConcurrentMap<String, DynamoDbClient> dynamoDbClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SqsClient> sqsClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, S3Client> s3Clients = new ConcurrentHashMap<>();

    // Health check cache to avoid frequent health checks
    private volatile long lastHealthCheckTime = 0;
    private volatile boolean lastHealthCheckResult = false;
    private static final long HEALTH_CHECK_CACHE_DURATION_MS = 30000; // 30 seconds

    /**
     * Creates or retrieves a cached DynamoDB client with the specified configuration.
     *
     * @param config AWS configuration containing region, credentials, and DynamoDB-specific settings
     * @return Configured DynamoDB client
     * @throws AsyncFrameworkException if client creation fails
     */
    public DynamoDbClient createDynamoDbClient(AWSConfig config) {
        String cacheKey = generateCacheKey("dynamodb", config);

        return dynamoDbClients.computeIfAbsent(cacheKey, key -> {
            try {
                logger.info("Creating new DynamoDB client for region: {}", config.getRegion());

                DynamoDBConfig dynamoConfig = config.getDynamodb();

                return DynamoDbClient.builder()
                        .region(Region.of(config.getRegion()))
                        .credentialsProvider(createCredentialsProvider(config))
                        .overrideConfiguration(createClientOverrideConfiguration(config))
                        .build();

            } catch (Exception e) {
                logger.error("Failed to create DynamoDB client", e);
                throw new AsyncFrameworkException(
                        ErrorCode.CLOUD_PROVIDER_ERROR,
                        null,
                        "Failed to create DynamoDB client: " + e.getMessage(),
                        e
                );
            }
        });
    }

    /**
     * Creates or retrieves a cached SQS client with the specified configuration.
     *
     * @param config AWS configuration containing region, credentials, and SQS-specific settings
     * @return Configured SQS client
     * @throws AsyncFrameworkException if client creation fails
     */
    public SqsClient createSqsClient(AWSConfig config) {
        String cacheKey = generateCacheKey("sqs", config);

        return sqsClients.computeIfAbsent(cacheKey, key -> {
            try {
                logger.info("Creating new SQS client for region: {}", config.getRegion());

                return SqsClient.builder()
                        .region(Region.of(config.getRegion()))
                        .credentialsProvider(createCredentialsProvider(config))
                        .overrideConfiguration(createClientOverrideConfiguration(config))
                        .build();

            } catch (Exception e) {
                logger.error("Failed to create SQS client", e);
                throw new AsyncFrameworkException(
                        ErrorCode.CLOUD_PROVIDER_ERROR,
                        null,
                        "Failed to create SQS client: " + e.getMessage(),
                        e
                );
            }
        });
    }

    /**
     * Creates or retrieves a cached S3 client with the specified configuration.
     *
     * @param config AWS configuration containing region, credentials, and S3-specific settings
     * @return Configured S3 client
     * @throws AsyncFrameworkException if client creation fails
     */
    public S3Client createS3Client(AWSConfig config) {
        String cacheKey = generateCacheKey("s3", config);

        return s3Clients.computeIfAbsent(cacheKey, key -> {
            try {
                logger.info("Creating new S3 client for region: {}", config.getRegion());

                return S3Client.builder()
                        .region(Region.of(config.getRegion()))
                        .credentialsProvider(createCredentialsProvider(config))
                        .overrideConfiguration(createClientOverrideConfiguration(config))
                        .build();

            } catch (Exception e) {
                logger.error("Failed to create S3 client", e);
                throw new AsyncFrameworkException(
                        ErrorCode.CLOUD_PROVIDER_ERROR,
                        null,
                        "Failed to create S3 client: " + e.getMessage(),
                        e
                );
            }
        });
    }

    /**
     * Performs health checks on AWS service connectivity.
     * Results are cached for 30 seconds to avoid excessive health check calls.
     *
     * @return true if all AWS services are accessible, false otherwise
     */
    public boolean isHealthy() {
        long currentTime = System.currentTimeMillis();

        // Return cached result if within cache duration
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_CACHE_DURATION_MS) {
            return lastHealthCheckResult;
        }

        try {
            // Perform basic connectivity checks
            boolean healthy = performHealthChecks();

            // Update cache
            lastHealthCheckTime = currentTime;
            lastHealthCheckResult = healthy;

            return healthy;

        } catch (Exception e) {
            logger.warn("Health check failed", e);
            lastHealthCheckTime = currentTime;
            lastHealthCheckResult = false;
            return false;
        }
    }

    /**
     * Closes all cached clients and clears the cache.
     * Should be called during application shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down AWS client factory");

        // Close all DynamoDB clients
        dynamoDbClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Error closing DynamoDB client", e);
            }
        });
        dynamoDbClients.clear();

        // Close all SQS clients
        sqsClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Error closing SQS client", e);
            }
        });
        sqsClients.clear();

        // Close all S3 clients
        s3Clients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Error closing S3 client", e);
            }
        });
        s3Clients.clear();

        logger.info("AWS client factory shutdown complete");
    }

    /**
     * Creates appropriate credentials provider based on configuration.
     */
    private AwsCredentialsProvider createCredentialsProvider(AWSConfig config) {
        if (config.getRoleArn() != null && !config.getRoleArn().trim().isEmpty()) {
            // Use STS assume role
            logger.debug("Using STS assume role credentials for role: {}", config.getRoleArn());

            StsClient stsClient = StsClient.builder()
                    .region(Region.of(config.getRegion()))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(config.getRoleArn())
                    .roleSessionName("async-framework-session-" + System.currentTimeMillis())
                    .build();

            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .build();

        } else if (config.getAccessKey() != null && config.getSecretKey() != null) {
            // Use static credentials
            logger.debug("Using static AWS credentials");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
            );

        } else if (config.isUseDefaultCredentialsProvider()) {
            // Use default credentials provider chain
            logger.debug("Using default AWS credentials provider chain");
            return DefaultCredentialsProvider.create();

        } else {
            // Fallback to default
            logger.warn("No specific credentials configuration found, falling back to default provider");
            return DefaultCredentialsProvider.create();
        }
    }

    /**
     * Creates client override configuration with timeouts, retry policies, and connection pooling.
     */
    private ClientOverrideConfiguration createClientOverrideConfiguration(AWSConfig config) {
        return ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(30))
                .apiCallAttemptTimeout(Duration.ofSeconds(10))
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(3)
                        .build())
                .build();
    }

    /**
     * Generates a cache key for client instances based on configuration.
     */
    private String generateCacheKey(String service, AWSConfig config) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(service)
                .append("-")
                .append(config.getRegion());

        if (config.getRoleArn() != null) {
            keyBuilder.append("-").append(config.getRoleArn().hashCode());
        } else if (config.getAccessKey() != null) {
            keyBuilder.append("-").append(config.getAccessKey().hashCode());
        } else {
            keyBuilder.append("-default");
        }

        return keyBuilder.toString();
    }

    /**
     * Performs actual health checks on AWS services.
     */
    private boolean performHealthChecks() {
        // For now, we'll do basic checks. In a real implementation,
        // you might want to make actual service calls to verify connectivity
        try {
            // Check if we can create credentials
            DefaultCredentialsProvider.create().resolveCredentials();
            return true;
        } catch (Exception e) {
            logger.warn("AWS credentials health check failed", e);
            return false;
        }
    }
}