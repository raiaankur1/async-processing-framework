package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Storage Client interface for async request payload handling.
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
     * Put an object with metadata.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param content the content to store
     * @param metadata object metadata
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, String content, Map<String, String> metadata);

    /**
     * Put an object with content type and metadata.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param content the content to store
     * @param contentType the MIME content type
     * @param metadata object metadata
     * @throws AsyncFrameworkException if storage fails
     */
    void putObject(String id, String key, byte[] content, String contentType, Map<String, String> metadata);

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
     * @return true if object was deleted, false if it didn't exist
     * @throws AsyncFrameworkException if deletion fails
     */
    boolean deleteObject(String id, String key);

    /**
     * List objects with a given prefix.
     *
     * @param id the container/bucket identifier
     * @param prefix the key prefix to filter by
     * @return list of object metadata
     * @throws AsyncFrameworkException if listing fails
     */
    List<ObjectMetadata> listObjects(String id, String prefix);

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
    PaginatedObjectList listObjects(String id, String prefix, int maxKeys, String continuationToken);

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
     * @param expirationSeconds URL expiration time in seconds
     * @param operation the operation type (GET, PUT, DELETE)
     * @return pre-signed URL
     * @throws AsyncFrameworkException if URL generation fails
     */
    String generatePresignedUrl(String id, String key, int expirationSeconds, Operation operation);

    /**
     * Batch delete multiple objects.
     *
     * @param id the container/bucket identifier
     * @param keys list of object keys to delete
     * @return list of successfully deleted keys
     * @throws AsyncFrameworkException if batch delete fails
     */
    List<String> deleteObjects(String id, List<String> keys);

    /**
     * Set object lifecycle policy.
     *
     * @param id the container/bucket identifier
     * @param key the object key
     * @param lifecyclePolicy the lifecycle policy
     * @throws AsyncFrameworkException if policy setting fails
     */
    void setObjectLifecycle(String id, String key, LifecyclePolicy lifecyclePolicy);

    /**
     * Get storage statistics for a container.
     *
     * @param id the container/bucket identifier
     * @return storage statistics
     * @throws AsyncFrameworkException if statistics retrieval fails
     */
    StorageStatistics getStorageStatistics(String id);

    /**
     * Check if the storage client is healthy and accessible.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Get the storage type (e.g., "S3", "Azure Blob", "GCS").
     *
     * @return storage type
     */
    String getStorageType();

    /**
     * Clean up old objects based on age.
     *
     * @param id the container/bucket identifier
     * @param olderThanDays delete objects older than this many days
     * @return number of deleted objects
     * @throws AsyncFrameworkException if cleanup fails
     */
    int cleanupOldObjects(String id, int olderThanDays);

    /**
     * Object metadata information.
     */
    class ObjectMetadata {
        private final String key;
        private final long size;
        private final long lastModified;
        private final String etag;
        private final String contentType;
        private final Map<String, String> userMetadata;

        public ObjectMetadata(String key, long size, long lastModified, String etag,
                              String contentType, Map<String, String> userMetadata) {
            this.key = key;
            this.size = size;
            this.lastModified = lastModified;
            this.etag = etag;
            this.contentType = contentType;
            this.userMetadata = userMetadata;
        }

        public String getKey() {
            return key;
        }

        public long getSize() {
            return size;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getEtag() {
            return etag;
        }

        public String getContentType() {
            return contentType;
        }

        public Map<String, String> getUserMetadata() {
            return userMetadata;
        }

        public String getUserMetadata(String key) {
            return userMetadata != null ? userMetadata.get(key) : null;
        }
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
}