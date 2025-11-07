package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Entity representing a video generation job in the queue system.
 * Supports batch video processing with template-based generation.
 */
@Entity
@Table(name = "video_generation_jobs", indexes = {
        @Index(name = "idx_video_job_status", columnList = "status"),
        @Index(name = "idx_video_job_user", columnList = "user_id"),
        @Index(name = "idx_video_job_created", columnList = "created_at"),
        @Index(name = "idx_video_job_priority", columnList = "priority, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private VideoTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private ContentGeneration content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPriority priority;

    // Video Generation Parameters
    @Column(name = "video_title", length = 500)
    private String videoTitle;

    @Column(name = "video_description", columnDefinition = "TEXT")
    private String videoDescription;

    @Column(name = "video_script", columnDefinition = "TEXT")
    private String videoScript;

    @Column(name = "duration")
    private Integer duration; // in seconds

    // Branding Overrides (can override template settings)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "branding_config", columnDefinition = "jsonb")
    private Map<String, Object> brandingConfig;

    // Advanced Configuration
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_params", columnDefinition = "jsonb")
    private Map<String, Object> generationParams;

    // Processing Information
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // Result Information
    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "video_size_bytes")
    private Long videoSizeBytes;

    @Column(name = "video_format", length = 20)
    private String videoFormat;

    // Error Handling
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    // Batch Information
    @Column(name = "batch_id", length = 100)
    private String batchId; // For grouping multiple video jobs together

    @Column(name = "batch_position")
    private Integer batchPosition; // Position within batch

    // Scheduling
    @Column(name = "scheduled_at")
    private Instant scheduledAt; // When the job should start processing

    @Column(name = "scheduled_by")
    private Long scheduledBy; // User who scheduled the job

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = JobStatus.QUEUED;
        }
        if (priority == null) {
            priority = JobPriority.STANDARD;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum JobStatus {
        QUEUED, // Job is waiting in queue
        PROCESSING, // Job is currently being processed
        COMPLETED, // Job completed successfully
        FAILED, // Job failed after retries
        CANCELLED // Job was cancelled by user
    }

    public enum JobPriority {
        LOW, // Low priority (batch jobs)
        STANDARD, // Standard priority (default)
        HIGH, // High priority (premium users)
        URGENT // Urgent priority (system critical)
    }
}
