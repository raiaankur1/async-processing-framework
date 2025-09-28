package com.pravah.framework.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB-specific configuration for the Async Processing Framework.
 * <br>
 * This class contains all DynamoDB-related settings including table names,
 * capacity settings, and operational parameters.
 *
 * @author Ankur Rai
 * @version 2.0
 */
@ConfigurationProperties(prefix = "async.framework.aws.dynamodb")
@Validated
public class DynamoDBConfig {

    /**
     * Primary table name for storing async requests.
     */
    @NotBlank
    private String tableName = "async_requests";

    /**
     * Read capacity units for the main table (for provisioned billing mode).
     */
    @Min(1)
    private int readCapacity = 5;

    /**
     * Write capacity units for the main table (for provisioned billing mode).
     */
    @Min(1)
    private int writeCapacity = 5;

    /**
     * Billing mode for the table (PAY_PER_REQUEST or PROVISIONED).
     */
    private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

    /**
     * Whether to create the table automatically if it doesn't exist.
     */
    private boolean createTableIfNotExists = false;

    /**
     * Whether to enable point-in-time recovery.
     */
    private boolean enablePointInTimeRecovery = true;

    /**
     * Whether to enable server-side encryption.
     */
    private boolean enableEncryption = true;

    /**
     * KMS key ID for encryption (null for AWS managed key).
     */
    private String kmsKeyId;

    /**
     * TTL attribute name for automatic item expiration.
     */
    private String ttlAttributeName = "ttl";

    /**
     * Whether to enable TTL for automatic cleanup.
     */
    private boolean enableTtl = true;

    /**
     * Default TTL duration for async requests.
     */
    private Duration defaultTtl = Duration.ofDays(30);

    /**
     * Global Secondary Index configurations.
     */
    private Map<String, GSIConfig> globalSecondaryIndexes = new HashMap<>();

    /**
     * Connection pool size for DynamoDB client.
     */
    @Min(1)
    private int connectionPoolSize = 50;

    /**
     * Maximum number of connections per endpoint.
     */
    @Min(1)
    private int maxConnectionsPerEndpoint = 50;

    /**
     * Connection timeout for DynamoDB operations.
     */
    private Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Socket timeout for DynamoDB operations.
     */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * Request timeout for DynamoDB operations.
     */
    private Duration requestTimeout = Duration.ofMinutes(2);

    /**
     * Whether to enable consistent reads by default.
     */
    private boolean consistentReads = false;

    /**
     * Default constructor with GSI initialization.
     */
    public DynamoDBConfig() {
        // Initialize default GSI for status-based queries
        GSIConfig statusIndex = new GSIConfig()
                .setIndexName("status-created_at-index")
                .setPartitionKey("status")
                .setSortKey("created_at")
                .setReadCapacity(5)
                .setWriteCapacity(5)
                .setProjectionType(ProjectionType.ALL);

        globalSecondaryIndexes.put("status-index", statusIndex);
    }

    // Getters and setters

    public String getTableName() {
        return tableName;
    }

    public DynamoDBConfig setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public int getReadCapacity() {
        return readCapacity;
    }

    public DynamoDBConfig setReadCapacity(int readCapacity) {
        this.readCapacity = readCapacity;
        return this;
    }

    public int getWriteCapacity() {
        return writeCapacity;
    }

    public DynamoDBConfig setWriteCapacity(int writeCapacity) {
        this.writeCapacity = writeCapacity;
        return this;
    }

    public BillingMode getBillingMode() {
        return billingMode;
    }

    public DynamoDBConfig setBillingMode(BillingMode billingMode) {
        this.billingMode = billingMode;
        return this;
    }

    public boolean isCreateTableIfNotExists() {
        return createTableIfNotExists;
    }

    public DynamoDBConfig setCreateTableIfNotExists(boolean createTableIfNotExists) {
        this.createTableIfNotExists = createTableIfNotExists;
        return this;
    }

    public boolean isEnablePointInTimeRecovery() {
        return enablePointInTimeRecovery;
    }

    public DynamoDBConfig setEnablePointInTimeRecovery(boolean enablePointInTimeRecovery) {
        this.enablePointInTimeRecovery = enablePointInTimeRecovery;
        return this;
    }

    public boolean isEnableEncryption() {
        return enableEncryption;
    }

