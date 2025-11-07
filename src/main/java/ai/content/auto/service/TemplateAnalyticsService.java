package ai.content.auto.service;

import ai.content.auto.dtos.TemplateAnalyticsReport;
import ai.content.auto.dtos.TemplateOptimizationSuggestion;
import ai.content.auto.dtos.TemplatePerformanceMetrics;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.TemplateUsageLog;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for template analytics and performance tracking
 * Provides insights into template usage, success rates, and optimization
 * opportunities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateAnalyticsService {

    private final ContentTemplateRepository templateRepository;
    private final TemplateUsageLogRepository usageLogRepository;
    private final TemplatePopularityService popularityService;

    // Performance thresholds
    private static final double MIN_SUCCESS_RATE = 70.0;
    private static final double MIN_AVERAGE_RATING = 3.5;
    private static final int MIN_USAGE_COUNT_FOR_ANALYSIS = 10;
    private static final long MAX_GENERATION_TIME_MS = 30000; // 30 seconds

    /**
     * Get comprehensive analytics report for a template
     */
    public TemplateAnalyticsReport getTemplateAnalytics(Long templateId) {
        try {
            log.info("Generating analytics report for template: {}", templateId);

            return getTemplateAnalyticsInTransaction(templateId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating analytics for template: {}", templateId, e);
            throw new BusinessException("Failed to generate template analytics");
        }
    }

    @Transactional(readOnly = true)
    private TemplateAnalyticsReport getTemplateAnalyticsInTransaction(Long templateId) {
        ContentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found"));

        // Get performance metrics
        TemplatePerformanceMetrics metrics = calculatePerformanceMetrics(template);

        // Get usage trends
        Map<String, Integer> usageTrends = calculateUsageTrends(templateId);

        // Get optimization suggestions
        List<TemplateOptimizationSuggestion> suggestions = generateOptimizationSuggestions(template, metrics);

        // Get user satisfaction metrics
        Map<String, Object> satisfactionMetrics = calculateSatisfactionMetrics(templateId);

        return TemplateAnalyticsReport.builder()
                .templateId(templateId)
                .templateName(template.getName())
                .performanceMetrics(metrics)
                .usageTrends(usageTrends)
                .optimizationSuggestions(suggestions)
                .satisfactionMetrics(satisfactionMetrics)
                .generatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Calculate detailed performance metrics for a template
     */
    private TemplatePerformanceMetrics calculatePerformanceMetrics(ContentTemplate template) {
        Long templateId = template.getId();

        // Get usage statistics
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);

        Long totalUsage = (long) template.getUsageCount();
        Long last30DaysUsage = usageLogRepository.countUsageByTemplateInDateRange(
                templateId, thirtyDaysAgo, OffsetDateTime.now());
        Long last7DaysUsage = usageLogRepository.countUsageByTemplateInDateRange(
                templateId, sevenDaysAgo, OffsetDateTime.now());

        // Calculate success rate
        Double successRate = template.getSuccessRate() != null
                ? template.getSuccessRate().doubleValue()
                : calculateSuccessRate(templateId);

        // Calculate average generation time
        Long avgGenerationTime = template.getAverageGenerationTimeMs() != null
                ? template.getAverageGenerationTimeMs()
                : calculateAverageGenerationTime(templateId);

        // Calculate quality metrics
        Double avgQualityScore = calculateAverageQualityScore(templateId);
        Double avgRating = template.getAverageRating() != null
                ? template.getAverageRating().doubleValue()
                : 0.0;

        // Calculate engagement metrics
        Double completionRate = calculateCompletionRate(templateId);
        Double retryRate = calculateRetryRate(templateId);

        // Calculate popularity score
        double popularityScore = popularityService.calculatePopularityScore(template);

        return TemplatePerformanceMetrics.builder()
                .totalUsageCount(totalUsage.intValue())
                .last30DaysUsage(last30DaysUsage.intValue())
                .last7DaysUsage(last7DaysUsage.intValue())
                .successRate(BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP))
                .averageGenerationTimeMs(avgGenerationTime)
                .averageQualityScore(BigDecimal.valueOf(avgQualityScore).setScale(2, RoundingMode.HALF_UP))
                .averageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP))
                .completionRate(BigDecimal.valueOf(completionRate).setScale(2, RoundingMode.HALF_UP))
                .retryRate(BigDecimal.valueOf(retryRate).setScale(2, RoundingMode.HALF_UP))
                .popularityScore(BigDecimal.valueOf(popularityScore).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * Calculate success rate for a template
     */
    private Double calculateSuccessRate(Long templateId) {
        try {
            Long totalUsage = usageLogRepository.countByTemplateId(templateId);
            if (totalUsage == 0) {
                return 0.0;
            }

            Long successfulUsage = usageLogRepository.countByTemplateIdAndWasSuccessful(templateId, true);
            return (successfulUsage.doubleValue() / totalUsage.doubleValue()) * 100.0;
        } catch (Exception e) {
            log.error("Error calculating success rate for template: {}", templateId, e);
            return 0.0;
        }
    }

    /**
     * Calculate average generation time
     */
    private Long calculateAverageGenerationTime(Long templateId) {
        try {
            Double avgTime = usageLogRepository.getAverageGenerationTimeByTemplate(templateId);
            return avgTime != null ? avgTime.longValue() : 0L;
        } catch (Exception e) {
            log.error("Error calculating average generation time for template: {}", templateId, e);
            return 0L;
        }
    }

    /**
     * Calculate average quality score
     */
    private Double calculateAverageQualityScore(Long templateId) {
        try {
            Double avgScore = usageLogRepository.getAverageQualityScoreByTemplate(templateId);
            return avgScore != null ? avgScore : 0.0;
        } catch (Exception e) {
            log.error("Error calculating average quality score for template: {}", templateId, e);
            return 0.0;
        }
    }

    /**
     * Calculate completion rate (percentage of generations that were successful)
     * Using wasSuccessful as a proxy for completion
     */
    private Double calculateCompletionRate(Long templateId) {
        try {
            Long totalUsage = usageLogRepository.countByTemplateId(templateId);
            if (totalUsage == 0) {
                return 100.0;
            }

            Long successfulUsage = usageLogRepository.countByTemplateIdAndWasSuccessful(templateId, true);
            return (successfulUsage.doubleValue() / totalUsage.doubleValue()) * 100.0;
        } catch (Exception e) {
            log.error("Error calculating completion rate for template: {}", templateId, e);
            return 100.0;
        }
    }

    /**
     * Calculate retry rate based on failed attempts
     * This is an approximation since we don't have explicit retry tracking
     */
    private Double calculateRetryRate(Long templateId) {
        try {
            Long totalUsage = usageLogRepository.countByTemplateId(templateId);
            if (totalUsage == 0) {
                return 0.0;
            }

            // Count failed attempts as potential retries
            Long failedUsage = usageLogRepository.countByTemplateIdAndWasSuccessful(templateId, false);

            // Estimate retry rate as a percentage of failed attempts
            return (failedUsage.doubleValue() / totalUsage.doubleValue()) * 100.0;
        } catch (Exception e) {
            log.error("Error calculating retry rate for template: {}", templateId, e);
            return 0.0;
        }
    }

    /**
     * Calculate usage trends over time
     */
    private Map<String, Integer> calculateUsageTrends(Long templateId) {
        Map<String, Integer> trends = new HashMap<>();

        try {
            OffsetDateTime now = OffsetDateTime.now();

            // Last 7 days
            for (int i = 6; i >= 0; i--) {
                OffsetDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0);
                OffsetDateTime dayEnd = dayStart.plusDays(1);

                Long count = usageLogRepository.countUsageByTemplateInDateRange(templateId, dayStart, dayEnd);
                trends.put(dayStart.toLocalDate().toString(), count.intValue());
            }
        } catch (Exception e) {
            log.error("Error calculating usage trends for template: {}", templateId, e);
        }

        return trends;
    }

    /**
     * Generate optimization suggestions based on template performance
     */
    private List<TemplateOptimizationSuggestion> generateOptimizationSuggestions(
            ContentTemplate template, TemplatePerformanceMetrics metrics) {

        List<TemplateOptimizationSuggestion> suggestions = new ArrayList<>();

        // Check if template has enough data for analysis
        if (metrics.getTotalUsageCount() < MIN_USAGE_COUNT_FOR_ANALYSIS) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("DATA_COLLECTION")
                    .priority("LOW")
                    .title("Insufficient Usage Data")
                    .description("Template needs more usage data for meaningful analysis. Current usage: "
                            + metrics.getTotalUsageCount() + ", minimum required: " + MIN_USAGE_COUNT_FOR_ANALYSIS)
                    .impact("INFORMATIONAL")
                    .build());
            return suggestions;
        }

        // Check success rate
        if (metrics.getSuccessRate().doubleValue() < MIN_SUCCESS_RATE) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("SUCCESS_RATE")
                    .priority("HIGH")
                    .title("Low Success Rate")
                    .description(String.format("Template success rate (%.1f%%) is below threshold (%.1f%%). " +
                            "Consider reviewing prompt template and default parameters.",
                            metrics.getSuccessRate().doubleValue(), MIN_SUCCESS_RATE))
                    .impact("HIGH")
                    .recommendation("Review and optimize prompt template, adjust default parameters, " +
                            "or add more specific instructions.")
                    .build());
        }

        // Check average rating
        if (metrics.getAverageRating().doubleValue() < MIN_AVERAGE_RATING
                && metrics.getAverageRating().doubleValue() > 0) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("USER_SATISFACTION")
                    .priority("MEDIUM")
                    .title("Low User Satisfaction")
                    .description(String.format("Template average rating (%.1f/5.0) is below threshold (%.1f/5.0). " +
                            "Users may not be satisfied with generated content quality.",
                            metrics.getAverageRating().doubleValue(), MIN_AVERAGE_RATING))
                    .impact("MEDIUM")
                    .recommendation("Gather user feedback, analyze low-rated generations, " +
                            "and refine template based on common issues.")
                    .build());
        }

        // Check generation time
        if (metrics.getAverageGenerationTimeMs() > MAX_GENERATION_TIME_MS) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("PERFORMANCE")
                    .priority("MEDIUM")
                    .title("Slow Generation Time")
                    .description(String.format("Average generation time (%.1fs) exceeds threshold (%.1fs). " +
                            "This may impact user experience.",
                            metrics.getAverageGenerationTimeMs() / 1000.0, MAX_GENERATION_TIME_MS / 1000.0))
                    .impact("MEDIUM")
                    .recommendation("Simplify prompt template, reduce token count, " +
                            "or optimize AI provider selection.")
                    .build());
        }

        // Check retry rate
        if (metrics.getRetryRate().doubleValue() > 20.0) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("RELIABILITY")
                    .priority("HIGH")
                    .title("High Retry Rate")
                    .description(String.format("Template has high retry rate (%.1f%%). " +
                            "Users frequently need to regenerate content.",
                            metrics.getRetryRate().doubleValue()))
                    .impact("HIGH")
                    .recommendation("Investigate common failure patterns, improve prompt clarity, " +
                            "and add better error handling.")
                    .build());
        }

        // Check completion rate
        if (metrics.getCompletionRate().doubleValue() < 80.0) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("ENGAGEMENT")
                    .priority("MEDIUM")
                    .title("Low Completion Rate")
                    .description(String.format("Template has low completion rate (%.1f%%). " +
                            "Users may be abandoning generations.",
                            metrics.getCompletionRate().doubleValue()))
                    .impact("MEDIUM")
                    .recommendation("Reduce generation time, improve initial results quality, " +
                            "or add progress indicators.")
                    .build());
        }

        // Check usage trends
        if (metrics.getLast7DaysUsage() == 0 && metrics.getTotalUsageCount() > 20) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("POPULARITY")
                    .priority("LOW")
                    .title("Declining Usage")
                    .description("Template has not been used in the last 7 days despite previous usage. " +
                            "It may be outdated or replaced by better alternatives.")
                    .impact("LOW")
                    .recommendation("Review template relevance, update content to current trends, " +
                            "or consider archiving if no longer needed.")
                    .build());
        }

        // Check for high performance templates
        if (metrics.getSuccessRate().doubleValue() >= 90.0
                && metrics.getAverageRating().doubleValue() >= 4.5
                && metrics.getTotalUsageCount() >= 50) {
            suggestions.add(TemplateOptimizationSuggestion.builder()
                    .type("BEST_PRACTICE")
                    .priority("LOW")
                    .title("High-Performing Template")
                    .description("This template shows excellent performance metrics. " +
                            "Consider using it as a reference for creating similar templates.")
                    .impact("POSITIVE")
                    .recommendation("Document what makes this template successful and apply " +
                            "similar patterns to other templates.")
                    .build());
        }

        return suggestions;
    }

    /**
     * Calculate user satisfaction metrics
     */
    private Map<String, Object> calculateSatisfactionMetrics(Long templateId) {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Get rating distribution
            List<Object[]> ratingResults = usageLogRepository
                    .getRatingDistributionByTemplate(templateId);

            // Convert to map
            Map<Integer, Long> ratingDistribution = new HashMap<>();
            for (Object[] result : ratingResults) {
                Integer rating = (Integer) result[0];
                Long count = (Long) result[1];
                ratingDistribution.put(rating, count);
            }
            metrics.put("ratingDistribution", ratingDistribution);

            // Calculate Net Promoter Score (NPS) - ratings 4-5 are promoters, 1-2 are
            // detractors
            long promoters = ratingDistribution.getOrDefault(5, 0L) + ratingDistribution.getOrDefault(4, 0L);
            long detractors = ratingDistribution.getOrDefault(1, 0L) + ratingDistribution.getOrDefault(2, 0L);
            long total = ratingDistribution.values().stream().mapToLong(Long::longValue).sum();

            if (total > 0) {
                double nps = ((promoters - detractors) / (double) total) * 100.0;
                metrics.put("netPromoterScore", Math.round(nps * 100.0) / 100.0);
            } else {
                metrics.put("netPromoterScore", 0.0);
            }

            // Get user feedback summary
            List<String> commonIssues = identifyCommonIssues(templateId);
            metrics.put("commonIssues", commonIssues);

        } catch (Exception e) {
            log.error("Error calculating satisfaction metrics for template: {}", templateId, e);
        }

        return metrics;
    }

    /**
     * Identify common issues from usage logs
     */
    private List<String> identifyCommonIssues(Long templateId) {
        List<String> issues = new ArrayList<>();

        try {
            // Get failed usage logs
            List<TemplateUsageLog> failedLogs = usageLogRepository
                    .findByTemplateIdAndWasSuccessful(templateId, false);

            // Analyze feedback patterns for common issues
            Map<String, Integer> issuePatterns = new HashMap<>();

            for (TemplateUsageLog log : failedLogs) {
                // Analyze user feedback if available
                if (log.getUserFeedback() != null && !log.getUserFeedback().isEmpty()) {
                    String issueCategory = categorizeIssue(log.getUserFeedback());
                    issuePatterns.merge(issueCategory, 1, Integer::sum);
                } else {
                    // If no feedback, categorize as "Unknown Issue"
                    issuePatterns.merge("Unknown Issue", 1, Integer::sum);
                }
            }

            // Return top 3 issues
            issues = issuePatterns.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> entry.getKey() + " (" + entry.getValue() + " occurrences)")
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error identifying common issues for template: {}", templateId, e);
        }

        return issues;
    }

    /**
     * Categorize issues from user feedback
     */
    private String categorizeIssue(String feedback) {
        String lowerFeedback = feedback.toLowerCase();

        if (lowerFeedback.contains("slow") || lowerFeedback.contains("timeout") || lowerFeedback.contains("long")) {
            return "Performance Issues";
        } else if (lowerFeedback.contains("quality") || lowerFeedback.contains("poor")
                || lowerFeedback.contains("bad")) {
            return "Quality Issues";
        } else if (lowerFeedback.contains("error") || lowerFeedback.contains("fail")
                || lowerFeedback.contains("broken")) {
            return "Technical Errors";
        } else if (lowerFeedback.contains("confusing") || lowerFeedback.contains("unclear")
                || lowerFeedback.contains("difficult")) {
            return "Usability Issues";
        } else if (lowerFeedback.contains("irrelevant") || lowerFeedback.contains("wrong")
                || lowerFeedback.contains("incorrect")) {
            return "Content Relevance Issues";
        } else {
            return "Other Issues";
        }
    }

    /**
     * Get comparative analytics for multiple templates
     */
    public List<TemplateAnalyticsReport> getComparativeAnalytics(List<Long> templateIds) {
        try {
            log.info("Generating comparative analytics for {} templates", templateIds.size());

            return templateIds.stream()
                    .map(this::getTemplateAnalytics)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error generating comparative analytics", e);
            throw new BusinessException("Failed to generate comparative analytics");
        }
    }
}
