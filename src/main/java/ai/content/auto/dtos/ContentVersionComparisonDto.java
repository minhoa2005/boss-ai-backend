package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for content version comparison results.
 * Contains detailed comparison data between two content versions.
 * 
 * Requirements: 1.3, 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionComparisonDto {

    private Long contentId;
    private ContentVersionDto versionA;
    private ContentVersionDto versionB;

    // Text comparison results
    private List<TextDifferenceDto> textDifferences;
    private ComparisonSummaryDto comparisonSummary;

    // Metrics comparison
    private MetricComparisonDto metricsComparison;

    // Performance analysis
    private PerformanceComparisonDto performanceComparison;

    // Recommendation
    private VersionRecommendationDto recommendation;

    // Comparison metadata
    private Long comparedBy;
    private String comparedByUsername;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant comparedAt;

    /**
     * Summary of text comparison results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummaryDto {
        private Integer totalChanges;
        private Integer additions;
        private Integer deletions;
        private Integer modifications;
        private BigDecimal similarityPercentage;
        private Integer wordCountDifference;
        private Integer characterCountDifference;
    }

    /**
     * Individual text difference between versions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextDifferenceDto {
        private String type; // ADDITION, DELETION, MODIFICATION, UNCHANGED
        private String originalText;
        private String newText;
        private Integer startPosition;
        private Integer endPosition;
        private Integer lineNumber;
    }

    /**
     * Comparison of metrics between versions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricComparisonDto {
        private ScoreComparisonDto qualityScore;
        private ScoreComparisonDto readabilityScore;
        private ScoreComparisonDto seoScore;
        private ScoreComparisonDto sentimentScore;
        private ScoreComparisonDto overallScore;

        // Performance metrics
        private Long processingTimeDifference;
        private BigDecimal costDifference;
        private Integer tokenUsageDifference;
    }

    /**
     * Individual score comparison.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreComparisonDto {
        private BigDecimal versionAScore;
        private BigDecimal versionBScore;
        private BigDecimal difference;
        private BigDecimal percentageChange;
        private String winner; // A, B, or TIE
        private String significance; // MAJOR, MINOR, NEGLIGIBLE
    }

    /**
     * Performance comparison analysis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceComparisonDto {
        private String overallWinner; // A, B, or TIE
        private BigDecimal performanceGap;
        private List<String> versionAStrengths;
        private List<String> versionBStrengths;
        private List<String> improvementAreas;
        private Map<String, Object> detailedAnalysis;
    }

    /**
     * Version recommendation based on comparison.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionRecommendationDto {
        private String recommendedVersion; // A or B
        private BigDecimal confidenceScore;
        private String reasoning;
        private List<String> keyFactors;
        private List<String> considerations;
        private Map<String, Object> analysisDetails;
    }
}