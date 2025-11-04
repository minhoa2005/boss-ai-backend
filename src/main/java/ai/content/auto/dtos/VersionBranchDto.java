package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for version branch information.
 * Contains branch metadata and version list.
 * 
 * Requirements: 1.2, 1.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionBranchDto {

    private String branchName;
    private String description;
    private Boolean isExperimental;
    private Integer versionCount;
    private ContentVersionDto latestVersion;
    private List<ContentVersionDto> versions;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastModifiedAt;
}