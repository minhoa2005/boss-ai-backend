package ai.content.auto.service;

import ai.content.auto.dto.VideoAnalyticsDto;
import ai.content.auto.entity.VideoAnalytics;
import ai.content.auto.entity.VideoPublication;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.VideoAnalyticsRepository;
import ai.content.auto.repository.VideoPublicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for tracking and analyzing video performance metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalyticsService {

    private final VideoAnalyticsRepository analyticsRepository;
    private final VideoPublicationRepository publicationRepository;

    /**
     * Record analytics snapshot for a publication
     */
    @Transactional
    public VideoAnalyticsDto recordAnalytics(Long publicationId, VideoAnalyticsDto analyticsDto) {
        try {
            VideoPublication publication = publicationRepository.findById(publicationId)
                    .orElseThrow(() -> new BusinessException("Publication not found: " + publicationId));

            VideoAnalytics analytics = VideoAnalytics.builder()
                    .publication(publication)
                    .views(analyticsDto.getViews())
                    .likes(analyticsDto.getLikes())
                    .dislikes(analyticsDto.getDislikes())
                    .comments(analyticsDto.getComments())
                    .shares(analyticsDto.getShares())
                    .watchTimeSeconds(analyticsDto.getWatchTimeSeconds())
                    .averageViewDurationSeconds(analyticsDto.getAverageViewDurationSeconds())
                    .clickThroughRate(analyticsDto.getClickThroughRate())
                    .conversionRate(analyticsDto.getConversionRate())
                    .revenue(analyticsDto.getRevenue())
                    .impressions(analyticsDto.getImpressions())
                    .reach(analyticsDto.getReach())
                    .snapshotAt(Instant.now())
                    .build();

            // Calculate engagement rate
            analytics.calculateEngagementRate();

            VideoAnalytics saved = analyticsRepository.save(analytics);

            log.info("Recorded analytics snapshot for publication {}", publicationId);

            return mapToDto(saved);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to record analytics for publication: {}", publicationId, e);
            throw new BusinessException("Failed to record video analytics");
        }
    }

    /**
     * Get analytics history for a publication
     */
    public List<VideoAnalyticsDto> getAnalyticsHistory(Long publicationId) {
        try {
            publicationRepository.findById(publicationId)
                    .orElseThrow(() -> new BusinessException("Publication not found: " + publicationId));

            List<VideoAnalytics> analytics = analyticsRepository
                    .findByPublicationIdOrderBySnapshotAtDesc(publicationId);

            return analytics.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get analytics history for publication: {}", publicationId, e);
            throw new BusinessException("Failed to retrieve analytics history");
        }
    }

    /**
     * Get latest analytics for a publication
     */
    public VideoAnalyticsDto getLatestAnalytics(Long publicationId) {
        try {
            publicationRepository.findById(publicationId)
                    .orElseThrow(() -> new BusinessException("Publication not found: " + publicationId));

            return analyticsRepository.findLatestByPublicationId(publicationId)
                    .map(this::mapToDto)
                    .orElse(null);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get latest analytics for publication: {}", publicationId, e);
            throw new BusinessException("Failed to retrieve latest analytics");
        }
    }

    /**
     * Get analytics for date range
     */
    public List<VideoAnalyticsDto> getAnalyticsByDateRange(Instant startDate, Instant endDate) {
        try {
            List<VideoAnalytics> analytics = analyticsRepository.findByDateRange(startDate, endDate);

            return analytics.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get analytics for date range", e);
            throw new BusinessException("Failed to retrieve analytics");
        }
    }

    /**
     * Get platform performance summary
     */
    public PlatformPerformanceSummary getPlatformPerformance(String platform) {
        try {
            Long totalViews = analyticsRepository.getTotalViewsByPlatform(platform);
            Double avgEngagementRate = analyticsRepository.getAverageEngagementRateByPlatform(platform);
            Long publishedCount = publicationRepository.countPublishedByPlatform(platform);

            return PlatformPerformanceSummary.builder()
                    .platform(platform)
                    .totalViews(totalViews != null ? totalViews : 0L)
                    .averageEngagementRate(avgEngagementRate != null ? avgEngagementRate : 0.0)
                    .publishedVideosCount(publishedCount != null ? publishedCount : 0L)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get platform performance for: {}", platform, e);
            throw new BusinessException("Failed to retrieve platform performance");
        }
    }

    private VideoAnalyticsDto mapToDto(VideoAnalytics analytics) {
        return VideoAnalyticsDto.builder()
                .id(analytics.getId())
                .publicationId(analytics.getPublication().getId())
                .views(analytics.getViews())
                .likes(analytics.getLikes())
                .dislikes(analytics.getDislikes())
                .comments(analytics.getComments())
                .shares(analytics.getShares())
                .watchTimeSeconds(analytics.getWatchTimeSeconds())
                .averageViewDurationSeconds(analytics.getAverageViewDurationSeconds())
                .engagementRate(analytics.getEngagementRate())
                .clickThroughRate(analytics.getClickThroughRate())
                .conversionRate(analytics.getConversionRate())
                .revenue(analytics.getRevenue())
                .impressions(analytics.getImpressions())
                .reach(analytics.getReach())
                .snapshotAt(analytics.getSnapshotAt())
                .createdAt(analytics.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PlatformPerformanceSummary {
        private String platform;
        private Long totalViews;
        private Double averageEngagementRate;
        private Long publishedVideosCount;
    }
}
