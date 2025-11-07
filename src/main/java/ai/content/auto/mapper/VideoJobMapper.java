package ai.content.auto.mapper;

import ai.content.auto.dto.response.VideoJobDto;
import ai.content.auto.entity.VideoGenerationJob;
import org.springframework.stereotype.Component;

/**
 * Mapper for VideoGenerationJob entity to DTO conversions.
 */
@Component
public class VideoJobMapper {

    public VideoJobDto toDto(VideoGenerationJob job) {
        if (job == null) {
            return null;
        }

        VideoJobDto dto = VideoJobDto.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .userId(job.getUser() != null ? job.getUser().getId() : null)
                .templateId(job.getTemplate() != null ? job.getTemplate().getId() : null)
                .templateName(job.getTemplate() != null ? job.getTemplate().getName() : null)
                .contentId(job.getContent() != null ? job.getContent().getId() : null)
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .priority(job.getPriority() != null ? job.getPriority().name() : null)
                .videoTitle(job.getVideoTitle())
                .videoDescription(job.getVideoDescription())
                .videoScript(job.getVideoScript())
                .duration(job.getDuration())
                .brandingConfig(job.getBrandingConfig())
                .generationParams(job.getGenerationParams())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .processingTimeMs(job.getProcessingTimeMs())
                .videoUrl(job.getVideoUrl())
                .thumbnailUrl(job.getThumbnailUrl())
                .videoSizeBytes(job.getVideoSizeBytes())
                .videoFormat(job.getVideoFormat())
                .errorMessage(job.getErrorMessage())
                .errorCode(job.getErrorCode())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .nextRetryAt(job.getNextRetryAt())
                .batchId(job.getBatchId())
                .batchPosition(job.getBatchPosition())
                .scheduledAt(job.getScheduledAt())
                .scheduledBy(job.getScheduledBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();

        // Calculate progress percentage
        dto.setProgressPercentage(calculateProgress(job));

        // Set status message
        dto.setStatusMessage(getStatusMessage(job));

        return dto;
    }

    private Integer calculateProgress(VideoGenerationJob job) {
        if (job.getStatus() == null) {
            return 0;
        }

        return switch (job.getStatus()) {
            case QUEUED -> 0;
            case PROCESSING -> {
                if (job.getStartedAt() != null && job.getDuration() != null) {
                    // Estimate progress based on expected duration
                    long elapsedSeconds = java.time.Duration.between(
                            job.getStartedAt(), java.time.Instant.now()).getSeconds();
                    int estimatedTotalSeconds = job.getDuration() * 2; // Video generation takes ~2x video duration
                    int progress = (int) ((elapsedSeconds * 100.0) / estimatedTotalSeconds);
                    yield Math.min(progress, 95); // Cap at 95% until actually completed
                }
                yield 50; // Default to 50% if we can't estimate
            }
            case COMPLETED -> 100;
            case FAILED, CANCELLED -> 0;
        };
    }

    private String getStatusMessage(VideoGenerationJob job) {
        if (job.getStatus() == null) {
            return "Unknown status";
        }

        return switch (job.getStatus()) {
            case QUEUED -> {
                if (job.getScheduledAt() != null && job.getScheduledAt().isAfter(java.time.Instant.now())) {
                    yield "Scheduled for " + job.getScheduledAt();
                }
                yield "Waiting in queue...";
            }
            case PROCESSING -> "Generating video...";
            case COMPLETED -> "Video generation completed";
            case FAILED ->
                job.getErrorMessage() != null ? "Failed: " + job.getErrorMessage() : "Video generation failed";
            case CANCELLED -> "Video generation cancelled";
        };
    }
}
