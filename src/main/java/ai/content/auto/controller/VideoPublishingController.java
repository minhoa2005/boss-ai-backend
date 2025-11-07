package ai.content.auto.controller;

import ai.content.auto.dto.PublishVideoRequest;
import ai.content.auto.dto.VideoAnalyticsDto;
import ai.content.auto.dto.VideoPublicationDto;
import ai.content.auto.dto.VideoSEOMetadataDto;
import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.VideoAnalyticsService;
import ai.content.auto.service.VideoPublishingService;
import ai.content.auto.service.VideoSEOService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for video publishing and distribution
 */
@RestController
@RequestMapping("/api/v1/video/publishing")
@RequiredArgsConstructor
@Slf4j
public class VideoPublishingController {

    private final VideoPublishingService publishingService;
    private final VideoAnalyticsService analyticsService;
    private final VideoSEOService seoService;

    /**
     * Schedule video publication to social media platforms
     */
    @PostMapping("/schedule")
    public ResponseEntity<BaseResponse<List<VideoPublicationDto>>> schedulePublication(
            @Valid @RequestBody PublishVideoRequest request) {

        log.info("Scheduling video publication for job {} to {} platforms",
                request.getVideoJobId(), request.getPlatforms().size());

        List<VideoPublicationDto> publications = publishingService.schedulePublication(request);

        BaseResponse<List<VideoPublicationDto>> response = new BaseResponse<List<VideoPublicationDto>>()
                .setErrorMessage("Video publication scheduled successfully")
                .setData(publications);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get publications for a video job
     */
    @GetMapping("/video/{videoJobId}")
    public ResponseEntity<BaseResponse<List<VideoPublicationDto>>> getPublicationsByVideoJob(
            @PathVariable Long videoJobId) {

        log.info("Retrieving publications for video job {}", videoJobId);

        List<VideoPublicationDto> publications = publishingService.getPublicationsByVideoJob(videoJobId);

        BaseResponse<List<VideoPublicationDto>> response = new BaseResponse<List<VideoPublicationDto>>()
                .setErrorMessage("Publications retrieved successfully")
                .setData(publications);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's publications
     */
    @GetMapping("/my-publications")
    public ResponseEntity<BaseResponse<List<VideoPublicationDto>>> getUserPublications() {

        log.info("Retrieving user publications");

        List<VideoPublicationDto> publications = publishingService.getUserPublications();

        BaseResponse<List<VideoPublicationDto>> response = new BaseResponse<List<VideoPublicationDto>>()
                .setErrorMessage("User publications retrieved successfully")
                .setData(publications);

        return ResponseEntity.ok(response);
    }

    /**
     * Get publications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<BaseResponse<List<VideoPublicationDto>>> getPublicationsByStatus(
            @PathVariable String status) {

        log.info("Retrieving publications with status {}", status);

        List<VideoPublicationDto> publications = publishingService.getPublicationsByStatus(status);

        BaseResponse<List<VideoPublicationDto>> response = new BaseResponse<List<VideoPublicationDto>>()
                .setErrorMessage("Publications retrieved successfully")
                .setData(publications);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a scheduled publication
     */
    @DeleteMapping("/{publicationId}")
    public ResponseEntity<BaseResponse<Void>> cancelPublication(@PathVariable Long publicationId) {

        log.info("Cancelling publication {}", publicationId);

        publishingService.cancelPublication(publicationId);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Publication cancelled successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Record analytics for a publication
     */
    @PostMapping("/{publicationId}/analytics")
    public ResponseEntity<BaseResponse<VideoAnalyticsDto>> recordAnalytics(
            @PathVariable Long publicationId,
            @Valid @RequestBody VideoAnalyticsDto analyticsDto) {

        log.info("Recording analytics for publication {}", publicationId);

        VideoAnalyticsDto analytics = analyticsService.recordAnalytics(publicationId, analyticsDto);

        BaseResponse<VideoAnalyticsDto> response = new BaseResponse<VideoAnalyticsDto>()
                .setErrorMessage("Analytics recorded successfully")
                .setData(analytics);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get analytics history for a publication
     */
    @GetMapping("/{publicationId}/analytics")
    public ResponseEntity<BaseResponse<List<VideoAnalyticsDto>>> getAnalyticsHistory(
            @PathVariable Long publicationId) {

        log.info("Retrieving analytics history for publication {}", publicationId);

        List<VideoAnalyticsDto> analytics = analyticsService.getAnalyticsHistory(publicationId);

        BaseResponse<List<VideoAnalyticsDto>> response = new BaseResponse<List<VideoAnalyticsDto>>()
                .setErrorMessage("Analytics history retrieved successfully")
                .setData(analytics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get latest analytics for a publication
     */
    @GetMapping("/{publicationId}/analytics/latest")
    public ResponseEntity<BaseResponse<VideoAnalyticsDto>> getLatestAnalytics(
            @PathVariable Long publicationId) {

        log.info("Retrieving latest analytics for publication {}", publicationId);

        VideoAnalyticsDto analytics = analyticsService.getLatestAnalytics(publicationId);

        BaseResponse<VideoAnalyticsDto> response = new BaseResponse<VideoAnalyticsDto>()
                .setErrorMessage("Latest analytics retrieved successfully")
                .setData(analytics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get platform performance summary
     */
    @GetMapping("/analytics/platform/{platform}")
    public ResponseEntity<BaseResponse<VideoAnalyticsService.PlatformPerformanceSummary>> getPlatformPerformance(
            @PathVariable String platform) {

        log.info("Retrieving platform performance for {}", platform);

        VideoAnalyticsService.PlatformPerformanceSummary performance = analyticsService
                .getPlatformPerformance(platform);

        BaseResponse<VideoAnalyticsService.PlatformPerformanceSummary> response = new BaseResponse<VideoAnalyticsService.PlatformPerformanceSummary>()
                .setErrorMessage("Platform performance retrieved successfully")
                .setData(performance);

        return ResponseEntity.ok(response);
    }

    /**
     * Generate SEO metadata for a video
     */
    @PostMapping("/seo/{videoJobId}")
    public ResponseEntity<BaseResponse<VideoSEOMetadataDto>> generateSEOMetadata(
            @PathVariable Long videoJobId,
            @Valid @RequestBody VideoSEOMetadataDto metadataDto) {

        log.info("Generating SEO metadata for video job {}", videoJobId);

        VideoSEOMetadataDto seoMetadata = seoService.generateSEOMetadata(videoJobId, metadataDto);

        BaseResponse<VideoSEOMetadataDto> response = new BaseResponse<VideoSEOMetadataDto>()
                .setErrorMessage("SEO metadata generated successfully")
                .setData(seoMetadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get SEO metadata for a video
     */
    @GetMapping("/seo/{videoJobId}")
    public ResponseEntity<BaseResponse<VideoSEOMetadataDto>> getSEOMetadata(
            @PathVariable Long videoJobId) {

        log.info("Retrieving SEO metadata for video job {}", videoJobId);

        VideoSEOMetadataDto seoMetadata = seoService.getSEOMetadata(videoJobId);

        BaseResponse<VideoSEOMetadataDto> response = new BaseResponse<VideoSEOMetadataDto>()
                .setErrorMessage("SEO metadata retrieved successfully")
                .setData(seoMetadata);

        return ResponseEntity.ok(response);
    }

    /**
     * Get videos with high SEO scores
     */
    @GetMapping("/seo/high-performing")
    public ResponseEntity<BaseResponse<List<VideoSEOMetadataDto>>> getHighPerformingSEO(
            @RequestParam(defaultValue = "70.0") Double minScore) {

        log.info("Retrieving high performing SEO videos with min score {}", minScore);

        List<VideoSEOMetadataDto> videos = seoService.getHighPerformingSEO(minScore);

        BaseResponse<List<VideoSEOMetadataDto>> response = new BaseResponse<List<VideoSEOMetadataDto>>()
                .setErrorMessage("High performing videos retrieved successfully")
                .setData(videos);

        return ResponseEntity.ok(response);
    }

    /**
     * Get average SEO score
     */
    @GetMapping("/seo/average-score")
    public ResponseEntity<BaseResponse<Double>> getAverageSEOScore() {

        log.info("Retrieving average SEO score");

        Double avgScore = seoService.getAverageSEOScore();

        BaseResponse<Double> response = new BaseResponse<Double>()
                .setErrorMessage("Average SEO score retrieved successfully")
                .setData(avgScore);

        return ResponseEntity.ok(response);
    }
}
