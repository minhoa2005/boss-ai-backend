package ai.content.auto.service;

import ai.content.auto.dtos.ContentVersionComparisonDto.VersionRecommendationDto;
import ai.content.auto.dtos.ContentVersionComparisonDto.PerformanceComparisonDto;
import ai.content.auto.dtos.ContentVersionComparisonDto.MetricComparisonDto;
import ai.content.auto.dtos.ContentVersionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for generating version recommendations based on performance data.
 * Analyzes multiple factors to recommend the best version.
 * 
 * Requirements: 1.3, 1.4
 */
@Service
@Slf4j
public class VersionRecommendationEngine {

    // Weights for different factors in recommendation algorithm
    private static final BigDecimal QUALITY_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal READABILITY_WEIGHT = BigDecimal.valueOf(0.25);
    private static final BigDecimal SEO_WEIGHT = BigDecimal.valueOf(0.20);
    private static final BigDecimal COST_WEIGHT = BigDecimal.valueOf(0.10);
    private static final BigDecimal PERFORMANCE_WEIGHT = BigDecimal.valueOf(0.10);

    // Thresholds for significance levels
    private static final BigDecimal MAJOR_DIFFERENCE_THRESHOLD = BigDecimal.valueOf(10.0);
    private static final BigDecimal MINOR_DIFFERENCE_THRESHOLD = BigDecimal.valueOf(5.0);
    private static final BigDecimal HIGH_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(80.0);
    private static final BigDecimal MEDIUM_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(60.0);

    /**
     * Generate version recommendation based on comparison data.
     * 
     * @param versionA              Version A data
     * @param versionB              Version B data
     * @param metricsComparison     Metrics comparison data
     * @param performanceComparison Performance comparison data
     * @return Version recommendation
     */
    public VersionRecommendationDto generateRecommendation(ContentVersionDto versionA,
            ContentVersionDto versionB,
            MetricComparisonDto metricsComparison,
            PerformanceComparisonDto performanceComparison) {
        try {
            log.debug("Generating recommendation for versions {} vs {}",
                    versionA.getVersionNumber(), versionB.getVersionNumber());

            // Calculate weighted scores for both versions
            BigDecimal scoreA = calculateWeightedScore(versionA, metricsComparison, true);
            BigDecimal scoreB = calculateWeightedScore(versionB, metricsComparison, false);

            // Determine recommended version
            String recommendedVersion = determineRecommendedVersion(scoreA, scoreB, performanceComparison);

            // Calculate confidence score
            BigDecimal confidenceScore = calculateConfidenceScore(scoreA, scoreB, metricsComparison);

            // Generate reasoning
            String reasoning = generateReasoning(recommendedVersion, scoreA, scoreB,
                    metricsComparison, performanceComparison);

            // Identify key factors
            List<String> keyFactors = identifyKeyFactors(metricsComparison, performanceComparison);

            // Generate considerations
            List<String> considerations = generateConsiderations(versionA, versionB,
                    metricsComparison, confidenceScore);

            // Create analysis details
            Map<String, Object> analysisDetails = createAnalysisDetails(scoreA, scoreB,
                    metricsComparison, performanceComparison);

            return VersionRecommendationDto.builder()
                    .recommendedVersion(recommendedVersion)
                    .confidenceScore(confidenceScore)
                    .reasoning(reasoning)
                    .keyFactors(keyFactors)
                    .considerations(considerations)
                    .analysisDetails(analysisDetails)
                    .build();

        } catch (Exception e) {
            log.error("Error generating version recommendation", e);
            return createFallbackRecommendation(versionA, versionB);
        }
    }