    public DynamoDBConfig setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
        return this;
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public DynamoDBConfig setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
        return this;
    }

    public String getTtlAttributeName() {
        return ttlAttributeName;
    }

    public DynamoDBConfig setTtlAttributeName(String ttlAttributeName) {
        this.ttlAttributeName = ttlAttributeName;
        return this;
    }

    public boolean isEnableTtl() {
        return enableTtl;
    }

    public DynamoDBConfig setEnableTtl(boolean enableTtl) {
        this.enableTtl = enableTtl;
        return this;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public DynamoDBConfig setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
        return this;
    }

    public Map<String, GSIConfig> getGlobalSecondaryIndexes() {
        return globalSecondaryIndexes;
    }

    public DynamoDBConfig setGlobalSecondaryIndexes(Map<String, GSIConfig> globalSecondaryIndexes) {
        this.globalSecondaryIndexes = globalSecondaryIndexes;
        return this;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public DynamoDBConfig setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    public int getMaxConnectionsPerEndpoint() {
        return maxConnectionsPerEndpoint;
    }

    public DynamoDBConfig setMaxConnectionsPerEndpoint(int maxConnectionsPerEndpoint) {
        this.maxConnectionsPerEndpoint = maxConnectionsPerEndpoint;
        return this;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public DynamoDBConfig setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public DynamoDBConfig setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public DynamoDBConfig setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public boolean isConsistentReads() {
        return consistentReads;
    }

    public DynamoDBConfig setConsistentReads(boolean consistentReads) {
        this.consistentReads = consistentReads;
        return this;
    }

    /**
     * Validate the DynamoDB configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("DynamoDB table name must be specified");
        }

        if (readCapacity <= 0) {
            throw new IllegalArgumentException("Read capacity must be positive");
        }

        if (writeCapacity <= 0) {
            throw new IllegalArgumentException("Write capacity must be positive");
        }

        if (connectionPoolSize <= 0) {
            throw new IllegalArgumentException("Connection pool size must be positive");
        }

        if (maxConnectionsPerEndpoint <= 0) {
            throw new IllegalArgumentException("Max connections per endpoint must be positive");
        }

        if (connectionTimeout == null || connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }

        if (socketTimeout == null || socketTimeout.isNegative()) {
            throw new IllegalArgumentException("Socket timeout must be positive");
        }

        if (requestTimeout == null || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("Request timeout must be positive");
        }

        if (defaultTtl == null || defaultTtl.isNegative()) {
            throw new IllegalArgumentException("Default TTL must be positive");
        }

        // Validate GSI configurations
        if (globalSecondaryIndexes != null) {
            globalSecondaryIndexes.values().forEach(GSIConfig::validate);
        }
    }

    /**
     * DynamoDB billing mode enumeration.
     */
    public enum BillingMode {
        PROVISIONED,
        PAY_PER_REQUEST
    }

    /**
     * DynamoDB projection type enumeration.
     */
    public enum ProjectionType {
        ALL,
        KEYS_ONLY,
        INCLUDE
    }

    /**
     * Global Secondary Index configuration.
     */
    public static class GSIConfig {
        private String indexName;
        private String partitionKey;
        private String sortKey;
        private int readCapacity = 5;
        private int writeCapacity = 5;
        private ProjectionType projectionType = ProjectionType.ALL;
        private String[] nonKeyAttributes;

        public String getIndexName() {
            return indexName;
        }

        public GSIConfig setIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public String getPartitionKey() {
            return partitionKey;
        }

        public GSIConfig setPartitionKey(String partitionKey) {
            this.partitionKey = partitionKey;
            return this;
        }

        public String getSortKey() {
            return sortKey;
        }

        public GSIConfig setSortKey(String sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        public int getReadCapacity() {
            return readCapacity;
        }

        public GSIConfig setReadCapacity(int readCapacity) {
            this.readCapacity = readCapacity;
            return this;
        }

        public int getWriteCapacity() {
            return writeCapacity;
        }

        public GSIConfig setWriteCapacity(int writeCapacity) {
            this.writeCapacity = writeCapacity;
            return this;
        }

        public ProjectionType getProjectionType() {
            return projectionType;
        }

        public GSIConfig setProjectionType(ProjectionType projectionType) {
            this.projectionType = projectionType;
            return this;
        }

        public String[] getNonKeyAttributes() {
            return nonKeyAttributes;
        }

        public GSIConfig setNonKeyAttributes(String[] nonKeyAttributes) {
            this.nonKeyAttributes = nonKeyAttributes;
            return this;
        }

        public void validate() {
            if (indexName == null || indexName.trim().isEmpty()) {
                throw new IllegalArgumentException("GSI index name must be specified");
            }

            if (partitionKey == null || partitionKey.trim().isEmpty()) {
                throw new IllegalArgumentException("GSI partition key must be specified");
            }

            if (readCapacity <= 0) {
                throw new IllegalArgumentException("GSI read capacity must be positive");
            }

            if (writeCapacity <= 0) {
                throw new IllegalArgumentException("GSI write capacity must be positive");
            }

            if (projectionType == ProjectionType.INCLUDE &&
                    (nonKeyAttributes == null || nonKeyAttributes.length == 0)) {
                throw new IllegalArgumentException("Non-key attributes must be specified for INCLUDE projection type");
            }
        }
    }

    /**
     * Create a configuration optimized for development.
     *
     * @return development-optimized configuration
     */
    public static DynamoDBConfig forDevelopment() {
        return new DynamoDBConfig()
                .setTableName("async_requests_dev")
                .setBillingMode(BillingMode.PAY_PER_REQUEST)
                .setCreateTableIfNotExists(true)
                .setEnablePointInTimeRecovery(false)
                .setEnableEncryption(false)
                .setDefaultTtl(Duration.ofDays(7))
                .setConnectionPoolSize(10);
    }

    /**
     * Create a configuration optimized for production.
     *
     * @return production-optimized configuration
     */
    public static DynamoDBConfig forProduction() {
        return new DynamoDBConfig()
                .setTableName("async_requests")
                .setBillingMode(BillingMode.PAY_PER_REQUEST)
                .setCreateTableIfNotExists(false)
                .setEnablePointInTimeRecovery(true)
                .setEnableEncryption(true)
                .setDefaultTtl(Duration.ofDays(30))
                .setConnectionPoolSize(50);
    }

    @Override
    public String toString() {
        return String.format("DynamoDBConfig{" +
                        "tableName='%s', " +
                        "billingMode=%s, " +
                        "readCapacity=%d, " +
                        "writeCapacity=%d, " +
                        "enableTtl=%s, " +
                        "defaultTtl=%s" +
                        "}",
                tableName, billingMode, readCapacity, writeCapacity, enableTtl, defaultTtl);
    }
}