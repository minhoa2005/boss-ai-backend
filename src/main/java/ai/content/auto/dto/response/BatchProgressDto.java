package ai.content.auto.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for batch video generation progress tracking.
 * Provides real-time progress information for batch operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProgressDto {

    private String batchId;
    private String batchName;

    // Job counts
    private Integer totalJobs;
    private Integer queuedJobs;
    private Integer processingJobs;
    private Integer completedJobs;
    private Integer failedJobs;
    private Integer cancelledJobs;

    // Progress metrics
    private Integer progressPercentage; // 0-100
    private String status; // QUEUED, PROCESSING, COMPLETED, FAILED, PARTIAL

    // Timing information
    private Instant startedAt;
    private Instant completedAt;
    private Long estimatedTimeRemainingMs;
    private Long averageProcessingTimeMs;

    // Additional information
    private String message;
    private Boolean isComplete;
    private Boolean hasErrors;
}
