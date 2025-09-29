package com.pravah.framework.async.aws;

import com.pravah.framework.async.api.StorageClient;
import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.pravah.framework.async.api.StorageClient.Operation.GET;
import static com.pravah.framework.async.api.StorageClient.Operation.PUT;

/**
 * S3 implementation of StorageClient with streaming support,
 * multipart upload capabilities, encryption support, lifecycle policy
 * integration, and comprehensive error handling.
 *
 * @author Ankur Rai
 * @version 1.0
 */
public class S3StorageClient implements StorageClient {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageClient.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String environmentPrefix;

    // S3 limits and defaults
    private static final long MIN_MULTIPART_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_SINGLE_PUT_SIZE = 5L * 1024 * 1024 * 1024; // 5 GB
    private static final int MAX_DELETE_BATCH_SIZE = 1000;
    private static final int MAX_LIST_OBJECTS_SIZE = 1000;

    public S3StorageClient(S3Client s3Client, String bucketName, String environmentPrefix) {
        this.s3Client = s3Client;
        this.s3Presigner = S3Presigner.create();
        this.bucketName = bucketName;
        this.environmentPrefix = environmentPrefix != null ? environmentPrefix : "";
    }

    @Override
    public String getObject(String id, String key) {
        byte[] content = getObjectAsBytes(id, key);
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getObjectAsBytes(String id, String key) {
        try {
            String fullKey = buildFullKey(id, key);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);

            logger.debug("Retrieved object: bucket={}, key={}, size={}",
                    bucketName, fullKey, responseBytes.asByteArray().length);

            return responseBytes.asByteArray();

        } catch (NoSuchKeyException e) {
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Object not found: " + key,
                    e);
        } catch (Exception e) {
            logger.error("Failed to retrieve object: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to retrieve object from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public InputStream getObjectAsStream(String id, String key) {
        try {
            String fullKey = buildFullKey(id, key);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            InputStream inputStream = s3Client.getObject(getRequest, ResponseTransformer.toInputStream());

            logger.debug("Retrieved object as stream: bucket={}, key={}", bucketName, fullKey);
            return inputStream;

        } catch (NoSuchKeyException e) {
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Object not found: " + key,
                    e);
        } catch (Exception e) {
            logger.error("Failed to retrieve object as stream: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to retrieve object stream from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public StorageObject getObjectWithMetadata(String id, String key) {
        try {
            byte[] content = getObjectAsBytes(id, key);
            ObjectMetadata metadata = getObjectMetadata(id, key);

            return new StorageObject(content, metadata);

        } catch (Exception e) {
            logger.error("Failed to retrieve object with metadata: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to retrieve object with metadata from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void putObject(String id, String key, String content) {
        putObject(id, key, content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void putObject(String id, String key, byte[] content) {
        putObject(id, key, content, null, null);
    }

    @Override
    public void putObject(String id, String key, InputStream inputStream, long contentLength) {
        try {
            String fullKey = buildFullKey(id, key);

            if (contentLength > MAX_SINGLE_PUT_SIZE) {
                // Use multipart upload for large objects
                putObjectMultipart(id, key, inputStream, contentLength, MIN_MULTIPART_SIZE);
                return;
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

            logger.debug("Stored object from stream: bucket={}, key={}, size={}",
                    bucketName, fullKey, contentLength);

        } catch (Exception e) {
            logger.error("Failed to store object from stream: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to store object stream to S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void putObject(String id, String key, byte[] content, Map<String, String> metadata, StorageOptions options) {
        try {
            String fullKey = buildFullKey(id, key);

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .contentLength((long) content.length);

            // Add metadata
            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }

            // Add storage options
            if (options != null) {
                if (options.getStorageClass() != null) {
                    requestBuilder.storageClass(StorageClass.fromValue(options.getStorageClass()));
                }
                if (options.getServerSideEncryption() != null) {
                    requestBuilder
                            .serverSideEncryption(ServerSideEncryption.fromValue(options.getServerSideEncryption()));
                }
                if (options.getKmsKeyId() != null) {
                    requestBuilder.ssekmsKeyId(options.getKmsKeyId());
                }
                if (options.getContentType() != null) {
                    requestBuilder.contentType(options.getContentType());
                }
                if (options.getContentEncoding() != null) {
                    requestBuilder.contentEncoding(options.getContentEncoding());
                }
                if (options.getCacheControl() != null) {
                    requestBuilder.cacheControl(options.getCacheControl());
                }
                if (options.getExpires() != null) {
                    requestBuilder.expires(options.getExpires());
                }
            }

            s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(content));

            logger.debug("Stored object with options: bucket={}, key={}, size={}",
                    bucketName, fullKey, content.length);

        } catch (Exception e) {
            logger.error("Failed to store object with options: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to store object with options to S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public UploadResult putObjectMultipart(String id, String key, InputStream inputStream, long contentLength,
            long partSize) {
        try {
            String fullKey = buildFullKey(id, key);

            if (partSize < MIN_MULTIPART_SIZE) {
                partSize = MIN_MULTIPART_SIZE;
            }

            // Initiate multipart upload
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            String uploadId = createResponse.uploadId();

            List<CompletedPart> completedParts = new ArrayList<>();

            try {
                byte[] buffer = new byte[(int) partSize];
                int partNumber = 1;
                long totalBytesRead = 0;

                while (totalBytesRead < contentLength) {
                    long remainingBytes = contentLength - totalBytesRead;
                    int currentPartSize = (int) Math.min(partSize, remainingBytes);

                    int bytesRead = inputStream.read(buffer, 0, currentPartSize);
                    if (bytesRead == -1)
                        break;

                    // Upload part
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(fullKey)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .contentLength((long) bytesRead)
                            .build();

                    UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                            uploadPartRequest,
                            RequestBody.fromBytes(Arrays.copyOf(buffer, bytesRead)));

                    completedParts.add(CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(uploadPartResponse.eTag())
                            .build());

                    totalBytesRead += bytesRead;
                    partNumber++;

                    logger.debug("Uploaded part {}: {} bytes", partNumber - 1, bytesRead);
                }

                // Complete multipart upload
                CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build();

                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fullKey)
                        .uploadId(uploadId)
                        .multipartUpload(completedUpload)
                        .build();

                CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(completeRequest);

                logger.info("Completed multipart upload: bucket={}, key={}, parts={}, totalSize={}",
                        bucketName, fullKey, completedParts.size(), totalBytesRead);

                return new UploadResult(
                        completeResponse.eTag(),
                        completeResponse.location(),
                        bucketName,
                        fullKey);

            } catch (Exception e) {
                // Abort multipart upload on failure
                try {
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(fullKey)
                            .uploadId(uploadId)
                            .build();
                    s3Client.abortMultipartUpload(abortRequest);
                    logger.warn("Aborted multipart upload due to error: uploadId={}", uploadId);
                } catch (Exception abortException) {
                    logger.error("Failed to abort multipart upload: uploadId={}", uploadId, abortException);
                }
                throw e;
            }

        } catch (Exception e) {
            logger.error("Failed multipart upload: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed multipart upload to S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean objectExists(String id, String key) {
        try {
            String fullKey = buildFullKey(id, key);

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Failed to check object existence: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to check object existence in S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public ObjectMetadata getObjectMetadata(String id, String key) {
        try {
            String fullKey = buildFullKey(id, key);

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(response.contentLength());
            metadata.setContentType(response.contentType());
            metadata.setEtag(response.eTag());
            metadata.setLastModified(response.lastModified());
            metadata.setUserMetadata(response.metadata());
            metadata.setStorageClass(response.storageClassAsString());
            metadata.setServerSideEncryption(response.serverSideEncryptionAsString());

            return metadata;

        } catch (NoSuchKeyException e) {
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Object not found: " + key,
                    e);
        } catch (Exception e) {
            logger.error("Failed to get object metadata: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to get object metadata from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void deleteObject(String id, String key) {
        try {
            String fullKey = buildFullKey(id, key);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .build();

            s3Client.deleteObject(deleteRequest);

            logger.debug("Deleted object: bucket={}, key={}", bucketName, fullKey);

        } catch (Exception e) {
            logger.error("Failed to delete object: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to delete object from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public List<DeletionResult> deleteObjects(String id, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<DeletionResult> results = new ArrayList<>();

            // Split into batches of 1000 (S3 limit)
            List<List<String>> batches = partitionList(keys, MAX_DELETE_BATCH_SIZE);

            for (List<String> batch : batches) {
                results.addAll(deleteObjectBatch(id, batch));
            }

            logger.debug("Deleted {} objects in {} batches", keys.size(), batches.size());
            return results;

        } catch (Exception e) {
            logger.error("Failed to delete objects batch: bucket={}, id={}", bucketName, id, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to delete objects batch from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public List<ObjectSummary> listObjects(String id, String prefix) {
        try {
            String fullPrefix = buildFullKey(id, prefix);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(fullPrefix)
                    .maxKeys(MAX_LIST_OBJECTS_SIZE)
                    .build();

            List<ObjectSummary> summaries = new ArrayList<>();
            ListObjectsV2Response response;

            do {
                response = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : response.contents()) {
                    ObjectSummary summary = new ObjectSummary();
                    summary.setKey(s3Object.key());
                    summary.setSize(s3Object.size());
                    summary.setLastModified(s3Object.lastModified());
                    summary.setEtag(s3Object.eTag());
                    summary.setStorageClass(s3Object.storageClassAsString());
                    summaries.add(summary);
                }

                listRequest = listRequest.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();

            } while (response.isTruncated());

            logger.debug("Listed {} objects with prefix: bucket={}, prefix={}", summaries.size(), bucketName,
                    fullPrefix);
            return summaries;

        } catch (Exception e) {
            logger.error("Failed to list objects: bucket={}, id={}, prefix={}", bucketName, id, prefix, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to list objects from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public ListResult listObjects(String id, String prefix, int maxKeys, String continuationToken) {
        try {
            String fullPrefix = buildFullKey(id, prefix);

            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(fullPrefix)
                    .maxKeys(Math.min(maxKeys, MAX_LIST_OBJECTS_SIZE));

            if (continuationToken != null && !continuationToken.trim().isEmpty()) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<ObjectSummary> summaries = response.contents().stream()
                    .map(s3Object -> {
                        ObjectSummary summary = new ObjectSummary();
                        summary.setKey(s3Object.key());
                        summary.setSize(s3Object.size());
                        summary.setLastModified(s3Object.lastModified());
                        summary.setEtag(s3Object.eTag());
                        summary.setStorageClass(s3Object.storageClassAsString());
                        return summary;
                    })
                    .collect(Collectors.toList());

            logger.debug("Listed {} objects (paginated): bucket={}, prefix={}", summaries.size(), bucketName,
                    fullPrefix);

            return new ListResult(
                    summaries,
                    response.nextContinuationToken(),
                    response.isTruncated(),
                    fullPrefix);

        } catch (Exception e) {
            logger.error("Failed to list objects (paginated): bucket={}, id={}, prefix={}", bucketName, id, prefix, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to list objects from S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void copyObject(String sourceId, String sourceKey, String destinationId, String destinationKey) {
        try {
            String sourceFullKey = buildFullKey(sourceId, sourceKey);
            String destinationFullKey = buildFullKey(destinationId, destinationKey);

            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceFullKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationFullKey)
                    .build();

            s3Client.copyObject(copyRequest);

            logger.debug("Copied object: from={} to={}", sourceFullKey, destinationFullKey);

        } catch (Exception e) {
            logger.error("Failed to copy object: from={}:{} to={}:{}", sourceId, sourceKey, destinationId,
                    destinationKey, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    sourceId,
                    "Failed to copy object in S3: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public String generatePresignedUrl(String id, String key, Instant expiration, Operation operation) {
        try {
            String fullKey = buildFullKey(id, key);
            Duration duration = Duration.between(Instant.now(), expiration);

            if (GET.equals(operation)) {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fullKey)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(getRequest)
                        .build();

                return s3Presigner.presignGetObject(presignRequest).url().toString();

            } else if (PUT.equals(operation)) {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fullKey)
                        .build();

                PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .putObjectRequest(putRequest)
                        .build();

                return s3Presigner.presignPutObject(presignRequest).url().toString();

            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + operation);
            }

        } catch (Exception e) {
            logger.error("Failed to generate presigned URL: bucket={}, id={}, key={}", bucketName, id, key, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to generate presigned URL: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void setLifecyclePolicy(String id, List<LifecycleRule> lifecycleRules) {
        try {
            List<software.amazon.awssdk.services.s3.model.LifecycleRule> s3Rules = lifecycleRules.stream()
                    .map(this::convertToS3LifecycleRule)
                    .collect(Collectors.toList());

            BucketLifecycleConfiguration lifecycleConfig = BucketLifecycleConfiguration.builder()
                    .rules(s3Rules)
                    .build();

            PutBucketLifecycleConfigurationRequest lifecycleRequest = PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(lifecycleConfig)
                    .build();

            s3Client.putBucketLifecycleConfiguration(lifecycleRequest);

            logger.debug("Set lifecycle policy: bucket={}, rules={}", bucketName, lifecycleRules.size());

        } catch (Exception e) {
            logger.error("Failed to set lifecycle policy: bucket={}, id={}", bucketName, id, e);
            throw new AsyncFrameworkException(
                    ErrorCode.STORAGE_ERROR,
                    id,
                    "Failed to set lifecycle policy: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getEnvironmentPrefix() {
        return environmentPrefix;
    }

    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple head bucket operation to check connectivity
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headRequest);
            return true;

        } catch (Exception e) {
            logger.warn("S3 storage health check failed for bucket: {}", bucketName, e);
            return false;
        }
    }

    // Helper methods

    private String buildFullKey(String id, String key) {
        StringBuilder keyBuilder = new StringBuilder();

        if (environmentPrefix != null && !environmentPrefix.isEmpty()) {
            keyBuilder.append(environmentPrefix);
            if (!environmentPrefix.endsWith("/")) {
                keyBuilder.append("/");
            }
        }

        keyBuilder.append(id);

        if (key != null && !key.isEmpty()) {
            if (!key.startsWith("/")) {
                keyBuilder.append("/");
            }
            keyBuilder.append(key);
        }

        return keyBuilder.toString();
    }

    private List<DeletionResult> deleteObjectBatch(String id, List<String> keys) {
        List<ObjectIdentifier> objectIdentifiers = keys.stream()
                .map(key -> ObjectIdentifier.builder()
                        .key(buildFullKey(id, key))
                        .build())
                .collect(Collectors.toList());

        Delete delete = Delete.builder()
                .objects(objectIdentifiers)
                .quiet(false)
                .build();

        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(delete)
                .build();

        DeleteObjectsResponse response = s3Client.deleteObjects(deleteRequest);

        List<DeletionResult> results = new ArrayList<>();

        // Add successful deletions
        for (DeletedObject deleted : response.deleted()) {
            results.add(new DeletionResult(deleted.key(), true));
        }

        // Add failed deletions
        for (S3Error error : response.errors()) {
            results.add(new DeletionResult(error.key(), error.code(), error.message()));
        }

        return results;
    }

    private software.amazon.awssdk.services.s3.model.LifecycleRule convertToS3LifecycleRule(LifecycleRule rule) {
        software.amazon.awssdk.services.s3.model.LifecycleRule.Builder s3RuleBuilder = software.amazon.awssdk.services.s3.model.LifecycleRule
                .builder()
                .id(rule.getId())
                .status(rule.isEnabled() ? ExpirationStatus.ENABLED : ExpirationStatus.DISABLED);

        // Set filter
        if (rule.getPrefix() != null && !rule.getPrefix().isEmpty()) {
            s3RuleBuilder.filter(LifecycleRuleFilter.builder()
                    .prefix(rule.getPrefix())
                    .build());
        }

        // Set expiration
        if (rule.getExpirationDays() > 0) {
            s3RuleBuilder.expiration(LifecycleExpiration.builder()
                    .days(rule.getExpirationDays())
                    .build());
        }

        // Set transition
        if (rule.getStorageClassTransition() != null && rule.getTransitionDays() > 0) {
            s3RuleBuilder.transitions(Transition.builder()
                    .days(rule.getTransitionDays())
                    .storageClass(rule.getStorageClassTransition())
                    .build());
        }

        return s3RuleBuilder.build();
    }

    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }
}