    /**
     * Calculate weighted score for a version.
     * 
     * @param version           Version data
     * @param metricsComparison Metrics comparison
     * @param isVersionA        Whether this is version A
     * @return Weighted score
     */
    private BigDecimal calculateWeightedScore(ContentVersionDto version,
            MetricComparisonDto metricsComparison,
            boolean isVersionA) {
        BigDecimal totalScore = BigDecimal.ZERO;

        // Quality score component
        BigDecimal qualityScore = isVersionA ? metricsComparison.getQualityScore().getVersionAScore()
                : metricsComparison.getQualityScore().getVersionBScore();
        if (qualityScore != null) {
            totalScore = totalScore.add(qualityScore.multiply(QUALITY_WEIGHT));
        }

        // Readability score component
        BigDecimal readabilityScore = isVersionA ? metricsComparison.getReadabilityScore().getVersionAScore()
                : metricsComparison.getReadabilityScore().getVersionBScore();
        if (readabilityScore != null) {
            totalScore = totalScore.add(readabilityScore.multiply(READABILITY_WEIGHT));
        }

        // SEO score component
        BigDecimal seoScore = isVersionA ? metricsComparison.getSeoScore().getVersionAScore()
                : metricsComparison.getSeoScore().getVersionBScore();
        if (seoScore != null) {
            totalScore = totalScore.add(seoScore.multiply(SEO_WEIGHT));
        }

        // Cost efficiency component (inverse - lower cost is better)
        if (version.getGenerationCost() != null && version.getGenerationCost().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costEfficiency = BigDecimal.valueOf(100).divide(version.getGenerationCost(), 2,
                    RoundingMode.HALF_UP);
            totalScore = totalScore.add(costEfficiency.multiply(COST_WEIGHT));
        }

        // Performance component (inverse - lower processing time is better)
        if (version.getProcessingTimeMs() != null && version.getProcessingTimeMs() > 0) {
            BigDecimal performanceScore = BigDecimal.valueOf(10000)
                    .divide(BigDecimal.valueOf(version.getProcessingTimeMs()), 2, RoundingMode.HALF_UP);
            totalScore = totalScore.add(performanceScore.multiply(PERFORMANCE_WEIGHT));
        }

        return totalScore;
    }

    /**
     * Determine which version to recommend.
     * 
     * @param scoreA                Score for version A
     * @param scoreB                Score for version B
     * @param performanceComparison Performance comparison data
     * @return Recommended version ("A" or "B")
     */
    private String determineRecommendedVersion(BigDecimal scoreA, BigDecimal scoreB,
            PerformanceComparisonDto performanceComparison) {
        // Primary decision based on weighted scores
        if (scoreA.compareTo(scoreB) > 0) {
            return "A";
        } else if (scoreB.compareTo(scoreA) > 0) {
            return "B";
        }

        // Tie-breaker: use overall performance winner
        if (performanceComparison != null && performanceComparison.getOverallWinner() != null) {
            if (!"TIE".equals(performanceComparison.getOverallWinner())) {
                return performanceComparison.getOverallWinner();
            }
        }

        // Final tie-breaker: prefer version B (newer version)
        return "B";
    }

