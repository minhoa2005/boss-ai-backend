package ai.content.auto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a video generation job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoJobRequest {

    @NotNull(message = "Template ID is required")
    @Positive(message = "Template ID must be positive")
    private Long templateId;

    private Long contentId; // Optional: generate video from existing content

    @NotBlank(message = "Video title is required")
    @Size(max = 500, message = "Video title must not exceed 500 characters")
    private String videoTitle;

    @Size(max = 5000, message = "Video description must not exceed 5000 characters")
    private String videoDescription;

    @NotBlank(message = "Video script is required")
    @Size(max = 50000, message = "Video script must not exceed 50000 characters")
    private String videoScript;

    @Positive(message = "Duration must be positive")
    private Integer duration; // in seconds

    private Map<String, Object> brandingConfig; // Override template branding

    private Map<String, Object> generationParams; // Additional parameters

    private String priority; // LOW, STANDARD, HIGH, URGENT

    private String scheduledAt; // ISO-8601 timestamp for scheduled execution

    private String batchId; // For batch processing

    private Integer batchPosition; // Position within batch
}
