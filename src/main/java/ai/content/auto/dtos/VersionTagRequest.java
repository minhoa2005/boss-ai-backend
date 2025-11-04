package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for tagging and annotating content versions.
 * Used for organizing and categorizing content versions.
 * 
 * Requirements: 1.2, 1.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionTagRequest {

    @NotBlank(message = "Version tag is required")
    @Size(max = 100, message = "Version tag must not exceed 100 characters")
    private String versionTag;

    @Size(max = 1000, message = "Annotation must not exceed 1000 characters")
    private String annotation;
}