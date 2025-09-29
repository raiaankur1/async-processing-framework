package com.pravah.framework.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * S3-specific configuration for the Async Processing Framework.
 * <br>
 * This class contains all S3-related settings including bucket names,
 * encryption, lifecycle policies, and performance optimizations.
 *
 * @author Ankur Rai
 * @version 1.0
 */
@ConfigurationProperties(prefix = "async.framework.aws.s3")
@Validated
public class S3Config {

    /**
     * Primary S3 bucket name for storing payloads.
     */
    @NotBlank
    private String bucketName = "async-framework-payloads";

    /**
     * Key prefix for all objects stored in S3.
     */
    private String keyPrefix = "async-requests/";

    /**
     * Whether to create the bucket automatically if it doesn't exist.
     */
    private boolean createBucketIfNotExists = false;

    /**
     * S3 storage class for objects.
     */
    private StorageClass storageClass = StorageClass.STANDARD;

    /**
     * Server-side encryption configuration.
     */
    private EncryptionConfig encryption = new EncryptionConfig();

    /**
     * Lifecycle policy configuration.
     */
    private LifecyclePolicyConfig lifecyclePolicy = new LifecyclePolicyConfig();

    /**
     * Multipart upload configuration.
     */
    private MultipartUploadConfig multipartUpload = new MultipartUploadConfig();

    /**
     * Transfer acceleration configuration.
     */
    private boolean enableTransferAcceleration = false;

    /**
     * Whether to enable versioning on the bucket.
     */
    private boolean enableVersioning = false;

    /**
     * Whether to enable public read access blocking.
     */
    private boolean blockPublicAccess = true;

    /**
     * Connection timeout for S3 operations.
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * Socket timeout for S3 operations.
     */
    private Duration socketTimeout = Duration.ofMinutes(2);

    /**
     * Request timeout for S3 operations.
     */
    private Duration requestTimeout = Duration.ofMinutes(5);

    /**
     * Maximum number of connections in the connection pool.
     */
    @Min(1)
    private int maxConnections = 50;

    /**
     * Whether to use path-style access (useful for LocalStack).
     */
    private boolean usePathStyleAccess = false;

    /**
     * Custom S3 endpoint URL (useful for LocalStack or S3-compatible services).
     */
    private String endpointUrl;

    /**
     * Metadata to be added to all objects.
     */
    private Map<String, String> defaultMetadata = new HashMap<>();

    /**
     * Tags to be added to all objects.
     */
    private Map<String, String> defaultTags = new HashMap<>();

    /**
     * Default constructor.
     */
    public S3Config() {
        // Initialize default metadata
        defaultMetadata.put("CreatedBy", "AsyncFramework");
        defaultMetadata.put("Purpose", "AsyncRequestPayload");

        // Initialize default tags
        defaultTags.put("Service", "AsyncFramework");
        defaultTags.put("Environment", "production");
    }

    // Getters and setters

    public String getBucketName() {
        return bucketName;
    }

