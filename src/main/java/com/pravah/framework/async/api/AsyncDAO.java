package com.pravah.framework.async.api;

import com.pravah.framework.async.exception.AsyncFrameworkException;
import com.pravah.framework.async.model.AsyncRequest;
import com.pravah.framework.async.model.AsyncRequestAPI;
import com.pravah.framework.async.model.AsyncRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object interface for async request persistence.
 * <br>
 * Provides comprehensive CRUD operations for async requests
 * with batch support, status filtering, and TTL management for automatic cleanup.
 *
 * @author Ankur Rai
 * @version 2.0
 */
public interface AsyncDAO {

    /**
     * Creates a new async request in the data store.
     *
     * @param request The async request to create
     * @throws AsyncFrameworkException if creation fails
     */
    void createRequest(AsyncRequest request);

    /**
     * Creates multiple async requests in a single batch operation.
     *
     * @param requests List of async requests to create
     * @throws AsyncFrameworkException if batch creation fails
     */
    void createRequests(List<AsyncRequest> requests);

    /**
     * Retrieves an async request by its ID.
     *
     * @param requestId The unique identifier of the request
     * @return Optional containing the request if found, empty otherwise
     * @throws AsyncFrameworkException if retrieval fails
     */
    Optional<AsyncRequest> getAsyncRequest(String requestId);

    /**
     * Retrieves multiple async requests by their IDs in a single batch operation.
     *
     * @param requestIds List of request IDs to retrieve
     * @return Map of request ID to AsyncRequest for found requests
     * @throws AsyncFrameworkException if batch retrieval fails
     */
    Map<String, AsyncRequest> getAsyncRequests(List<String> requestIds);

    /**
     * Update the success status for a specific API in the request.
     * @param requestId the request ID
     * @param api the API that completed successfully
     * @throws AsyncFrameworkException if update fails
     */
    void updateSuccess(String requestId, AsyncRequestAPI api);

    /**
     * Updates the status of a specific API within an async request.
     *
     * @param requestId The unique identifier of the request
     * @param api The API whose status should be updated
     * @param status The new status for the API
     * @throws AsyncFrameworkException if update fails
     */
    void updateAPIStatus(String requestId, AsyncRequestAPI api, AsyncRequestStatus status);

    /**
     * Updates the overall status of an async request.
     *
     * @param requestId The unique identifier of the request
     * @param status The new overall status
     * @throws AsyncFrameworkException if update fails
     */
    void updateStatus(String requestId, AsyncRequestStatus status);

    /**
     * Updates the overall status and last processed timestamp of an async request.
     *
     * @param requestId The unique identifier of the request
     * @param status The new overall status
     * @param lastProcessedAt The timestamp when the request was last processed
     * @throws AsyncFrameworkException if update fails
     */
    void updateStatus(String requestId, AsyncRequestStatus status, Instant lastProcessedAt);

    /**
     * Increments the retry count for an async request.
     *
     * @param requestId The unique identifier of the request
     * @return The new retry count after increment
     * @throws AsyncFrameworkException if update fails
     */
    int incrementRetryCount(String requestId);

    /**
     * Updates the error message for an async request.
     *
     * @param requestId The unique identifier of the request
     * @param errorMessage The error message to store
     * @throws AsyncFrameworkException if update fails
     */
    void updateErrorMessage(String requestId, String errorMessage);

    /**
     * Retrieves async requests by their overall status.
     * Uses Global Secondary Index for efficient querying.
     *
     * @param status The status to filter by
     * @return List of requests with the specified status
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByStatus(AsyncRequestStatus status);

    /**
     * Retrieves async requests by status with pagination support.
     *
     * @param status The status to filter by
     * @param limit Maximum number of requests to return
     * @param lastEvaluatedKey The last evaluated key from previous query (for pagination)
     * @return Paginated result containing requests and next page token
     * @throws AsyncFrameworkException if query fails
     */
    PaginatedResult<AsyncRequest> getRequestsByStatus(AsyncRequestStatus status, int limit, String lastEvaluatedKey);

    /**
     * Retrieves async requests by status within a time range.
     *
     * @param status The status to filter by
     * @param fromTime Start of time range (inclusive)
     * @param toTime End of time range (exclusive)
     * @return List of requests matching the criteria
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByStatusAndTimeRange(AsyncRequestStatus status, Instant fromTime, Instant toTime);

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
     * Retrieves async requests by application ID.
     *
     * @param appId The application ID to filter by
     * @return List of requests for the specified application
     * @throws AsyncFrameworkException if query fails
     */
    List<AsyncRequest> getRequestsByAppId(String appId);

    /**
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
     * Deletes an async request by its ID.
     *
     * @param requestId The unique identifier of the request to delete
     * @return true if deleted, false if not found
     * @throws AsyncFrameworkException if deletion fails
     */
    void deleteRequest(String requestId);

    /**
     * Deletes multiple async requests in a single batch operation.
     *
     * @param requestIds List of request IDs to delete
     * @throws AsyncFrameworkException if batch deletion fails
     */
    void deleteRequests(List<String> requestIds);

    /**
     * Batch update request statuses.
     *
     * @param updates map of request ID to new status
     * @return number of successfully updated requests
     * @throws AsyncFrameworkException if batch operation fails
     */
    int batchUpdateStatus(Map<String, AsyncRequestStatus> updates);

    /**
     * Get request statistics by status.
     *
     * @return map of status to count
     * @throws AsyncFrameworkException if query fails
     */
    Map<AsyncRequestStatus, Long> getRequestStatistics();

    /**
     * Clean up old completed requests.
     *
     * @param olderThanDays delete requests completed more than this many days ago
     * @return number of deleted requests
     * @throws AsyncFrameworkException if cleanup fails
     */
    int cleanupOldRequests(int olderThanDays);

    /**
     * Sets TTL (Time To Live) for an async request for automatic cleanup.
     *
     * @param requestId The unique identifier of the request
     * @param ttlTimestamp The timestamp when the request should be automatically deleted
     * @throws AsyncFrameworkException if TTL update fails
     */
    void setTTL(String requestId, Instant ttlTimestamp);

    /**
     * Checks if the underlying data store is healthy and accessible.
     *
     * @return true if the data store is healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Represents a paginated result from a query operation.
     *
     * @param <T> The type of items in the result
     */
    class PaginatedResult<T> {
        private final List<T> items;
        private final String nextPageToken;
        private final boolean hasMoreResults;

        public PaginatedResult(List<T> items, String nextPageToken, boolean hasMoreResults) {
            this.items = items;
            this.nextPageToken = nextPageToken;
            this.hasMoreResults = hasMoreResults;
        }

        public List<T> getItems() {
            return items;
        }

        public String getNextPageToken() {
            return nextPageToken;
        }

        public boolean hasMoreResults() {
            return hasMoreResults;
        }

        public int size() {
            return items.size();
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

    }
}