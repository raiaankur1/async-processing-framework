package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.model.AsyncRequestAPI;
import com.pravah.framework.async.model.AsyncRequestStatus;

import java.util.List;
import java.util.Optional;

/**
 * Enhanced Data Access Object interface for async request persistence.
 * <br>
 * This interface provides comprehensive CRUD operations for async requests
 * with support for status tracking, querying, and batch operations.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public interface AsyncDAO {

    /**
     * Create a new async request in the data store.
     *
     * @param request the async request to create
     * @throws AsyncFrameworkException if creation fails
     */
    void createRequest(AsyncRequest request);

    /**
     * Update the success status for a specific API in the request.
     *
     * @param requestId the request ID
     * @param api the API that completed successfully
     * @throws AsyncFrameworkException if update fails
     */
    void updateSuccess(String requestId, AsyncRequestAPI api);

    /**
     * Update the overall status of a request.
     *
     * @param requestId the request ID
     * @param status the new status
     * @throws AsyncFrameworkException if update fails
     */
    void updateStatus(String requestId, AsyncRequestStatus status);

    /**
     * Update the error information for a request.
     *
     * @param requestId the request ID
     * @param errorMessage the error message
     * @param errorCode the error code
     * @throws AsyncFrameworkException if update fails
     */
    void updateError(String requestId, String errorMessage, String errorCode);

    /**
     * Increment the retry count for a request.
     *
     * @param requestId the request ID
     * @return the new retry count
     * @throws AsyncFrameworkException if update fails
     */
    int incrementRetryCount(String requestId);

    /**
     * Get an async request by ID.
     *
     * @param requestId the request ID
     * @return the async request, or empty if not found
     * @throws AsyncFrameworkException if retrieval fails
     */
    Optional<AsyncRequest> getAsyncRequest(String requestId);

    /**
     * Get async requests by status.
     *
     * @param status the status to filter by
     * @return list of requests with the specified status
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByStatus(AsyncRequestStatus status);

    /**
     * Get async requests by status with pagination.
     *
     * @param status the status to filter by
     * @param limit maximum number of results
     * @param lastEvaluatedKey pagination token (null for first page)
     * @return paginated list of requests
     * @throws AsyncFrameworkException if query fails
     */
    PaginatedResult<AsyncRequest> getRequestsByStatus(AsyncRequestStatus status, int limit, String lastEvaluatedKey);

    /**
     * Get async requests by application ID.
     *
     * @param appId the application ID
     * @return list of requests for the application
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByAppId(String appId);

    /**
     * Get async requests by type and status.
     *
     * @param type the request type
     * @param status the status to filter by
     * @return list of matching requests
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByTypeAndStatus(String type, AsyncRequestStatus status);

    /**
     * Get requests that are stuck (processing for too long).
     *
     * @param timeoutMinutes requests processing longer than this are considered stuck
     * @return list of stuck requests
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getStuckRequests(int timeoutMinutes);

    /**
     * Get requests eligible for retry.
     *
     * @return list of requests that can be retried
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRetryableRequests();

    /**
     * Delete an async request.
     *
     * @param requestId the request ID to delete
     * @return true if deleted, false if not found
     * @throws AsyncFrameworkException if deletion fails
     */
    boolean deleteRequest(String requestId);

    /**
     * Batch create multiple requests.
     *
     * @param requests list of requests to create
     * @return list of successfully created request IDs
     * @throws AsyncFrameworkException if batch operation fails
     */
    List<String> batchCreateRequests(List<AsyncRequest> requests);

    /**
     * Batch update request statuses.
     *
     * @param updates map of request ID to new status
     * @return number of successfully updated requests
     * @throws AsyncFrameworkException if batch operation fails
     */
    int batchUpdateStatus(java.util.Map<String, AsyncRequestStatus> updates);

    /**
     * Get request statistics by status.
     *
     * @return map of status to count
     * @throws AsyncFrameworkException if query fails
     */
    java.util.Map<AsyncRequestStatus, Long> getRequestStatistics();

    /**
     * Clean up old completed requests.
     *
     * @param olderThanDays delete requests completed more than this many days ago
     * @return number of deleted requests
     * @throws AsyncFrameworkException if cleanup fails
     */
    int cleanupOldRequests(int olderThanDays);

    /**
     * Check if the DAO is healthy and can perform operations.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Get the underlying data store type (e.g., "DynamoDB", "MongoDB").
     *
     * @return data store type
     */
    String getDataStoreType();

    /**
     * Result wrapper for paginated queries.
     *
     * @param <T> the type of items in the result
     */
    class PaginatedResult<T> {
        private final List<T> items;
        private final String lastEvaluatedKey;
        private final boolean hasMore;

        public PaginatedResult(List<T> items, String lastEvaluatedKey, boolean hasMore) {
            this.items = items;
            this.lastEvaluatedKey = lastEvaluatedKey;
            this.hasMore = hasMore;
        }

        public List<T> getItems() {
            return items;
        }

        public String getLastEvaluatedKey() {
            return lastEvaluatedKey;
        }

        public boolean hasMore() {
            return hasMore;
        }

        public int size() {
            return items.size();
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
}