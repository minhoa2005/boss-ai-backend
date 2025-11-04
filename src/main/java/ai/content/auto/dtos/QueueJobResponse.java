package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for queued content generation job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueJobResponse {

    /**
     * Unique job identifier for tracking
     */
    private String jobId;

    /**
     * Current job status
     */
    private JobStatus status;

    /**
     * Job priority
     */
    private JobPriority priority;

    /**
     * Estimated queue position (1-based)
     */
    private Integer queuePosition;

    /**
     * Estimated processing time in minutes
     */
    private Integer estimatedProcessingTimeMinutes;

    /**
     * Job creation timestamp
     */
    private Instant createdAt;

    /**
     * Job expiration timestamp
     */
    private Instant expiresAt;

    /**
     * Message for the user
     */
    private String message;

    /**
     * WebSocket channel for real-time updates
     */
    private String websocketChannel;
}