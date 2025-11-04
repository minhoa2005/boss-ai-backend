package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for queue statistics and monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatisticsDto {

    /**
     * Total jobs in queue
     */
    private Long queuedJobs;

    /**
     * Jobs currently being processed
     */
    private Long processingJobs;

    /**
     * Jobs completed in the last hour
     */
    private Long completedJobsLastHour;

    /**
     * Jobs failed in the last hour
     */
    private Long failedJobsLastHour;

    /**
     * Average processing time in milliseconds
     */
    private Double averageProcessingTimeMs;

    /**
     * Queue depth by priority
     */
    private Map<String, Long> queueDepthByPriority;

    /**
     * Processing capacity utilization (0.0 to 1.0)
     */
    private Double capacityUtilization;

    /**
     * Estimated wait time for new jobs in minutes
     */
    private Integer estimatedWaitTimeMinutes;

    /**
     * System health status
     */
    private String healthStatus;

    /**
     * Last updated timestamp
     */
    private Instant lastUpdated;

    /**
     * Additional metrics
     */
    private Map<String, Object> additionalMetrics;
}