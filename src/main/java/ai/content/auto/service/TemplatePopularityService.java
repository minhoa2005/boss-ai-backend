package ai.content.auto.service;

import ai.content.auto.dtos.TemplatePerformanceSummary;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for calculating and managing template popularity scores
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplatePopularityService {

    private final ContentTemplateRepository templateRepository;
    private final TemplateUsageLogRepository usageLogRepository;

    // Weights for popularity score calculation
    private static final double USAGE_COUNT_WEIGHT = 0.40;
    private static final double RATING_WEIGHT = 0.30;
    private static final double SUCCESS_RATE_WEIGHT = 0.20;
    private static final double RECENCY_WEIGHT = 0.10;

    /**
     * Calculate popularity score for a template
     * Score is based on: usage count (40%), average rating (30%), success rate
     * (20%), recency (10%)
     */
    public double calculatePopularityScore(ContentTemplate template) {
        try {
            // Normalize usage count (0-100 scale, assuming max 1000 uses)
            double normalizedUsage = Math.min(template.getUsageCount() / 10.0, 100.0);

            // Normalize rating (0-100 scale, assuming 5-star rating)
            double normalizedRating = template.getAverageRating() != null
                    ? template.getAverageRating().doubleValue() * 20.0
                    : 0.0;

            // Success rate is already in percentage (0-100)
            double successRate = template.getSuccessRate() != null
                    ? template.getSuccessRate().doubleValue()
                    : 0.0;

            // Calculate recency score based on last 30 days usage
            double recencyScore = calculateRecencyScore(template.getId());

            // Calculate weighted popularity score
            double popularityScore = (normalizedUsage * USAGE_COUNT_WEIGHT)
                    + (normalizedRating * RATING_WEIGHT)
                    + (successRate * SUCCESS_RATE_WEIGHT)
                    + (recencyScore * RECENCY_WEIGHT);

            log.debug("Calculated popularity score for template {}: {} " +
                    "(usage: {}, rating: {}, success: {}, recency: {})",
                    template.getId(), popularityScore, normalizedUsage, normalizedRating,
                    successRate, recencyScore);

            return Math.round(popularityScore * 100.0) / 100.0; // Round to 2 decimal places
        } catch (Exception e) {
            log.error("Error calculating popularity score for template: {}", template.getId(), e);
            return 0.0;
        }
    }

    /**
     * Calculate recency score based on usage in last 30 days
     * Returns a score from 0-100 based on recent activity
     */
    private double calculateRecencyScore(Long templateId) {
        try {
            OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
            OffsetDateTime now = OffsetDateTime.now();

            Long recentUsageCount = usageLogRepository.countUsageByTemplateInDateRange(
                    templateId, thirtyDaysAgo, now);

            // Normalize to 0-100 scale (assuming max 100 uses in 30 days is excellent)
            return Math.min(recentUsageCount.doubleValue(), 100.0);
        } catch (Exception e) {
            log.error("Error calculating recency score for template: {}", templateId, e);
            return 0.0;
        }
    }

    /**
     * Update template metrics from usage logs
     * This should be called periodically or after significant usage
     */
    @Transactional
    public void updateTemplateMetrics(Long templateId) {
        try {
            ContentTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

            log.info("Updating metrics for template: {}", templateId);

            // Update average generation time
            Double avgGenerationTime = usageLogRepository.getAverageGenerationTimeByTemplate(templateId);
            if (avgGenerationTime != null) {
                template.setAverageGenerationTimeMs(avgGenerationTime.longValue());
            }

            // Update average quality score (if available)
            Double avgQualityScore = usageLogRepository.getAverageQualityScoreByTemplate(templateId);
            if (avgQualityScore != null) {
                template.setAverageRating(BigDecimal.valueOf(avgQualityScore)
                        .setScale(2, RoundingMode.HALF_UP));
            }

            // Update success rate
            Double successRate = usageLogRepository.getSuccessRateByTemplate(templateId);
            if (successRate != null) {
                template.setSuccessRate(BigDecimal.valueOf(successRate)
                        .setScale(2, RoundingMode.HALF_UP));
            }

            templateRepository.save(template);

            log.info("Template metrics updated successfully for template: {}", templateId);
        } catch (Exception e) {
            log.error("Error updating template metrics for template: {}", templateId, e);
            // Don't throw exception to avoid breaking batch operations
        }
    }

    /**
     * Update metrics for all active templates
     * This should be run as a scheduled job
     */
    @Transactional
    public void updateAllTemplateMetrics() {
        try {
            log.info("Starting batch update of template metrics");

            List<ContentTemplate> activeTemplates = templateRepository.findByStatus("ACTIVE");
            int updatedCount = 0;

            for (ContentTemplate template : activeTemplates) {
                try {
                    updateTemplateMetrics(template.getId());
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to update metrics for template: {}", template.getId(), e);
                    // Continue with next template
                }
            }

            log.info("Batch update completed. Updated {} out of {} templates",
                    updatedCount, activeTemplates.size());
        } catch (Exception e) {
            log.error("Error during batch template metrics update", e);
        }
    }

    /**
     * Get trending templates based on recent usage
     * Returns templates with highest usage in the last 7 days
     */
    public List<Long> getTrendingTemplateIds(int limit) {
        try {
            OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
            List<Object[]> results = usageLogRepository.findMostPopularTemplates(
                    sevenDaysAgo,
                    org.springframework.data.domain.PageRequest.of(0, limit));

            return results.stream()
                    .map(row -> (Long) row[0])
                    .toList();
        } catch (Exception e) {
            log.error("Error getting trending templates", e);
            return List.of();
        }
    }

    /**
     * Calculate and update popularity score for a template
     */
    @Transactional
    public void updatePopularityScore(Long templateId) {
        try {
            ContentTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

            double popularityScore = calculatePopularityScore(template);

            // Store popularity score in a custom field or use it for sorting
            // For now, we'll log it and it can be calculated on-demand
            log.info("Popularity score for template {}: {}", templateId, popularityScore);

            // If you want to persist the score, add a field to ContentTemplate entity
            // template.setPopularityScore(BigDecimal.valueOf(popularityScore));
            // templateRepository.save(template);
        } catch (Exception e) {
            log.error("Error updating popularity score for template: {}", templateId, e);
        }
    }

    /**
     * Get template performance summary
     */
    public TemplatePerformanceSummary getTemplatePerformance(Long templateId) {
        try {
            ContentTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

            double popularityScore = calculatePopularityScore(template);
            double recencyScore = calculateRecencyScore(templateId);

            OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
            Long recentUsageCount = usageLogRepository.countUsageByTemplateInDateRange(
                    templateId, thirtyDaysAgo, OffsetDateTime.now());

            return TemplatePerformanceSummary.builder()
                    .templateId(templateId)
                    .templateName(template.getName())
                    .totalUsageCount(template.getUsageCount())
                    .recentUsageCount(recentUsageCount.intValue())
                    .averageRating(template.getAverageRating())
                    .successRate(template.getSuccessRate())
                    .popularityScore(BigDecimal.valueOf(popularityScore))
                    .recencyScore(BigDecimal.valueOf(recencyScore))
                    .averageGenerationTimeMs(template.getAverageGenerationTimeMs())
                    .build();
        } catch (Exception e) {
            log.error("Error getting template performance for template: {}", templateId, e);
            return null;
        }
    }
}
