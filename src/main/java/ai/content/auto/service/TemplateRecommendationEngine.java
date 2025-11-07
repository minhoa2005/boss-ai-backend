package ai.content.auto.service;

import ai.content.auto.dtos.TemplateRecommendation;
import ai.content.auto.dtos.UserBehaviorProfile;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.TemplateUsageLog;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Template Recommendation Engine
 * 
 * Analyzes user behavior patterns to recommend the most relevant templates.
 * Uses collaborative filtering and content-based filtering approaches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateRecommendationEngine {

    private final ContentTemplateRepository templateRepository;
    private final TemplateUsageLogRepository usageLogRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final TemplatePopularityService popularityService;

    // Recommendation weights
    private static final double USER_HISTORY_WEIGHT = 0.35;
    private static final double COLLABORATIVE_FILTERING_WEIGHT = 0.25;
    private static final double CONTENT_SIMILARITY_WEIGHT = 0.20;
    private static final double POPULARITY_WEIGHT = 0.15;
    private static final double RECENCY_WEIGHT = 0.05;

    /**
     * Get personalized template recommendations for a user
     * 
     * @param userId User ID
     * @param limit  Maximum number of recommendations
     * @return List of recommended template IDs with scores
     */
    public List<TemplateRecommendation> getRecommendationsForUser(Long userId, int limit) {
        try {
            log.info("Generating template recommendations for user: {}", userId);

            return getRecommendationsInTransaction(userId, limit);
        } catch (Exception e) {
            log.error("Error generating recommendations for user: {}", userId, e);
            // Fallback to popular templates
            return getFallbackRecommendations(userId, limit);
        }
    }

    @Transactional(readOnly = true)
    private List<TemplateRecommendation> getRecommendationsInTransaction(Long userId, int limit) {
        // Get user's template usage history
        UserBehaviorProfile userProfile = buildUserBehaviorProfile(userId);

        // Get all active templates
        List<ContentTemplate> allTemplates = templateRepository.findByStatus("ACTIVE");

        // Calculate recommendation scores for each template
        Map<Long, Double> templateScores = new HashMap<>();

        for (ContentTemplate template : allTemplates) {
            // Skip templates user has used recently (last 7 days)
            if (userProfile.getRecentlyUsedTemplateIds().contains(template.getId())) {
                continue;
            }

            double score = calculateRecommendationScore(template, userProfile, userId);
            templateScores.put(template.getId(), score);
        }

        // Sort by score and return top N
        return templateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new TemplateRecommendation(
                        entry.getKey(),
                        entry.getValue(),
                        determineRecommendationReason(entry.getKey(), userProfile)))
                .collect(Collectors.toList());
    }

    /**
     * Calculate recommendation score for a template
     */
    private double calculateRecommendationScore(ContentTemplate template, UserBehaviorProfile userProfile,
            Long userId) {
        double userHistoryScore = calculateUserHistoryScore(template, userProfile);
        double collaborativeScore = calculateCollaborativeFilteringScore(template, userProfile);
        double contentSimilarityScore = calculateContentSimilarityScore(template, userProfile);
        double popularityScore = calculatePopularityScore(template);
        double recencyScore = calculateRecencyScore(template);

        return (userHistoryScore * USER_HISTORY_WEIGHT) +
                (collaborativeScore * COLLABORATIVE_FILTERING_WEIGHT) +
                (contentSimilarityScore * CONTENT_SIMILARITY_WEIGHT) +
                (popularityScore * POPULARITY_WEIGHT) +
                (recencyScore * RECENCY_WEIGHT);
    }

    /**
     * Score based on user's historical preferences
     */
    private double calculateUserHistoryScore(ContentTemplate template, UserBehaviorProfile userProfile) {
        double score = 0.0;

        // Match industry preference
        if (userProfile.getPreferredIndustries().contains(template.getIndustry())) {
            score += 30.0;
        }

        // Match content type preference
        if (userProfile.getPreferredContentTypes().contains(template.getContentType())) {
            score += 25.0;
        }

        // Match tone preference
        if (userProfile.getPreferredTones().contains(template.getTone())) {
            score += 20.0;
        }

        // Match language preference
        if (userProfile.getPreferredLanguages().contains(template.getLanguage())) {
            score += 15.0;
        }

        // Match target audience
        if (userProfile.getPreferredAudiences().contains(template.getTargetAudience())) {
            score += 10.0;
        }

        return score;
    }

    /**
     * Score based on similar users' preferences (collaborative filtering)
     */
    private double calculateCollaborativeFilteringScore(ContentTemplate template, UserBehaviorProfile userProfile) {
        // Find users with similar behavior patterns
        List<Long> similarUserIds = findSimilarUsers(userProfile);

        if (similarUserIds.isEmpty()) {
            return 0.0;
        }

        // Count how many similar users have used this template successfully
        long successfulUsages = usageLogRepository.countByTemplateIdAndUserIdInAndWasSuccessful(
                template.getId(), similarUserIds, true);

        // Normalize to 0-100 scale
        return Math.min(100.0, (successfulUsages / (double) similarUserIds.size()) * 100.0);
    }

    /**
     * Score based on content similarity to user's successful generations
     */
    private double calculateContentSimilarityScore(ContentTemplate template, UserBehaviorProfile userProfile) {
        double score = 0.0;

        // Check if template category matches user's successful content types
        if (userProfile.getSuccessfulContentTypes().contains(template.getContentType())) {
            score += 40.0;
        }

        // Check if template industry matches user's successful industries
        if (userProfile.getSuccessfulIndustries().contains(template.getIndustry())) {
            score += 30.0;
        }

        // Check if template tone matches user's successful tones
        if (userProfile.getSuccessfulTones().contains(template.getTone())) {
            score += 30.0;
        }

        return score;
    }

    /**
     * Score based on template popularity
     */
    private double calculatePopularityScore(ContentTemplate template) {
        try {
            return popularityService.calculatePopularityScore(template);
        } catch (Exception e) {
            log.warn("Failed to calculate popularity score for template: {}", template.getId(), e);
            return 0.0;
        }
    }

    /**
     * Score based on template recency (newer templates get slight boost)
     */
    private double calculateRecencyScore(ContentTemplate template) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime createdAt = template.getCreatedAt();

        if (createdAt == null) {
            return 0.0;
        }

        long daysSinceCreation = java.time.Duration.between(createdAt, now).toDays();

        // Templates created in last 30 days get higher score
        if (daysSinceCreation <= 30) {
            return 100.0 - (daysSinceCreation * 3.0); // Decreases from 100 to 10
        } else if (daysSinceCreation <= 90) {
            return 10.0; // Stable score for 1-3 months old
        } else {
            return 5.0; // Lower score for older templates
        }
    }

    /**
     * Build user behavior profile from historical data
     */
    @Transactional(readOnly = true)
    private UserBehaviorProfile buildUserBehaviorProfile(Long userId) {
        UserBehaviorProfile profile = new UserBehaviorProfile();
        profile.setUserId(userId);

        // Get user's template usage history (last 90 days)
        OffsetDateTime since = OffsetDateTime.now().minusDays(90);
        List<TemplateUsageLog> usageLogs = usageLogRepository.findByUserIdAndUsedAtAfter(userId, since);

        // Get recently used template IDs (last 7 days)
        OffsetDateTime recentSince = OffsetDateTime.now().minusDays(7);
        Set<Long> recentTemplateIds = usageLogRepository.findByUserIdAndUsedAtAfter(userId, recentSince)
                .stream()
                .map(log -> log.getTemplate().getId())
                .collect(Collectors.toSet());
        profile.setRecentlyUsedTemplateIds(recentTemplateIds);

        // Analyze usage patterns
        Map<String, Integer> industryCount = new HashMap<>();
        Map<String, Integer> contentTypeCount = new HashMap<>();
        Map<String, Integer> toneCount = new HashMap<>();
        Map<String, Integer> languageCount = new HashMap<>();
        Map<String, Integer> audienceCount = new HashMap<>();

        for (TemplateUsageLog log : usageLogs) {
            ContentTemplate template = log.getTemplate();

            // Count preferences
            incrementCount(industryCount, template.getIndustry());
            incrementCount(contentTypeCount, template.getContentType());
            incrementCount(toneCount, template.getTone());
            incrementCount(languageCount, template.getLanguage());
            incrementCount(audienceCount, template.getTargetAudience());

            // Track successful generations
            if (Boolean.TRUE.equals(log.getWasSuccessful())) {
                profile.getSuccessfulIndustries().add(template.getIndustry());
                profile.getSuccessfulContentTypes().add(template.getContentType());
                profile.getSuccessfulTones().add(template.getTone());
            }
        }

        // Set preferred attributes (top 3 most used)
        profile.setPreferredIndustries(getTopKeys(industryCount, 3));
        profile.setPreferredContentTypes(getTopKeys(contentTypeCount, 3));
        profile.setPreferredTones(getTopKeys(toneCount, 3));
        profile.setPreferredLanguages(getTopKeys(languageCount, 3));
        profile.setPreferredAudiences(getTopKeys(audienceCount, 3));

        // Get user's content generation history
        Instant sinceInstant = since.toInstant();
        List<ContentGeneration> generations = contentGenerationRepository
                .findByUserIdAndCreatedAtAfter(userId, sinceInstant);
        profile.setTotalGenerations(generations.size());

        // Calculate average quality score
        double avgQuality = generations.stream()
                .filter(g -> g.getQualityScore() != null)
                .mapToDouble(g -> g.getQualityScore().doubleValue())
                .average()
                .orElse(0.0);
        profile.setAverageQualityScore(avgQuality);

        return profile;
    }

    /**
     * Find users with similar behavior patterns
     */
    @Transactional(readOnly = true)
    private List<Long> findSimilarUsers(UserBehaviorProfile userProfile) {
        // Get all users who have used similar templates
        Set<String> userIndustries = new HashSet<>(userProfile.getPreferredIndustries());
        Set<String> userContentTypes = new HashSet<>(userProfile.getPreferredContentTypes());

        if (userIndustries.isEmpty() && userContentTypes.isEmpty()) {
            return Collections.emptyList();
        }

        // Find users with overlapping preferences
        OffsetDateTime since = OffsetDateTime.now().minusDays(90);
        List<TemplateUsageLog> similarUsageLogs = usageLogRepository.findByUsedAtAfter(since);

        Map<Long, Integer> userSimilarityScores = new HashMap<>();

        for (TemplateUsageLog log : similarUsageLogs) {
            Long otherUserId = log.getUser().getId();

            // Skip the current user
            if (otherUserId.equals(userProfile.getUserId())) {
                continue;
            }

            ContentTemplate template = log.getTemplate();
            int similarityScore = 0;

            if (userIndustries.contains(template.getIndustry())) {
                similarityScore += 2;
            }
            if (userContentTypes.contains(template.getContentType())) {
                similarityScore += 3;
            }

            userSimilarityScores.merge(otherUserId, similarityScore, Integer::sum);
        }

        // Return top 10 most similar users
        return userSimilarityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Determine why a template is being recommended
     */
    private String determineRecommendationReason(Long templateId, UserBehaviorProfile userProfile) {
        ContentTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            return "Popular template";
        }

        List<String> reasons = new ArrayList<>();

        if (userProfile.getPreferredContentTypes().contains(template.getContentType())) {
            reasons.add("Matches your preferred content type");
        }
        if (userProfile.getPreferredIndustries().contains(template.getIndustry())) {
            reasons.add("Matches your industry");
        }
        if (userProfile.getSuccessfulContentTypes().contains(template.getContentType())) {
            reasons.add("Similar to your successful content");
        }
        if (template.getUsageCount() > 100) {
            reasons.add("Popular with other users");
        }

        return reasons.isEmpty() ? "Recommended for you" : String.join(", ", reasons);
    }

    /**
     * Get fallback recommendations (popular templates)
     */
    private List<TemplateRecommendation> getFallbackRecommendations(Long userId, int limit) {
        try {
            List<Long> trendingIds = popularityService.getTrendingTemplateIds(limit);
            return trendingIds.stream()
                    .map(id -> new TemplateRecommendation(id, 50.0, "Popular template"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get fallback recommendations", e);
            return Collections.emptyList();
        }
    }

    // Helper methods

    private void incrementCount(Map<String, Integer> map, String key) {
        if (key != null && !key.isEmpty()) {
            map.merge(key, 1, Integer::sum);
        }
    }

    private List<String> getTopKeys(Map<String, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
