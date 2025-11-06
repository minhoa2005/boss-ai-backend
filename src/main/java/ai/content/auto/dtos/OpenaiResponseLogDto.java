package ai.content.auto.dtos;

import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** DTO for {@link ai.content.auto.entity.OpenaiResponseLog} */
public record OpenaiResponseLogDto(
        Long id,
        UserDto user,
        Map<String, Object> contentInput,
        Map<String, Object> openaiResult,
        Instant createAt,
        Instant responseTime,
        @Size(max = 50) String model,
        @Size(max = 255) String openaiResponseId)
        implements Serializable {
}
