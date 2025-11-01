package ai.content.auto.dtos;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

/** DTO for {@link ai.content.auto.entity.VUserConfig} */
public record VUserConfigDto(
    Long id,
    @Size(max = 100) String category,
    @Size(max = 100) String value,
    @Size(max = 200) String label,
    @Size(max = 200) String displayLabel,
    @Size(max = 500) String description,
    Integer sortOrder,
    Boolean configActive,
    @Size(max = 10) String language,
    Instant configCreatedAt,
    Instant configUpdatedAt,
    Long userId,
    Boolean isSelected,
    Instant userSelectionCreatedAt,
    Instant userSelectionUpdatedAt)
    implements Serializable {}
