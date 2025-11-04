package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing a content generation job in the queue system
 * Supports asynchronous processing with status tracking and priority queuing
 */
@Entity
@Table(name = "generation_jobs", indexes = {
        @Index(name = "idx_generation_jobs_status", columnList = "status"),
        @Index(name = "idx_generation_jobs_priority", columnList = "priority"),
        @Index(name = "idx_generation_jobs_user_id", columnList = "user_id"),
        @Index(name = "idx_generation_jobs_created_at", columnList = "created_at"),
        @Index(name = "idx_generation_jobs_status_priority", columnList = "status, priority, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique job identifier for tracking
     */
    @Column(name = "job_id", unique = true, nullable = false, length = 36)
    private String jobId;

    /**
     * User who requested the content generation
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Content generation request parameters stored as JSON
     */
    @Column(name = "request_params", columnDefinition = "jsonb")
    private Map<String, Object> requestParams;

    /**
     * Job status tracking
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    /**
     * Job priority for queue processing
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private JobPriority priority = JobPriority.STANDARD;

    /**
     * Content type being generated
     */
    @Column(name = "content_type", length = 50)
    private String contentType;

    /**
     * AI provider selected for this job
     */
    @Column(name = "ai_provider", length = 50)
    private String aiProvider;

    /**
     * AI model used for generation
     */
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    /**
     * Generated content result
     */
    @Column(name = "result_content", columnDefinition = "text")
    private String resultContent;

    /**
     * Error message if job failed
     */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Error details for debugging
     */
    @Column(name = "error_details", columnDefinition = "jsonb")
    private Map<String, Object> errorDetails;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed
     */
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Processing start time
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Processing completion time
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Next retry time for failed jobs
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    /**
     * Job expiration time for cleanup
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Processing time in milliseconds
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * Tokens used for generation
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * Generation cost
     */
    @Column(name = "generation_cost", precision = 10, scale = 4)
    private BigDecimal generationCost;

    /**
     * Job metadata for additional information
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Job status enumeration
     */
    public enum JobStatus {
        QUEUED, // Job is waiting in queue
        PROCESSING, // Job is currently being processed
        COMPLETED, // Job completed successfully
        FAILED, // Job failed after all retries
        CANCELLED, // Job was cancelled by user
        EXPIRED // Job expired before processing
    }

    /**
     * Job priority enumeration for queue processing
     */
    public enum JobPriority {
        PREMIUM(1), // Highest priority for premium users
        STANDARD(2), // Standard priority for regular users
        BATCH(3); // Lowest priority for batch operations

        private final int level;

        JobPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * Check if job can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries &&
                (status == JobStatus.FAILED || status == JobStatus.QUEUED);
    }

    /**
     * Check if job is in a final state
     */
    public boolean isFinalState() {
        return status == JobStatus.COMPLETED ||
                status == JobStatus.FAILED ||
                status == JobStatus.CANCELLED ||
                status == JobStatus.EXPIRED;
    }

    /**
     * Check if job has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Calculate processing duration
     */
    public Long getProcessingDuration() {
        if (startedAt != null && completedAt != null) {
            return completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return null;
    }
}