    public S3Config setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public S3Config setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
    }

    public boolean isCreateBucketIfNotExists() {
        return createBucketIfNotExists;
    }

    public S3Config setCreateBucketIfNotExists(boolean createBucketIfNotExists) {
        this.createBucketIfNotExists = createBucketIfNotExists;
        return this;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public S3Config setStorageClass(StorageClass storageClass) {
        this.storageClass = storageClass;
        return this;
    }

    public EncryptionConfig getEncryption() {
        return encryption;
    }

    public S3Config setEncryption(EncryptionConfig encryption) {
        this.encryption = encryption;
        return this;
    }

    public LifecyclePolicyConfig getLifecyclePolicy() {
        return lifecyclePolicy;
    }

    public S3Config setLifecyclePolicy(LifecyclePolicyConfig lifecyclePolicy) {
        this.lifecyclePolicy = lifecyclePolicy;
        return this;
    }

    public MultipartUploadConfig getMultipartUpload() {
        return multipartUpload;
    }

    public S3Config setMultipartUpload(MultipartUploadConfig multipartUpload) {
        this.multipartUpload = multipartUpload;
        return this;
    }

    public boolean isEnableTransferAcceleration() {
        return enableTransferAcceleration;
    }

    public S3Config setEnableTransferAcceleration(boolean enableTransferAcceleration) {
        this.enableTransferAcceleration = enableTransferAcceleration;
        return this;
    }

    public boolean isEnableVersioning() {
        return enableVersioning;
    }

    public S3Config setEnableVersioning(boolean enableVersioning) {
        this.enableVersioning = enableVersioning;
        return this;
    }

    public boolean isBlockPublicAccess() {
        return blockPublicAccess;
    }

    public S3Config setBlockPublicAccess(boolean blockPublicAccess) {
        this.blockPublicAccess = blockPublicAccess;
        return this;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public S3Config setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    public S3Config setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public S3Config setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public S3Config setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public boolean isUsePathStyleAccess() {
        return usePathStyleAccess;
    }

    public S3Config setUsePathStyleAccess(boolean usePathStyleAccess) {
        this.usePathStyleAccess = usePathStyleAccess;
        return this;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public S3Config setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        return this;
    }

    public Map<String, String> getDefaultMetadata() {
        return defaultMetadata;
    }

    public S3Config setDefaultMetadata(Map<String, String> defaultMetadata) {
        this.defaultMetadata = defaultMetadata;
        return this;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public S3Config setDefaultTags(Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
        return this;
    }

    /**
     * Generate a full S3 key for a given request ID and key suffix.
     *
     * @param requestId the async request ID
     * @param keySuffix additional key suffix (optional)
     * @return the full S3 key
     */
    public String generateKey(String requestId, String keySuffix) {
        StringBuilder keyBuilder = new StringBuilder();

        if (keyPrefix != null && !keyPrefix.isEmpty()) {
            keyBuilder.append(keyPrefix);
            if (!keyPrefix.endsWith("/")) {
                keyBuilder.append("/");
            }
        }

        keyBuilder.append(requestId);

        if (keySuffix != null && !keySuffix.isEmpty()) {
            if (!keySuffix.startsWith("/")) {
                keyBuilder.append("/");
            }
            keyBuilder.append(keySuffix);
        }

        return keyBuilder.toString();
    }

    /**
     * Validate the S3 configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 bucket name must be specified");
        }

        // Validate bucket name format (basic validation)
        if (!bucketName.matches("^[a-z0-9][a-z0-9.-]*[a-z0-9]$")) {
            throw new IllegalArgumentException("S3 bucket name must follow AWS naming conventions");
        }

        if (bucketName.length() < 3 || bucketName.length() > 63) {
            throw new IllegalArgumentException("S3 bucket name must be between 3 and 63 characters");
        }

        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max connections must be positive");
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

        // Validate nested configurations
        if (encryption != null) {
            encryption.validate();
        }
        if (lifecyclePolicy != null) {
            lifecyclePolicy.validate();
        }
        if (multipartUpload != null) {
            multipartUpload.validate();
        }
    }

    /**
     * S3 storage class enumeration.
     */
    public enum StorageClass {
        STANDARD,
        REDUCED_REDUNDANCY,
        STANDARD_IA,
        ONEZONE_IA,
        INTELLIGENT_TIERING,
        GLACIER,
        DEEP_ARCHIVE
    }

    /**
     * S3 server-side encryption configuration.
     */
    public static class EncryptionConfig {
        private boolean enabled = true;
        private EncryptionType type = EncryptionType.SSE_S3;
        private String kmsKeyId;
        private String kmsKeyArn;

        public boolean isEnabled() {
            return enabled;
        }

        public EncryptionConfig setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public EncryptionType getType() {
            return type;
        }

        public EncryptionConfig setType(EncryptionType type) {
            this.type = type;
            return this;
        }

        public String getKmsKeyId() {
            return kmsKeyId;
        }

        public EncryptionConfig setKmsKeyId(String kmsKeyId) {
            this.kmsKeyId = kmsKeyId;
            return this;
        }

        public String getKmsKeyArn() {
            return kmsKeyArn;
        }

        public EncryptionConfig setKmsKeyArn(String kmsKeyArn) {
            this.kmsKeyArn = kmsKeyArn;
            return this;
        }

        public void validate() {
            if (enabled && type == EncryptionType.SSE_KMS) {
                if ((kmsKeyId == null || kmsKeyId.trim().isEmpty()) &&
                        (kmsKeyArn == null || kmsKeyArn.trim().isEmpty())) {
                    throw new IllegalArgumentException("KMS key ID or ARN must be specified for SSE-KMS encryption");
                }
            }
        }

        public enum EncryptionType {
            SSE_S3,
            SSE_KMS,
            SSE_C
        }
    }

    /**
     * S3 lifecycle policy configuration.
     */
    public static class LifecyclePolicyConfig {
        private boolean enabled = true;
        private Duration transitionToIA = Duration.ofDays(30);
        private Duration transitionToGlacier = Duration.ofDays(90);
        private Duration transitionToDeepArchive = Duration.ofDays(180);
        private Duration expiration = Duration.ofDays(365);
        private Duration abortIncompleteMultipartUpload = Duration.ofDays(7);

        public boolean isEnabled() {
            return enabled;
        }

        public LifecyclePolicyConfig setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Duration getTransitionToIA() {
            return transitionToIA;
        }

        public LifecyclePolicyConfig setTransitionToIA(Duration transitionToIA) {
            this.transitionToIA = transitionToIA;
            return this;
        }

        public Duration getTransitionToGlacier() {
            return transitionToGlacier;
        }

        public LifecyclePolicyConfig setTransitionToGlacier(Duration transitionToGlacier) {
            this.transitionToGlacier = transitionToGlacier;
            return this;
        }

        public Duration getTransitionToDeepArchive() {
            return transitionToDeepArchive;
        }

        public LifecyclePolicyConfig setTransitionToDeepArchive(Duration transitionToDeepArchive) {
            this.transitionToDeepArchive = transitionToDeepArchive;
            return this;
        }

        public Duration getExpiration() {
            return expiration;
        }

        public LifecyclePolicyConfig setExpiration(Duration expiration) {
            this.expiration = expiration;
            return this;
        }

        public Duration getAbortIncompleteMultipartUpload() {
            return abortIncompleteMultipartUpload;
        }

        public LifecyclePolicyConfig setAbortIncompleteMultipartUpload(Duration abortIncompleteMultipartUpload) {
            this.abortIncompleteMultipartUpload = abortIncompleteMultipartUpload;
            return this;
        }

        public void validate() {
            if (enabled) {
                if (transitionToIA != null && transitionToIA.isNegative()) {
                    throw new IllegalArgumentException("Transition to IA duration must be positive");
                }
                if (transitionToGlacier != null && transitionToGlacier.isNegative()) {
                    throw new IllegalArgumentException("Transition to Glacier duration must be positive");
                }
                if (transitionToDeepArchive != null && transitionToDeepArchive.isNegative()) {
                    throw new IllegalArgumentException("Transition to Deep Archive duration must be positive");
                }
                if (expiration != null && expiration.isNegative()) {
                    throw new IllegalArgumentException("Expiration duration must be positive");
                }
                if (abortIncompleteMultipartUpload != null && abortIncompleteMultipartUpload.isNegative()) {
                    throw new IllegalArgumentException("Abort incomplete multipart upload duration must be positive");
                }
            }
        }
    }

    /**
     * S3 multipart upload configuration.
     */
    public static class MultipartUploadConfig {
        private boolean enabled = true;
        private long thresholdBytes = 5 * 1024 * 1024; // 5MB
        private long partSizeBytes = 5 * 1024 * 1024; // 5MB
        private int maxConcurrentParts = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public MultipartUploadConfig setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public long getThresholdBytes() {
            return thresholdBytes;
        }

        public MultipartUploadConfig setThresholdBytes(long thresholdBytes) {
            this.thresholdBytes = thresholdBytes;
            return this;
        }

        public long getPartSizeBytes() {
            return partSizeBytes;
        }

        public MultipartUploadConfig setPartSizeBytes(long partSizeBytes) {
            this.partSizeBytes = partSizeBytes;
            return this;
        }

        public int getMaxConcurrentParts() {
            return maxConcurrentParts;
        }

        public MultipartUploadConfig setMaxConcurrentParts(int maxConcurrentParts) {
            this.maxConcurrentParts = maxConcurrentParts;
            return this;
        }

        public void validate() {
            if (enabled) {
                if (thresholdBytes < 5 * 1024 * 1024) { // 5MB minimum
                    throw new IllegalArgumentException("Multipart upload threshold must be at least 5MB");
                }
                if (partSizeBytes < 5 * 1024 * 1024) { // 5MB minimum
                    throw new IllegalArgumentException("Multipart upload part size must be at least 5MB");
                }
                if (maxConcurrentParts <= 0) {
                    throw new IllegalArgumentException("Max concurrent parts must be positive");
                }
            }
        }
    }

    /**
     * Create a configuration optimized for development.
     *
     * @return development-optimized configuration
     */
    public static S3Config forDevelopment() {
        return new S3Config()
                .setBucketName("async-framework-dev")
                .setCreateBucketIfNotExists(true)
                .setStorageClass(StorageClass.STANDARD)
                .setEnableVersioning(false)
                .setMaxConnections(10);
    }

    /**
     * Create a configuration optimized for production.
     *
     * @return production-optimized configuration
     */
    public static S3Config forProduction() {
        return new S3Config()
                .setBucketName("async-framework-prod")
                .setCreateBucketIfNotExists(false)
                .setStorageClass(StorageClass.INTELLIGENT_TIERING)
                .setEnableVersioning(true)
                .setEnableTransferAcceleration(true)
                .setMaxConnections(50);
    }

    /**
     * Create a configuration optimized for LocalStack.
     *
     * @param endpoint the LocalStack S3 endpoint
     * @return LocalStack-optimized configuration
     */
    public static S3Config forLocalStack(String endpoint) {
        return new S3Config()
                .setBucketName("async-framework-local")
                .setCreateBucketIfNotExists(true)
                .setUsePathStyleAccess(true)
                .setEndpointUrl(endpoint)
                .setStorageClass(StorageClass.STANDARD)
                .setMaxConnections(5);
    }

    @Override
    public String toString() {
        return String.format("S3Config{" +
                        "bucketName='%s', " +
                        "keyPrefix='%s', " +
                        "storageClass=%s, " +
                        "encryptionEnabled=%s, " +
                        "lifecyclePolicyEnabled=%s, " +
                        "multipartUploadEnabled=%s" +
                        "}",
                bucketName, keyPrefix, storageClass,
                encryption.isEnabled(), lifecyclePolicy.isEnabled(), multipartUpload.isEnabled());
    }
}