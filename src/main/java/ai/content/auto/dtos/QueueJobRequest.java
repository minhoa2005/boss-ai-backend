package ai.content.auto.dtos;

import ai.content.auto.entity.GenerationJob.JobPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for queuing a content generation job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueJobRequest {

    /**
     * Content generation parameters
     */
    @NotNull(message = "Request parameters are required")
    private Map<String, Object> requestParams;

    /**
     * Job priority level
     */
    @Builder.Default
    private JobPriority priority = JobPriority.STANDARD;

    /**
     * Content type being generated
     */
    private String contentType;

    /**
     * Preferred AI provider (optional)
     */
    private String preferredProvider;

    /**
     * Job expiration time in hours (optional, defaults to 24 hours)
     */
    @Builder.Default
    private Integer expirationHours = 24;

    /**
     * Maximum retry attempts (optional, defaults to 3)
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Additional metadata for the job
     */
    private Map<String, Object> metadata;
}