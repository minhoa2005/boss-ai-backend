package ai.content.auto.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** DTO for {@link ai.content.auto.entity.ContentGeneration} */
public record ContentGenerationDto(
        Long id,
        @NotNull UserDto user,
        @NotNull @Size(max = 20) String contentType,
        @NotNull @Size(max = 20) String status,
        @NotNull @Size(max = 50) String aiProvider,
        @Size(max = 100) String aiModel,
        String prompt,
        String generatedContent,
        @Size(max = 500) String title,
        Integer wordCount,
        Integer characterCount,
        Integer tokensUsed,
        BigDecimal generationCost,
        Long processingTimeMs,
        BigDecimal qualityScore,
        BigDecimal readabilityScore,
        BigDecimal sentimentScore,
        Long templateId,
        @Size(max = 100) String industry,
        @Size(max = 200) String targetAudience,
        @Size(max = 50) String tone,
        @Size(max = 10) String language,
        @Size(max = 1000) String errorMessage,
        @NotNull Integer retryCount,
        @NotNull Integer maxRetries,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        @NotNull Boolean isBillable,
        Long subscriptionId,
        @NotNull Instant createdAt,
        Instant updatedAt,
        Long version,
        Integer currentVersion,
        @Size(max = 255) String openaiResponseId)
        implements Serializable {
}
