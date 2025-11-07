package ai.content.auto.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for video generation job response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoJobDto {

    private Long id;
    private String jobId;
    private Long userId;
    private Long templateId;
    private String templateName;
    private Long contentId;
    private String status;
    private String priority;

    // Video Information
    private String videoTitle;
    private String videoDescription;
    private String videoScript;
    private Integer duration;

    // Branding Configuration
    private Map<String, Object> brandingConfig;
    private Map<String, Object> generationParams;

    // Processing Information
    private Instant startedAt;
    private Instant completedAt;
    private Long processingTimeMs;

    // Result Information
    private String videoUrl;
    private String thumbnailUrl;
    private Long videoSizeBytes;
    private String videoFormat;

    // Error Information
    private String errorMessage;
    private String errorCode;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant nextRetryAt;

    // Batch Information
    private String batchId;
    private Integer batchPosition;

    // Scheduling Information
    private Instant scheduledAt;
    private Long scheduledBy;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // Computed Fields
    private Integer progressPercentage;
    private String statusMessage;
}
