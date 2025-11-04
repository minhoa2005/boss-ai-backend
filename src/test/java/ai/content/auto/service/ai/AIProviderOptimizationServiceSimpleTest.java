package ai.content.auto.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test for AI Provider Optimization Service DTOs
 */
class AIProviderOptimizationServiceSimpleTest {

    @Test
    void testProviderSelectionCriteriaBuilder() {
        // Test that we can build criteria objects
        ProviderSelectionCriteria criteria = ProviderSelectionCriteria.builder()
                .contentType("blog")
                .language("en")
                .prioritizeQuality(true)
                .prioritizeCost(false)
                .maxCostPerToken(0.001)
                .minQualityScore(7.0)
                .build();

        assertNotNull(criteria);
        assertEquals("blog", criteria.getContentType());
        assertEquals("en", criteria.getLanguage());
        assertTrue(criteria.getPrioritizeQuality());
        assertFalse(criteria.getPrioritizeCost());
        assertEquals(0.001, criteria.getMaxCostPerToken());
        assertEquals(7.0, criteria.getMinQualityScore());
    }

    @Test
    void testProviderRecommendationBuilder() {
        // Test that we can build recommendation objects
        ProviderRecommendation recommendation = ProviderRecommendation.builder()
                .primaryScore(0.85)
                .optimizationReason("Test reason")
                .confidence(0.9)
                .build();

        assertNotNull(recommendation);
        assertEquals(0.85, recommendation.getPrimaryScore());
        assertEquals("Test reason", recommendation.getOptimizationReason());
        assertEquals(0.9, recommendation.getConfidence());
    }

    @Test
    void testProviderScoreBuilder() {
        // Test that we can build score objects
        ProviderScore score = ProviderScore.builder()
                .totalScore(0.75)
                .costScore(0.8)
                .qualityScore(0.7)
                .availabilityScore(0.9)
                .responseTimeScore(0.6)
                .loadScore(0.8)
                .build();

        assertNotNull(score);
        assertEquals(0.75, score.getTotalScore());
        assertEquals(0.8, score.getCostScore());
        assertEquals(0.7, score.getQualityScore());
        assertEquals(0.9, score.getAvailabilityScore());
        assertEquals(0.6, score.getResponseTimeScore());
        assertEquals(0.8, score.getLoadScore());
    }

    @Test
    void testCostOptimizationRecommendationBuilder() {
        // Test that we can build cost optimization objects
        CostOptimizationRecommendation recommendation = CostOptimizationRecommendation.builder()
                .currentAverageCost(0.005)
                .cheapestProvider("TestProvider")
                .cheapestCost(0.003)
                .potentialSavingsPerRequest(0.002)
                .potentialMonthlySavings(100.0)
                .build();

        assertNotNull(recommendation);
        assertEquals(0.005, recommendation.getCurrentAverageCost());
        assertEquals("TestProvider", recommendation.getCheapestProvider());
        assertEquals(0.003, recommendation.getCheapestCost());
        assertEquals(0.002, recommendation.getPotentialSavingsPerRequest());
        assertEquals(100.0, recommendation.getPotentialMonthlySavings());
    }
}