package ai.content.auto.service;

import ai.content.auto.dto.VideoSEOMetadataDto;
import ai.content.auto.entity.VideoGenerationJob;
import ai.content.auto.entity.VideoSEOMetadata;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.VideoGenerationJobRepository;
import ai.content.auto.repository.VideoSEOMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for video SEO optimization and metadata management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSEOService {

    private final VideoSEOMetadataRepository seoMetadataRepository;
    private final VideoGenerationJobRepository videoJobRepository;

    /**
     * Generate and save SEO metadata for a video
     */
    @Transactional
    public VideoSEOMetadataDto generateSEOMetadata(Long videoJobId, VideoSEOMetadataDto metadataDto) {
        try {
            VideoGenerationJob videoJob = videoJobRepository.findById(videoJobId)
                    .orElseThrow(() -> new BusinessException("Video job not found: " + videoJobId));

            // Check if SEO metadata already exists
            VideoSEOMetadata existing = seoMetadataRepository.findByVideoJobId(videoJobId).orElse(null);

            if (existing != null) {
                // Update existing metadata
                return updateSEOMetadata(existing, metadataDto);
            }

            // Create new SEO metadata
            VideoSEOMetadata seoMetadata = VideoSEOMetadata.builder()
                    .videoJob(videoJob)
                    .optimizedTitle(metadataDto.getOptimizedTitle())
                    .optimizedDescription(metadataDto.getOptimizedDescription())
                    .keywords(metadataDto.getKeywords())
                    .hashtags(metadataDto.getHashtags())
                    .targetAudience(metadataDto.getTargetAudience())
                    .contentCategory(metadataDto.getContentCategory())
                    .language(metadataDto.getLanguage() != null ? metadataDto.getLanguage() : "en")
                    .transcript(metadataDto.getTranscript())
                    .closedCaptionsUrl(metadataDto.getClosedCaptionsUrl())
                    .thumbnailAltText(metadataDto.getThumbnailAltText())
                    .schemaMarkup(metadataDto.getSchemaMarkup())
                    .canonicalUrl(metadataDto.getCanonicalUrl())
                    .build();

            // Calculate SEO scores
            calculateSEOScores(seoMetadata);

            VideoSEOMetadata saved = seoMetadataRepository.save(seoMetadata);

            log.info("Generated SEO metadata for video job {}", videoJobId);

            return mapToDto(saved);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate SEO metadata for video job: {}", videoJobId, e);
            throw new BusinessException("Failed to generate SEO metadata");
        }
    }

    /**
     * Get SEO metadata for a video
     */
    public VideoSEOMetadataDto getSEOMetadata(Long videoJobId) {
        try {
            VideoSEOMetadata metadata = seoMetadataRepository.findByVideoJobId(videoJobId)
                    .orElseThrow(() -> new BusinessException("SEO metadata not found for video job: " + videoJobId));

            return mapToDto(metadata);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get SEO metadata for video job: {}", videoJobId, e);
            throw new BusinessException("Failed to retrieve SEO metadata");
        }
    }

    /**
     * Optimize title for SEO
     */
    public String optimizeTitle(String originalTitle, List<String> keywords) {
        try {
            if (originalTitle == null || originalTitle.isEmpty()) {
                throw new BusinessException("Original title is required");
            }

            // Basic title optimization
            String optimized = originalTitle.trim();

            // Add primary keyword if not present
            if (keywords != null && !keywords.isEmpty()) {
                String primaryKeyword = keywords.get(0);
                if (!optimized.toLowerCase().contains(primaryKeyword.toLowerCase())) {
                    optimized = primaryKeyword + " - " + optimized;
                }
            }

            // Ensure title is within optimal length (60-70 characters)
            if (optimized.length() > 70) {
                optimized = optimized.substring(0, 67) + "...";
            }

            log.info("Optimized title from '{}' to '{}'", originalTitle, optimized);

            return optimized;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to optimize title", e);
            throw new BusinessException("Failed to optimize title");
        }
    }

    /**
     * Generate hashtags from keywords
     */
    public List<String> generateHashtags(List<String> keywords, int maxHashtags) {
        try {
            if (keywords == null || keywords.isEmpty()) {
                return List.of();
            }

            return keywords.stream()
                    .limit(maxHashtags)
                    .map(keyword -> "#" + keyword.replaceAll("\\s+", ""))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate hashtags", e);
            throw new BusinessException("Failed to generate hashtags");
        }
    }

    /**
     * Get videos with high SEO scores
     */
    public List<VideoSEOMetadataDto> getHighPerformingSEO(Double minScore) {
        try {
            List<VideoSEOMetadata> metadata = seoMetadataRepository.findBySeoScoreGreaterThanEqual(minScore);

            return metadata.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get high performing SEO videos", e);
            throw new BusinessException("Failed to retrieve high performing videos");
        }
    }

    /**
     * Get average SEO score across all videos
     */
    public Double getAverageSEOScore() {
        try {
            Double avgScore = seoMetadataRepository.getAverageSeoScore();
            return avgScore != null ? avgScore : 0.0;

        } catch (Exception e) {
            log.error("Failed to get average SEO score", e);
            throw new BusinessException("Failed to retrieve average SEO score");
        }
    }

    @Transactional
    private VideoSEOMetadataDto updateSEOMetadata(VideoSEOMetadata existing, VideoSEOMetadataDto metadataDto) {
        existing.setOptimizedTitle(metadataDto.getOptimizedTitle());
        existing.setOptimizedDescription(metadataDto.getOptimizedDescription());
        existing.setKeywords(metadataDto.getKeywords());
        existing.setHashtags(metadataDto.getHashtags());
        existing.setTargetAudience(metadataDto.getTargetAudience());
        existing.setContentCategory(metadataDto.getContentCategory());
        existing.setLanguage(metadataDto.getLanguage());
        existing.setTranscript(metadataDto.getTranscript());
        existing.setClosedCaptionsUrl(metadataDto.getClosedCaptionsUrl());
        existing.setThumbnailAltText(metadataDto.getThumbnailAltText());
        existing.setSchemaMarkup(metadataDto.getSchemaMarkup());
        existing.setCanonicalUrl(metadataDto.getCanonicalUrl());

        calculateSEOScores(existing);

        VideoSEOMetadata updated = seoMetadataRepository.save(existing);

        log.info("Updated SEO metadata for video job {}", existing.getVideoJob().getId());

        return mapToDto(updated);
    }

    private void calculateSEOScores(VideoSEOMetadata metadata) {
        // Calculate SEO score based on various factors
        double score = 0.0;

        // Title optimization (20 points)
        if (metadata.getOptimizedTitle() != null && !metadata.getOptimizedTitle().isEmpty()) {
            score += 20.0;
            if (metadata.getOptimizedTitle().length() >= 40 && metadata.getOptimizedTitle().length() <= 70) {
                score += 10.0; // Bonus for optimal length
            }
        }

        // Description (20 points)
        if (metadata.getOptimizedDescription() != null && metadata.getOptimizedDescription().length() > 100) {
            score += 20.0;
        }

        // Keywords (15 points)
        if (metadata.getKeywords() != null && !metadata.getKeywords().isEmpty()) {
            score += 15.0;
        }

        // Hashtags (10 points)
        if (metadata.getHashtags() != null && !metadata.getHashtags().isEmpty()) {
            score += 10.0;
        }

        // Transcript (15 points)
        if (metadata.getTranscript() != null && !metadata.getTranscript().isEmpty()) {
            score += 15.0;
        }

        // Closed captions (10 points)
        if (metadata.getClosedCaptionsUrl() != null && !metadata.getClosedCaptionsUrl().isEmpty()) {
            score += 10.0;
        }

        // Schema markup (10 points)
        if (metadata.getSchemaMarkup() != null && !metadata.getSchemaMarkup().isEmpty()) {
            score += 10.0;
        }

        metadata.setSeoScore(score);

        // Calculate keyword density
        if (metadata.getKeywords() != null && metadata.getOptimizedDescription() != null) {
            String[] keywords = metadata.getKeywords().split(",");
            String description = metadata.getOptimizedDescription().toLowerCase();
            long keywordCount = Arrays.stream(keywords)
                    .filter(keyword -> description.contains(keyword.toLowerCase().trim()))
                    .count();
            metadata.setKeywordDensity((double) keywordCount / keywords.length * 100);
        }

        // Calculate readability score (simplified)
        if (metadata.getOptimizedDescription() != null) {
            String[] words = metadata.getOptimizedDescription().split("\\s+");
            String[] sentences = metadata.getOptimizedDescription().split("[.!?]+");
            double avgWordsPerSentence = (double) words.length / Math.max(sentences.length, 1);
            // Flesch Reading Ease approximation
            metadata.setReadabilityScore(Math.max(0, 100 - avgWordsPerSentence * 2));
        }
    }

    private VideoSEOMetadataDto mapToDto(VideoSEOMetadata metadata) {
        return VideoSEOMetadataDto.builder()
                .id(metadata.getId())
                .videoJobId(metadata.getVideoJob().getId())
                .optimizedTitle(metadata.getOptimizedTitle())
                .optimizedDescription(metadata.getOptimizedDescription())
                .keywords(metadata.getKeywords())
                .hashtags(metadata.getHashtags())
                .targetAudience(metadata.getTargetAudience())
                .contentCategory(metadata.getContentCategory())
                .language(metadata.getLanguage())
                .transcript(metadata.getTranscript())
                .closedCaptionsUrl(metadata.getClosedCaptionsUrl())
                .thumbnailAltText(metadata.getThumbnailAltText())
                .schemaMarkup(metadata.getSchemaMarkup())
                .canonicalUrl(metadata.getCanonicalUrl())
                .seoScore(metadata.getSeoScore())
                .readabilityScore(metadata.getReadabilityScore())
                .keywordDensity(metadata.getKeywordDensity())
                .createdAt(metadata.getCreatedAt())
                .build();
    }
}
