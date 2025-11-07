package ai.content.auto.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch video generation job creation.
 * Includes initial progress tracking information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchVideoJobResponse {

    private String batchId;
    private String batchName;
    private Integer totalJobs;
    private Integer queuedJobs;
    private Integer failedJobs;
    private List<VideoJobDto> jobs;
    private String message;

    // Progress tracking
    private BatchProgressDto progress;
}
