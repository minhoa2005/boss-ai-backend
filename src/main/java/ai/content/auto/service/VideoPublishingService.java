package ai.content.auto.service;

import ai.content.auto.dto.PublishVideoRequest;
import ai.content.auto.dto.VideoPublicationDto;
import ai.content.auto.entity.VideoGenerationJob;
import ai.content.auto.entity.VideoPublication;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.VideoGenerationJobRepository;
import ai.content.auto.repository.VideoPublicationRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing video publishing to social media platforms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoPublishingService {

    private final VideoPublicationRepository publicationRepository;
    private final VideoGenerationJobRepository videoJobRepository;
    private final SecurityUtil securityUtil;

    /**
     * Schedule video publication to multiple platforms
     */
    public List<VideoPublicationDto> schedulePublication(PublishVideoRequest request) {
        try {
            // Validate video job exists
            VideoGenerationJob videoJob = validateVideoJob(request.getVideoJobId());

            // Get current user
            Long userId = securityUtil.getCurrentUser().getId();

            // Create publications for each platform
            List<VideoPublication> publications = createPublications(request, videoJob, userId);

            log.info("Scheduled video publication for job {} to {} platforms by user {}",
                    request.getVideoJobId(), request.getPlatforms().size(), userId);

            return publications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to schedule video publication for job: {}", request.getVideoJobId(), e);
            throw new BusinessException("Failed to schedule video publication");
        }
    }

    @Transactional
    private List<VideoPublication> createPublications(PublishVideoRequest request, VideoGenerationJob videoJob,
            Long userId) {
        List<VideoPublication> publications = new ArrayList<>();

        for (String platform : request.getPlatforms()) {
            VideoPublication publication = VideoPublication.builder()
                    .videoJob(videoJob)
                    .platform(platform.toUpperCase())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                    .visibility(request.getVisibility() != null ? request.getVisibility() : "PUBLIC")
                    .category(request.getCategory())
                    .thumbnailUrl(request.getThumbnailUrl())
                    .scheduledAt(request.getScheduledAt())
                    .status(request.getScheduledAt() != null ? "SCHEDULED" : "PENDING")
                    .createdBy(userId)
                    .build();

            publications.add(publicationRepository.save(publication));
        }

        return publications;
    }

    /**
     * Get all publications for a video job
     */
    public List<VideoPublicationDto> getPublicationsByVideoJob(Long videoJobId) {
        try {
            validateVideoJob(videoJobId);

            List<VideoPublication> publications = publicationRepository.findByVideoJobId(videoJobId);

            return publications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get publications for video job: {}", videoJobId, e);
            throw new BusinessException("Failed to retrieve video publications");
        }
    }

    /**
     * Get publications by status
     */
    public List<VideoPublicationDto> getPublicationsByStatus(String status) {
        try {
            List<VideoPublication> publications = publicationRepository.findByStatus(status.toUpperCase());

            return publications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get publications by status: {}", status, e);
            throw new BusinessException("Failed to retrieve video publications");
        }
    }

    /**
     * Get user's publications
     */
    public List<VideoPublicationDto> getUserPublications() {
        try {
            Long userId = securityUtil.getCurrentUser().getId();

            List<VideoPublication> publications = publicationRepository.findByUserId(userId);

            return publications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get user publications", e);
            throw new BusinessException("Failed to retrieve user publications");
        }
    }

    /**
     * Cancel a scheduled publication
     */
    @Transactional
    public void cancelPublication(Long publicationId) {
        try {
            VideoPublication publication = publicationRepository.findById(publicationId)
                    .orElseThrow(() -> new BusinessException("Publication not found: " + publicationId));

            if (!"SCHEDULED".equals(publication.getStatus()) && !"PENDING".equals(publication.getStatus())) {
                throw new BusinessException("Cannot cancel publication with status: " + publication.getStatus());
            }

            publicationRepository.delete(publication);

            log.info("Cancelled publication {} for video job {}", publicationId, publication.getVideoJob().getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel publication: {}", publicationId, e);
            throw new BusinessException("Failed to cancel publication");
        }
    }

    /**
     * Update publication status
     */
    @Transactional
    public VideoPublicationDto updatePublicationStatus(Long publicationId, String status, String platformVideoId,
            String publicationUrl) {
        try {
            VideoPublication publication = publicationRepository.findById(publicationId)
                    .orElseThrow(() -> new BusinessException("Publication not found: " + publicationId));

            publication.setStatus(status.toUpperCase());

            if (platformVideoId != null) {
                publication.setPlatformVideoId(platformVideoId);
            }

            if (publicationUrl != null) {
                publication.setPublicationUrl(publicationUrl);
            }

            if ("PUBLISHED".equals(status.toUpperCase())) {
                publication.setPublishedAt(Instant.now());
            }

            VideoPublication updated = publicationRepository.save(publication);

            log.info("Updated publication {} status to {}", publicationId, status);

            return mapToDto(updated);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update publication status: {}", publicationId, e);
            throw new BusinessException("Failed to update publication status");
        }
    }

    /**
     * Get scheduled publications ready to publish
     */
    public List<VideoPublicationDto> getScheduledPublicationsReadyToPublish() {
        try {
            List<VideoPublication> publications = publicationRepository
                    .findScheduledPublicationsReadyToPublish(Instant.now());

            return publications.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get scheduled publications", e);
            throw new BusinessException("Failed to retrieve scheduled publications");
        }
    }

    private VideoGenerationJob validateVideoJob(Long videoJobId) {
        return videoJobRepository.findById(videoJobId)
                .orElseThrow(() -> new BusinessException("Video job not found: " + videoJobId));
    }

    private VideoPublicationDto mapToDto(VideoPublication publication) {
        return VideoPublicationDto.builder()
                .id(publication.getId())
                .videoJobId(publication.getVideoJob().getId())
                .platform(publication.getPlatform())
                .platformVideoId(publication.getPlatformVideoId())
                .publicationUrl(publication.getPublicationUrl())
                .status(publication.getStatus())
                .scheduledAt(publication.getScheduledAt())
                .publishedAt(publication.getPublishedAt())
                .title(publication.getTitle())
                .description(publication.getDescription())
                .tags(publication.getTags())
                .visibility(publication.getVisibility())
                .category(publication.getCategory())
                .thumbnailUrl(publication.getThumbnailUrl())
                .errorMessage(publication.getErrorMessage())
                .retryCount(publication.getRetryCount())
                .maxRetries(publication.getMaxRetries())
                .createdAt(publication.getCreatedAt())
                .updatedAt(publication.getUpdatedAt())
                .build();
    }
}