    /**
     * Calculate confidence score for the recommendation.
     * 
     * @param scoreA            Score for version A
     * @param scoreB            Score for version B
     * @param metricsComparison Metrics comparison
     * @return Confidence score (0-100)
     */
    private BigDecimal calculateConfidenceScore(BigDecimal scoreA, BigDecimal scoreB,
            MetricComparisonDto metricsComparison) {
        // Calculate score difference
        BigDecimal scoreDifference = scoreA.subtract(scoreB).abs();
        BigDecimal maxScore = scoreA.max(scoreB);

        if (maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(50); // Neutral confidence for identical scores
        }

        // Base confidence on relative difference
        BigDecimal relativeGap = scoreDifference.divide(maxScore, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Adjust confidence based on data availability
        int availableMetrics = countAvailableMetrics(metricsComparison);
        BigDecimal dataQualityMultiplier = BigDecimal.valueOf(Math.min(1.0, availableMetrics / 4.0));

        BigDecimal confidence = relativeGap.multiply(dataQualityMultiplier);

        // Ensure confidence is within bounds
        return confidence.min(BigDecimal.valueOf(95)).max(BigDecimal.valueOf(10));
    }

    /**
     * Generate reasoning text for the recommendation.
     * 
     * @param recommendedVersion    Recommended version
     * @param scoreA                Score for version A
     * @param scoreB                Score for version B
     * @param metricsComparison     Metrics comparison
     * @param performanceComparison Performance comparison
     * @return Reasoning text
     */
    private String generateReasoning(String recommendedVersion, BigDecimal scoreA, BigDecimal scoreB,
            MetricComparisonDto metricsComparison,
            PerformanceComparisonDto performanceComparison) {
        StringBuilder reasoning = new StringBuilder();

        BigDecimal winningScore = "A".equals(recommendedVersion) ? scoreA : scoreB;
        BigDecimal losingScore = "A".equals(recommendedVersion) ? scoreB : scoreA;
        BigDecimal gap = winningScore.subtract(losingScore);

        reasoning.append("Version ").append(recommendedVersion).append(" is recommended ");

        if (gap.compareTo(MAJOR_DIFFERENCE_THRESHOLD) >= 0) {
            reasoning.append("with a significant performance advantage");
        } else if (gap.compareTo(MINOR_DIFFERENCE_THRESHOLD) >= 0) {
            reasoning.append("with a moderate performance advantage");
        } else {
            reasoning.append("with a slight performance advantage");
        }

        // Add specific metric highlights
        List<String> strengths = new ArrayList<>();
        if (metricsComparison.getQualityScore() != null &&
                !metricsComparison.getQualityScore().getWinner().equals("TIE")) {
            if (metricsComparison.getQualityScore().getWinner().equals(recommendedVersion)) {
                strengths.add("higher quality score");
            }
        }

        if (metricsComparison.getReadabilityScore() != null &&
                !metricsComparison.getReadabilityScore().getWinner().equals("TIE")) {
            if (metricsComparison.getReadabilityScore().getWinner().equals(recommendedVersion)) {
                strengths.add("better readability");
            }
        }

        if (metricsComparison.getSeoScore() != null &&
                !metricsComparison.getSeoScore().getWinner().equals("TIE")) {
            if (metricsComparison.getSeoScore().getWinner().equals(recommendedVersion)) {
                strengths.add("superior SEO optimization");
            }
        }

        if (!strengths.isEmpty()) {
            reasoning.append(", particularly in ").append(String.join(", ", strengths));
        }

        reasoning.append(".");

        return reasoning.toString();
    }

    /**
     * Identify key factors that influenced the recommendation.
     * 
     * @param metricsComparison     Metrics comparison
     * @param performanceComparison Performance comparison
     * @return List of key factors
     */
    private List<String> identifyKeyFactors(MetricComparisonDto metricsComparison,
            PerformanceComparisonDto performanceComparison) {
        List<String> factors = new ArrayList<>();

        // Check for significant metric differences
        if (metricsComparison.getQualityScore() != null) {
            BigDecimal diff = metricsComparison.getQualityScore().getDifference().abs();
            if (diff.compareTo(MAJOR_DIFFERENCE_THRESHOLD) >= 0) {
                factors.add(
                        "Significant quality score difference (" + diff.setScale(1, RoundingMode.HALF_UP) + " points)");
            }
        }

        if (metricsComparison.getReadabilityScore() != null) {
            BigDecimal diff = metricsComparison.getReadabilityScore().getDifference().abs();
            if (diff.compareTo(MAJOR_DIFFERENCE_THRESHOLD) >= 0) {
                factors.add("Major readability improvement (" + diff.setScale(1, RoundingMode.HALF_UP) + " points)");
            }
        }

        if (metricsComparison.getSeoScore() != null) {
            BigDecimal diff = metricsComparison.getSeoScore().getDifference().abs();
            if (diff.compareTo(MAJOR_DIFFERENCE_THRESHOLD) >= 0) {
                factors.add("Substantial SEO enhancement (" + diff.setScale(1, RoundingMode.HALF_UP) + " points)");
            }
        }

        // Check cost efficiency
        if (metricsComparison.getCostDifference() != null) {
            BigDecimal costDiff = metricsComparison.getCostDifference().abs();
            if (costDiff.compareTo(BigDecimal.valueOf(0.01)) >= 0) {
                factors.add("Cost efficiency consideration");
            }
        }

        // Check processing time
        if (metricsComparison.getProcessingTimeDifference() != null) {
            Long timeDiff = Math.abs(metricsComparison.getProcessingTimeDifference());
            if (timeDiff > 1000) { // More than 1 second difference
                factors.add("Processing time efficiency");
            }
        }

        if (factors.isEmpty()) {
            factors.add("Overall balanced performance across all metrics");
        }

        return factors;
    }

    /**
     * Generate considerations for the recommendation.
     * 
     * @param versionA          Version A data
     * @param versionB          Version B data
     * @param metricsComparison Metrics comparison
     * @param confidenceScore   Confidence score
     * @return List of considerations
     */
    private List<String> generateConsiderations(ContentVersionDto versionA, ContentVersionDto versionB,
            MetricComparisonDto metricsComparison,
            BigDecimal confidenceScore) {
        List<String> considerations = new ArrayList<>();

        // Low confidence warning
        if (confidenceScore.compareTo(MEDIUM_CONFIDENCE_THRESHOLD) < 0) {
            considerations.add(
                    "The performance difference between versions is relatively small - consider your specific use case");
        }

        // Missing metrics warning
        int availableMetrics = countAvailableMetrics(metricsComparison);
        if (availableMetrics < 3) {
            considerations.add("Limited metrics available for comparison - recommendation based on partial data");
        }

        // Cost considerations
        if (metricsComparison.getCostDifference() != null &&
                metricsComparison.getCostDifference().abs().compareTo(BigDecimal.valueOf(0.05)) >= 0) {
            considerations.add("Consider cost implications for high-volume usage");
        }

        // Processing time considerations
        if (metricsComparison.getProcessingTimeDifference() != null &&
                Math.abs(metricsComparison.getProcessingTimeDifference()) > 2000) {
            considerations.add("Processing time difference may impact user experience");
        }

        // Content length considerations
        if (versionA.getWordCount() != null && versionB.getWordCount() != null) {
            int wordDiff = Math.abs(versionA.getWordCount() - versionB.getWordCount());
            if (wordDiff > 50) {
                considerations.add("Significant content length difference - ensure it meets your requirements");
            }
        }

        if (considerations.isEmpty()) {
            considerations.add("Both versions show good performance - the recommended version has a slight edge");
        }

        return considerations;
    }

    /**
     * Create detailed analysis data.
     * 
     * @param scoreA                Score for version A
     * @param scoreB                Score for version B
     * @param metricsComparison     Metrics comparison
     * @param performanceComparison Performance comparison
     * @return Analysis details map
     */
    private Map<String, Object> createAnalysisDetails(BigDecimal scoreA, BigDecimal scoreB,
            MetricComparisonDto metricsComparison,
            PerformanceComparisonDto performanceComparison) {
        Map<String, Object> details = new HashMap<>();

        details.put("weightedScoreA", scoreA.setScale(2, RoundingMode.HALF_UP));
        details.put("weightedScoreB", scoreB.setScale(2, RoundingMode.HALF_UP));
        details.put("scoreDifference", scoreA.subtract(scoreB).abs().setScale(2, RoundingMode.HALF_UP));

        // Weights used in calculation
        Map<String, BigDecimal> weights = new HashMap<>();
        weights.put("quality", QUALITY_WEIGHT);
        weights.put("readability", READABILITY_WEIGHT);
        weights.put("seo", SEO_WEIGHT);
        weights.put("cost", COST_WEIGHT);
        weights.put("performance", PERFORMANCE_WEIGHT);
        details.put("weights", weights);

        // Available metrics count
        details.put("availableMetrics", countAvailableMetrics(metricsComparison));
        details.put("totalPossibleMetrics", 5);

        // Performance gap
        if (performanceComparison != null && performanceComparison.getPerformanceGap() != null) {
            details.put("performanceGap", performanceComparison.getPerformanceGap());
        }

        return details;
    }

    /**
     * Count available metrics for confidence calculation.
     * 
     * @param metricsComparison Metrics comparison
     * @return Number of available metrics
     */
    private int countAvailableMetrics(MetricComparisonDto metricsComparison) {
        int count = 0;

        if (metricsComparison.getQualityScore() != null &&
                metricsComparison.getQualityScore().getVersionAScore() != null)
            count++;
        if (metricsComparison.getReadabilityScore() != null &&
                metricsComparison.getReadabilityScore().getVersionAScore() != null)
            count++;
        if (metricsComparison.getSeoScore() != null &&
                metricsComparison.getSeoScore().getVersionAScore() != null)
            count++;
        if (metricsComparison.getSentimentScore() != null &&
                metricsComparison.getSentimentScore().getVersionAScore() != null)
            count++;
        if (metricsComparison.getOverallScore() != null &&
                metricsComparison.getOverallScore().getVersionAScore() != null)
            count++;

        return count;
    }

    /**
     * Create fallback recommendation when analysis fails.
     * 
     * @param versionA Version A data
     * @param versionB Version B data
     * @return Fallback recommendation
     */
    private VersionRecommendationDto createFallbackRecommendation(ContentVersionDto versionA,
            ContentVersionDto versionB) {
        return VersionRecommendationDto.builder()
                .recommendedVersion("B") // Default to newer version
                .confidenceScore(BigDecimal.valueOf(50))
                .reasoning("Unable to perform detailed analysis. Recommending newer version by default.")
                .keyFactors(List.of("Default recommendation due to analysis error"))
                .considerations(List.of("Manual review recommended due to limited analysis"))
                .analysisDetails(Map.of("error", "Analysis failed", "fallback", true))
                .build();
    }
}