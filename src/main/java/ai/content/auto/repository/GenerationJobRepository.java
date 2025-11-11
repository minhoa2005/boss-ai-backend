package ai.content.auto.repository;

import ai.content.auto.entity.GenerationJob;
import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for GenerationJob entity with optimized queries for queue
 * operations
 */
@Repository
public interface GenerationJobRepository
                extends JpaRepository<GenerationJob, Long>, JpaSpecificationExecutor<GenerationJob> {

        /**
         * Find job by unique job ID
         */
        Optional<GenerationJob> findByJobId(String jobId);

        /**
         * Find jobs by user ID with pagination
         */
        Page<GenerationJob> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        /**
         * Find jobs by status
         */
        List<GenerationJob> findByStatusOrderByPriorityAscCreatedAtAsc(JobStatus status);

        /**
         * Find next jobs to process with priority ordering
         * Uses composite index for optimal performance
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
                        "ORDER BY j.priority ASC, j.createdAt ASC")
        List<GenerationJob> findNextJobsToProcess(@Param("status") JobStatus status, Pageable pageable);

        /**
         * Find jobs ready for retry
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
                        "AND j.nextRetryAt <= :now AND j.retryCount < j.maxRetries " +
                        "ORDER BY j.priority ASC, j.nextRetryAt ASC")
        List<GenerationJob> findJobsReadyForRetry(@Param("status") JobStatus status,
                        @Param("now") Instant now,
                        Pageable pageable);

        /**
         * Find expired jobs for cleanup
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.expiresAt <= :now")
        List<GenerationJob> findExpiredJobs(@Param("now") Instant now);

        /**
         * Find timed out jobs for retry or failure handling
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
                        "AND j.startedAt <= :timeoutThreshold")
        List<GenerationJob> findTimedOutJobs(@Param("status") JobStatus status,
                        @Param("timeoutThreshold") Instant timeoutThreshold);

        /**
         * Find completed jobs older than specified time for cleanup
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.status = :status " +
                        "AND j.completedAt <= :cutoffTime")
        List<GenerationJob> findCompletedJobsOlderThan(@Param("status") JobStatus status,
                        @Param("cutoffTime") Instant cutoffTime);

        /**
         * Count jobs by status
         */
        long countByStatus(JobStatus status);

        /**
         * Count jobs by status and priority
         */
        long countByStatusAndPriority(JobStatus status, JobPriority priority);

        /**
         * Count jobs by user and status
         */
        long countByUserIdAndStatus(Long userId, JobStatus status);

        /**
         * Find jobs by user and status with pagination
         */
        Page<GenerationJob> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, JobStatus status, Pageable pageable);

        /**
         * Update job status by job ID
         */
        @Modifying
        @Transactional
        @Query("UPDATE GenerationJob j SET j.status = :status, j.updatedAt = :now " +
                        "WHERE j.jobId = :jobId")
        int updateJobStatus(@Param("jobId") String jobId,
                        @Param("status") JobStatus status,
                        @Param("now") Instant now);

        /**
         * Update job to processing status
         */
        @Modifying
        @Transactional
        @Query("UPDATE GenerationJob j SET j.status = :status, j.startedAt = :now, " +
                        "j.updatedAt = :now WHERE j.jobId = :jobId AND j.status = :currentStatus")
        int updateJobToProcessing(@Param("jobId") String jobId,
                        @Param("status") JobStatus status,
                        @Param("currentStatus") JobStatus currentStatus,
                        @Param("now") Instant now);

        /**
         * Update job to completed status with results
         */
        @Modifying
        @Transactional
        @Query("UPDATE GenerationJob j SET j.status = :status, j.completedAt = :now, " +
                        "j.resultContent = :content, j.processingTimeMs = :processingTime, " +
                        "j.tokensUsed = :tokensUsed, j.generationCost = :cost, " +
                        "j.aiProvider = :aiProvider, j.aiModel = :aiModel, " +
                        "j.openaiResponseId = :openaiResponseId, j.updatedAt = :now " +
                        "WHERE j.jobId = :jobId")
        int updateJobToCompleted(@Param("jobId") String jobId,
                        @Param("status") JobStatus status,
                        @Param("content") String content,
                        @Param("processingTime") Long processingTime,
                        @Param("tokensUsed") Integer tokensUsed,
                        @Param("cost") java.math.BigDecimal cost,
                        @Param("aiProvider") String aiProvider,
                        @Param("aiModel") String aiModel,
                        @Param("openaiResponseId") String openaiResponseId,
                        @Param("now") Instant now);

        /**
         * Update job to failed status with error details
         */
        @Modifying
        @Transactional
        @Query("UPDATE GenerationJob j SET j.status = :status, j.completedAt = :now, " +
                        "j.errorMessage = :errorMessage, j.retryCount = j.retryCount + 1, " +
                        "j.nextRetryAt = :nextRetryAt, j.updatedAt = :now " +
                        "WHERE j.jobId = :jobId")
        int updateJobToFailed(@Param("jobId") String jobId,
                        @Param("status") JobStatus status,
                        @Param("errorMessage") String errorMessage,
                        @Param("nextRetryAt") Instant nextRetryAt,
                        @Param("now") Instant now);

        /**
         * Delete jobs older than specified time
         */
        @Modifying
        @Transactional
        @Query("DELETE FROM GenerationJob j WHERE j.completedAt <= :cutoffTime " +
                        "AND j.status IN (:statuses)")
        int deleteJobsOlderThan(@Param("cutoffTime") Instant cutoffTime,
                        @Param("statuses") List<JobStatus> statuses);

        /**
         * Get queue statistics
         */
        @Query("SELECT j.status as status, j.priority as priority, COUNT(j) as count " +
                        "FROM GenerationJob j " +
                        "WHERE j.status IN (:statuses) " +
                        "GROUP BY j.status, j.priority")
        List<Object[]> getQueueStatistics(@Param("statuses") List<JobStatus> statuses);

        /**
         * Get processing statistics for monitoring
         */
        @Query("SELECT " +
                        "COUNT(CASE WHEN j.status = 'QUEUED' THEN 1 END) as queuedCount, " +
                        "COUNT(CASE WHEN j.status = 'PROCESSING' THEN 1 END) as processingCount, " +
                        "COUNT(CASE WHEN j.status = 'COMPLETED' THEN 1 END) as completedCount, " +
                        "COUNT(CASE WHEN j.status = 'FAILED' THEN 1 END) as failedCount, " +
                        "AVG(CASE WHEN j.processingTimeMs IS NOT NULL THEN j.processingTimeMs END) as avgProcessingTime "
                        +
                        "FROM GenerationJob j " +
                        "WHERE j.createdAt >= :since")
        Object[] getProcessingStatistics(@Param("since") Instant since);

        /**
         * Check if user has reached concurrent job limit
         */
        @Query("SELECT COUNT(j) FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND j.status IN ('QUEUED', 'PROCESSING')")
        long countActiveJobsByUser(@Param("userId") Long userId);

        /**
         * Find jobs by user with content type filter
         */
        Page<GenerationJob> findByUserIdAndContentTypeOrderByCreatedAtDesc(
                        Long userId, String contentType, Pageable pageable);

        /**
         * Find jobs by user with status and content type filters
         */
        Page<GenerationJob> findByUserIdAndStatusAndContentTypeOrderByCreatedAtDesc(
                        Long userId, JobStatus status, String contentType, Pageable pageable);

        /**
         * Find jobs by user within date range
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND j.createdAt >= :startDate AND j.createdAt <= :endDate " +
                        "ORDER BY j.createdAt DESC")
        Page<GenerationJob> findByUserIdAndDateRange(@Param("userId") Long userId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate,
                        Pageable pageable);

        /**
         * Search jobs by user with text search across content and error messages
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND (LOWER(j.resultContent) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                        "OR LOWER(j.errorMessage) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
                        "OR LOWER(j.contentType) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
                        "ORDER BY j.createdAt DESC")
        Page<GenerationJob> searchJobsByUser(@Param("userId") Long userId,
                        @Param("searchText") String searchText,
                        Pageable pageable);

        /**
         * Get distinct content types for a user (for filter dropdown)
         */
        @Query("SELECT DISTINCT j.contentType FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND j.contentType IS NOT NULL ORDER BY j.contentType")
        List<String> findDistinctContentTypesByUserId(@Param("userId") Long userId);

        /**
         * Get user job statistics for dashboard
         */
        @Query("SELECT " +
                        "COUNT(j) as totalJobs, " +
                        "COUNT(CASE WHEN j.status = 'COMPLETED' THEN 1 END) as completedJobs, " +
                        "COUNT(CASE WHEN j.status = 'FAILED' THEN 1 END) as failedJobs, " +
                        "COUNT(CASE WHEN j.status = 'PROCESSING' THEN 1 END) as processingJobs, " +
                        "COUNT(CASE WHEN j.status = 'QUEUED' THEN 1 END) as queuedJobs, " +
                        "COUNT(CASE WHEN j.status = 'CANCELLED' THEN 1 END) as cancelledJobs, " +
                        "AVG(CASE WHEN j.processingTimeMs IS NOT NULL THEN j.processingTimeMs END) as avgProcessingTime, "
                        +
                        "SUM(CASE WHEN j.processingTimeMs IS NOT NULL THEN j.processingTimeMs ELSE 0 END) as totalProcessingTime "
                        +
                        "FROM GenerationJob j WHERE j.userId = :userId")
        Object[] getUserJobStatistics(@Param("userId") Long userId);

        /**
         * Find jobs that can be retried (failed or cancelled with retry count < max)
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND j.status IN ('FAILED', 'CANCELLED') " +
                        "AND j.retryCount < j.maxRetries " +
                        "ORDER BY j.createdAt DESC")
        Page<GenerationJob> findRetryableJobsByUser(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find completed jobs with content (eligible for video generation)
         */
        @Query("SELECT j FROM GenerationJob j WHERE j.userId = :userId " +
                        "AND j.status = 'COMPLETED' " +
                        "AND j.resultContent IS NOT NULL " +
                        "AND LENGTH(TRIM(j.resultContent)) > 0 " +
                        "ORDER BY j.completedAt DESC")
        Page<GenerationJob> findVideoEligibleJobsByUser(@Param("userId") Long userId, Pageable pageable);
}