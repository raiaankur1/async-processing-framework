package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Storage Client interface for async request payload handling.
 * <br>
 * This interface provides comprehensive storage operations including
 * object CRUD, streaming, metadata handling, and lifecycle management.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public interface StorageClient {

    /**
     * Get an object as a string.
     *
     * @param id the container/bucket identifier (typically request ID)
     * @param key the object key
     * @return object content as string
     * @throws AsyncFrameworkException if retrieval fails
     */
    String getObject(String id, String key);

    /**
     * Get an object as a byte array.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @return object content as byte array
     * @throws AsyncFrameworkException if retrieval fails
     */
    byte[] getObjectAsBytes(String id, String key);

    /**
     * Get an object as an input stream.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @return object content as input stream
     * @throws AsyncFrameworkException if retrieval fails
     */
    InputStream getObjectAsStream(String id, String key);

    /**
     * Retrieves an object with metadata.
     *
     * @param id The request ID or container identifier
     * @param key The object key
     * @return StorageObject containing content and metadata
     * @throws AsyncFrameworkException if retrieval fails
     */
    StorageObject getObjectWithMetadata(String id, String key);

    /**
     * Put an object from string content.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param content the content to store
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, String content);

    /**
     * Put an object from byte array.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param content the content to store
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, byte[] content);

    /**
     * Put an object from input stream.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param content the content stream
     * @param contentLength the content length (-1 if unknown)
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, InputStream content, long contentLength);

    /**
     * Stores an object with custom metadata and options.
     *
     * @param id The request ID or container identifier
     * @param key The object key
     * @param content The content to store
     * @param metadata Custom metadata to associate with the object
     * @param options Storage options (encryption, storage class, etc.)
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, byte[] content, Map<String, String> metadata, StorageOptions options);

    /**
     * Stores an object using multipart upload for large objects.
     *
     * @param id The request ID or container identifier
     * @param key The object key
     * @param inputStream The input stream containing the content
     * @param contentLength The length of the content in bytes
     * @param partSize The size of each part in bytes (minimum 5MB for S3)
     * @return Upload result containing ETag and other information
     * @throws AsyncFrameworkException if multipart upload fails
     */
    UploadResult putObjectMultipart(String id, String key, InputStream inputStream, long contentLength, long partSize);


    /**
     * Check if an object exists.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @return true if object exists, false otherwise
     * @throws AsyncFrameworkException if check fails
     */
    boolean objectExists(String id, String key);

    /**
     * Delete an object.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @throws AsyncFrameworkException if deletion fails
     */
    void deleteObject(String id, String key);

    /**
     * List objects with a given prefix.
     *
     * @param id the container/bucket identifier
     * @param prefix the key prefix to filter by
     * @return list of object metadata
     * @throws AsyncFrameworkException if listing fails
     */
    List<ObjectSummary> listObjects(String id, String prefix);

    /**
     * List objects with pagination.
     *
     * @param id the container/bucket identifier
     * @param prefix the key prefix to filter by
     * @param maxKeys maximum number of keys to return
     * @param continuationToken pagination token (null for first page)
     * @return paginated list of object metadata
     * @throws AsyncFrameworkException if listing fails
     */
    ListResult listObjects(String id, String prefix, int maxKeys, String continuationToken);

    /**
     * Get object metadata without retrieving the content.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @return object metadata
     * @throws AsyncFrameworkException if retrieval fails
     */
    ObjectMetadata getObjectMetadata(String id, String key);

    /**
     * Copy an object to a new location.
     *
     * @param sourceId source container/bucket identifier
     * @param sourceKey source object key
     * @param destId destination container/bucket identifier
     * @param destKey destination object key
     * @throws AsyncFrameworkException if copy fails
     */
    void copyObject(String sourceId, String sourceKey, String destId, String destKey);

    /**
     * Generate a pre-signed URL for object access.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param expiration URL expiration time in seconds
     * @param operation the operation type (GET, PUT, DELETE)
     * @return pre-signed URL
     * @throws AsyncFrameworkException if URL generation fails
     */
    String generatePresignedUrl(String id, String key, Instant expiration, Operation operation);

    /**
     * Batch delete multiple objects.
     *
     * @param id the container/bucket identifier
     * @param keys list of object keys to delete
     * @return list of deletion results
     * @throws AsyncFrameworkException if batch delete fails
     */
    List<DeletionResult> deleteObjects(String id, List<String> keys);
    /**
     * Sets lifecycle policy for automatic object cleanup.
     *
     * @param id The request ID or container identifier
     * @param lifecycleRules List of lifecycle rules
     * @throws AsyncFrameworkException if lifecycle policy setting fails
     */
    void setLifecyclePolicy(String id, List<LifecycleRule> lifecycleRules);

    /**
     * Gets the storage bucket or container name.
     *
     * @return The bucket or container name
     */
    String getBucketName();

    /**
     * Gets the environment prefix used for object keys.
     *
     * @return The environment prefix
     */
    String getEnvironmentPrefix();

    /**
     * Check if the storage client is healthy and accessible.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Represents a storage object with content and metadata.
     */
    class StorageObject {
        private byte[] content;
        private ObjectMetadata metadata;
        
        public StorageObject(byte[] content, ObjectMetadata metadata) {
            this.content = content;
            this.metadata = metadata;
        }
        
        public byte[] getContent() { return content; }
        public void setContent(byte[] content) { this.content = content; }
        
        public ObjectMetadata getMetadata() { return metadata; }
        public void setMetadata(ObjectMetadata metadata) { this.metadata = metadata; }
        
        public String getContentAsString() {
            return content != null ? new String(content) : null;
        }
    }

    /**
     * Represents object metadata.
     */
    class ObjectMetadata {
        private long contentLength;
        private String contentType;
        private String etag;
        private Instant lastModified;
        private Map<String, String> userMetadata;
        private String storageClass;
        private String serverSideEncryption;

        public long getContentLength() { return contentLength; }
        public void setContentLength(long contentLength) { this.contentLength = contentLength; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getEtag() { return etag; }
        public void setEtag(String etag) { this.etag = etag; }

        public Instant getLastModified() { return lastModified; }
        public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }

        public Map<String, String> getUserMetadata() { return userMetadata; }
        public void setUserMetadata(Map<String, String> userMetadata) { this.userMetadata = userMetadata; }

        public String getStorageClass() { return storageClass; }
        public void setStorageClass(String storageClass) { this.storageClass = storageClass; }

        public String getServerSideEncryption() { return serverSideEncryption; }
        public void setServerSideEncryption(String serverSideEncryption) { this.serverSideEncryption = serverSideEncryption; }
    }

    /**
     * Represents storage options for object operations.
     */
    class StorageOptions {
        private String storageClass;
        private String serverSideEncryption;
        private String kmsKeyId;
        private String contentType;
        private String contentEncoding;
        private String cacheControl;
        private Instant expires;

        public String getStorageClass() { return storageClass; }
        public void setStorageClass(String storageClass) { this.storageClass = storageClass; }

        public String getServerSideEncryption() { return serverSideEncryption; }
        public void setServerSideEncryption(String serverSideEncryption) { this.serverSideEncryption = serverSideEncryption; }

        public String getKmsKeyId() { return kmsKeyId; }
        public void setKmsKeyId(String kmsKeyId) { this.kmsKeyId = kmsKeyId; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getContentEncoding() { return contentEncoding; }
        public void setContentEncoding(String contentEncoding) { this.contentEncoding = contentEncoding; }

        public String getCacheControl() { return cacheControl; }
        public void setCacheControl(String cacheControl) { this.cacheControl = cacheControl; }

        public Instant getExpires() { return expires; }
        public void setExpires(Instant expires) { this.expires = expires; }
    }

    /**
     * Paginated object list result.
     */
    class PaginatedObjectList {
        private final List<ObjectMetadata> objects;
        private final String continuationToken;
        private final boolean truncated;

        public PaginatedObjectList(List<ObjectMetadata> objects, String continuationToken, boolean truncated) {
            this.objects = objects;
            this.continuationToken = continuationToken;
            this.truncated = truncated;
        }

        public List<ObjectMetadata> getObjects() {
            return objects;
        }

        public String getContinuationToken() {
            return continuationToken;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public int size() {
            return objects.size();
        }
    }

    /**
     * Represents a summary of an object in storage.
     */
    class ObjectSummary {
        private String key;
        private long size;
        private Instant lastModified;
        private String etag;
        private String storageClass;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public Instant getLastModified() { return lastModified; }
        public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }

        public String getEtag() { return etag; }
        public void setEtag(String etag) { this.etag = etag; }

        public String getStorageClass() { return storageClass; }
        public void setStorageClass(String storageClass) { this.storageClass = storageClass; }
    }

    /**
     * Represents a paginated list result.
     */
    class ListResult {
        private List<ObjectSummary> objects;
        private String nextContinuationToken;
        private boolean truncated;
        private String prefix;

        public ListResult(List<ObjectSummary> objects, String nextContinuationToken, boolean truncated, String prefix) {
            this.objects = objects;
            this.nextContinuationToken = nextContinuationToken;
            this.truncated = truncated;
            this.prefix = prefix;
        }

        public List<ObjectSummary> getObjects() { return objects; }
        public String getNextContinuationToken() { return nextContinuationToken; }
        public boolean isTruncated() { return truncated; }
        public String getPrefix() { return prefix; }
    }

    /**
     * Represents the result of a multipart upload.
     */
    class UploadResult {
        private String etag;
        private String location;
        private String bucketName;
        private String key;

        public UploadResult(String etag, String location, String bucketName, String key) {
            this.etag = etag;
            this.location = location;
            this.bucketName = bucketName;
            this.key = key;
        }

        public String getEtag() { return etag; }
        public String getLocation() { return location; }
        public String getBucketName() { return bucketName; }
        public String getKey() { return key; }
    }

    /**
     * Represents the result of an object deletion.
     */
    class DeletionResult {
        private String key;
        private boolean successful;
        private String errorCode;
        private String errorMessage;

        public DeletionResult(String key, boolean successful) {
            this.key = key;
            this.successful = successful;
        }

        public DeletionResult(String key, String errorCode, String errorMessage) {
            this.key = key;
            this.successful = false;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getKey() { return key; }
        public boolean isSuccessful() { return successful; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Storage operation types for pre-signed URLs.
     */
    enum Operation {
        GET, PUT, DELETE, HEAD
    }

    /**
     * Object lifecycle policy.
     */
    class LifecyclePolicy {
        private final int deleteAfterDays;
        private final int transitionToIAAfterDays;
        private final int transitionToGlacierAfterDays;

        public LifecyclePolicy(int deleteAfterDays, int transitionToIAAfterDays, int transitionToGlacierAfterDays) {
            this.deleteAfterDays = deleteAfterDays;
            this.transitionToIAAfterDays = transitionToIAAfterDays;
            this.transitionToGlacierAfterDays = transitionToGlacierAfterDays;
        }

        public int getDeleteAfterDays() {
            return deleteAfterDays;
        }

        public int getTransitionToIAAfterDays() {
            return transitionToIAAfterDays;
        }

        public int getTransitionToGlacierAfterDays() {
            return transitionToGlacierAfterDays;
        }
    }

    /**
     * Storage statistics.
     */
    class StorageStatistics {
        private final long totalObjects;
        private final long totalSize;
        private final long lastUpdated;

        public StorageStatistics(long totalObjects, long totalSize, long lastUpdated) {
            this.totalObjects = totalObjects;
            this.totalSize = totalSize;
            this.lastUpdated = lastUpdated;
        }

        public long getTotalObjects() {
            return totalObjects;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public long getLastUpdated() {
            return lastUpdated;
        }
    }

    /**
     * Represents a lifecycle rule for automatic object management.
     */
    class LifecycleRule {
        private String id;
        private String prefix;
        private boolean enabled;
        private int expirationDays;
        private String storageClassTransition;
        private int transitionDays;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getExpirationDays() { return expirationDays; }
        public void setExpirationDays(int expirationDays) { this.expirationDays = expirationDays; }

        public String getStorageClassTransition() { return storageClassTransition; }
        public void setStorageClassTransition(String storageClassTransition) { this.storageClassTransition = storageClassTransition; }

        public int getTransitionDays() { return transitionDays; }
        public void setTransitionDays(int transitionDays) { this.transitionDays = transitionDays; }
    }
}