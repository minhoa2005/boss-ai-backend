package ai.content.auto.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a batch of video generation jobs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchVideoJobRequest {

    @NotEmpty(message = "Video jobs list cannot be empty")
    @Size(max = 100, message = "Batch size cannot exceed 100 videos")
    @Valid
    private List<CreateVideoJobRequest> videoJobs;

    private String batchName; // Optional name for the batch

    private String priority; // Priority for all jobs in batch
}
