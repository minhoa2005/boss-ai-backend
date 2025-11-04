package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for creating a version branch.
 * Used for branching content versions for experimental development.
 * 
 * Requirements: 1.2, 1.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionBranchRequest {

    @NotNull(message = "Parent version ID is required")
    private Long parentVersionId;

    @NotBlank(message = "Branch name is required")
    @Size(max = 50, message = "Branch name must not exceed 50 characters")
    private String branchName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Builder.Default
    private Boolean isExperimental = true;

    @Size(max = 1000, message = "Annotation must not exceed 1000 characters")
    private String annotation;
}