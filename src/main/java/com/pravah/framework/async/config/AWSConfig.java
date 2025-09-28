package com.pravah.framework.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * AWS-specific configuration for the Async Processing Framework.
 * <br>
 * This class contains all AWS service configurations including
 * credentials, regions, and service-specific settings.
 *
 * @author Ankur Rai
 * @version 2.0
 */
@ConfigurationProperties(prefix = "async.framework.aws")
@Validated
public class AWSConfig {

    /**
     * AWS region to use for all services.
     */
    @NotBlank
    private String region = "ap-south-1";

    /**
     * AWS access key ID (optional if using IAM roles).
     */
    private String accessKey;

    /**
     * AWS secret access key (optional if using IAM roles).
     */
    private String secretKey;

    /**
     * AWS IAM role ARN to assume (optional).
     */
    private String roleArn;

    /**
     * External ID for role assumption (optional).
     */
    private String externalId;

    /**
     * Session name for role assumption.
     */
    private String sessionName = "AsyncFrameworkSession";

    /**
     * Whether to use the default AWS credentials provider chain.
     */
    private boolean useDefaultCredentialsProvider = true;

    /**
     * Connection timeout for AWS service calls.
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * Socket timeout for AWS service calls.
     */
    private Duration socketTimeout = Duration.ofMinutes(2);

    /**
     * Maximum number of retry attempts for AWS service calls.
     */
    private int maxRetryAttempts = 3;

    /**
     * Whether to use path-style access for S3 (useful for LocalStack).
     */
    private boolean usePathStyleAccess = false;

    /**
     * Custom endpoint URL (useful for LocalStack or custom AWS endpoints).
     */
    private String endpointUrl;

    /**
     * DynamoDB-specific configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private DynamoDBConfig dynamodb = new DynamoDBConfig();

    /**
     * SQS-specific configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private SQSConfig sqs = new SQSConfig();

    /**
     * S3-specific configuration.
     */
    @NestedConfigurationProperty
    @Valid
    @NotNull
    private S3Config s3 = new S3Config();

    /**
     * Default constructor.
     */
    public AWSConfig() {
        // Default values are set via field initialization
    }

    // Getters and setters

    public String getRegion() {
        return region;
    }

    public AWSConfig setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public AWSConfig setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public AWSConfig setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public AWSConfig setRoleArn(String roleArn) {
        this.roleArn = roleArn;
        return this;
    }

    public String getExternalId() {
        return externalId;
    }

    public AWSConfig setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public String getSessionName() {
        return sessionName;
    }

    public AWSConfig setSessionName(String sessionName) {
        this.sessionName = sessionName;
        return this;
    }

    public boolean isUseDefaultCredentialsProvider() {
        return useDefaultCredentialsProvider;
    }

    public AWSConfig setUseDefaultCredentialsProvider(boolean useDefaultCredentialsProvider) {
        this.useDefaultCredentialsProvider = useDefaultCredentialsProvider;
        return this;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public AWSConfig setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public AWSConfig setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public AWSConfig setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
        return this;
    }

    public boolean isUsePathStyleAccess() {
        return usePathStyleAccess;
    }

    public AWSConfig setUsePathStyleAccess(boolean usePathStyleAccess) {
        this.usePathStyleAccess = usePathStyleAccess;
        return this;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public AWSConfig setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        return this;
    }

    public DynamoDBConfig getDynamodb() {
        return dynamodb;
    }

    public AWSConfig setDynamodb(DynamoDBConfig dynamodb) {
        this.dynamodb = dynamodb;
        return this;
    }

    public SQSConfig getSqs() {
        return sqs;
    }

    public AWSConfig setSqs(SQSConfig sqs) {
        this.sqs = sqs;
        return this;
    }

    public S3Config getS3() {
        return s3;
    }

    public AWSConfig setS3(S3Config s3) {
        this.s3 = s3;
        return this;
    }

    /**
     * Check if explicit credentials are configured.
     *
     * @return true if access key and secret key are both provided
     */
    public boolean hasExplicitCredentials() {
        return accessKey != null && !accessKey.trim().isEmpty()
                && secretKey != null && !secretKey.trim().isEmpty();
    }

    /**
     * Check if role assumption is configured.
     *
     * @return true if role ARN is provided
     */
    public boolean hasRoleAssumption() {
        return roleArn != null && !roleArn.trim().isEmpty();
    }

    /**
     * Validate the AWS configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("AWS region must be specified");
        }

        if (connectionTimeout == null || connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }

        if (socketTimeout == null || socketTimeout.isNegative()) {
            throw new IllegalArgumentException("Socket timeout must be positive");
        }

        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("Max retry attempts cannot be negative");
        }

        // Validate that if explicit credentials are partially provided, both are required
        boolean hasAccessKey = accessKey != null && !accessKey.trim().isEmpty();
        boolean hasSecretKey = secretKey != null && !secretKey.trim().isEmpty();

        if (hasAccessKey != hasSecretKey) {
            throw new IllegalArgumentException("Both access key and secret key must be provided together");
        }

        // Validate nested configurations
        if (dynamodb != null) {
            dynamodb.validate();
        }
        if (sqs != null) {
            sqs.validate();
        }
        if (s3 != null) {
            s3.validate();
        }
    }

    /**
     * Create a configuration optimized for LocalStack development.
     *
     * @param localStackEndpoint the LocalStack endpoint URL
     * @return LocalStack-optimized configuration
     */
    public static AWSConfig forLocalStack(String localStackEndpoint) {
        return new AWSConfig()
                .setRegion("us-east-1")
                .setAccessKey("test")
                .setSecretKey("test")
                .setUseDefaultCredentialsProvider(false)
                .setUsePathStyleAccess(true)
                .setEndpointUrl(localStackEndpoint)
                .setConnectionTimeout(Duration.ofSeconds(10))
                .setSocketTimeout(Duration.ofSeconds(30))
                .setMaxRetryAttempts(1);
    }

    /**
     * Create a configuration optimized for production with IAM roles.
     *
     * @param region the AWS region
     * @return production-optimized configuration
     */
    public static AWSConfig forProduction(String region) {
        return new AWSConfig()
                .setRegion(region)
                .setUseDefaultCredentialsProvider(true)
                .setConnectionTimeout(Duration.ofSeconds(30))
                .setSocketTimeout(Duration.ofMinutes(2))
                .setMaxRetryAttempts(3);
    }

    @Override
    public String toString() {
        return String.format("AWSConfig{" +
                        "region='%s', " +
                        "useDefaultCredentialsProvider=%s, " +
                        "hasExplicitCredentials=%s, " +
                        "hasRoleAssumption=%s, " +
                        "connectionTimeout=%s, " +
                        "socketTimeout=%s" +
                        "}",
                region, useDefaultCredentialsProvider, hasExplicitCredentials(),
                hasRoleAssumption(), connectionTimeout, socketTimeout);
    }
}