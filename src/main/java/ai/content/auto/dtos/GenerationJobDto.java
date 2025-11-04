package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobStatus;
import ai.content.auto.entity.GenerationJob.JobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * DTO for GenerationJob entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJobDto {

    private Long id;
    private String jobId;
    private Long userId;
    private Map<String, Object> requestParams;
    private JobStatus status;
    private JobPriority priority;
    private String contentType;
    private String aiProvider;
    private String aiModel;
    private String resultContent;
    private String errorMessage;
    private Map<String, Object> errorDetails;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant startedAt;
    private Instant completedAt;
    private Instant nextRetryAt;
    private Instant expiresAt;
    private Long processingTimeMs;
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Get processing duration in milliseconds
     */
    public Long getProcessingDuration() {
        if (startedAt != null && completedAt != null) {
            return completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return null;
    }

    /**
     * Check if job is in progress
     */
    public boolean isInProgress() {
        return status == JobStatus.PROCESSING || status == JobStatus.QUEUED;
    }

    /**
     * Check if job is completed successfully
     */
    public boolean isCompleted() {
        return status == JobStatus.COMPLETED;
    }

    /**
     * Check if job has failed
     */
    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }
}