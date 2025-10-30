package ai.content.auto.dtos;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;

/** DTO for {@link ai.content.auto.entity.ConfigsUser} */
public record ConfigsUserDto(
    Long id,
    ConfigsPrimaryDto configsPrimary,
    UserDto user,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt)
    implements Serializable {}
