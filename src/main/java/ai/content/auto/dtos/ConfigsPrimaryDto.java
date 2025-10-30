package ai.content.auto.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

/** DTO for {@link ai.content.auto.entity.ConfigsPrimary} */
public record ConfigsPrimaryDto(
    Long id,
    @NotNull @Size(max = 100) String category,
    @NotNull @Size(max = 100) String value,
    @NotNull @Size(max = 200) String label,
    @NotNull @Size(max = 200) String displayLabel,
    @Size(max = 500) String description,
    @NotNull Integer sortOrder,
    @NotNull Boolean active,
    @NotNull @Size(max = 10) String language,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt,
    @Size(max = 100) String createdBy,
    @Size(max = 100) String updatedBy)
    implements Serializable {}
