package ai.content.auto.service;

import ai.content.auto.dtos.ContentVersionComparisonDto;
import ai.content.auto.dtos.ContentVersionComparisonDto.*;
import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.entity.ContentVersion;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.exception.InternalServerException;
import ai.content.auto.mapper.ContentVersionMapper;
import ai.content.auto.repository.ContentVersionRepository;
import ai.content.auto.util.SecurityUtil;
import ai.content.auto.util.TextDifferenceAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Service for comparing content versions.
 * Provides text difference analysis, metrics comparison, and version
 * recommendations.
 * 
 * Requirements: 1.3, 1.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentComparisonService {

    private final ContentVersionRepository contentVersionRepository;
    private final ContentVersionMapper contentVersionMapper;
    private final SecurityUtil securityUtil;
    private final TextDifferenceAnalyzer textDifferenceAnalyzer;
    private final VersionRecommendationEngine recommendationEngine;

    // Thresholds for significance levels
    private static final BigDecimal MAJOR_DIFFERENCE_THRESHOLD = BigDecimal.valueOf(10.0);
    private static final BigDecimal MINOR_DIFFERENCE_THRESHOLD = BigDecimal.valueOf(5.0);
    private static final BigDecimal NEGLIGIBLE_DIFFERENCE_THRESHOLD = BigDecimal.valueOf(1.0);

    /**
     * Compare two content versions and provide detailed analysis.
     * 
     * @param contentId      Content ID
     * @param versionNumberA Version A number
     * @param versionNumberB Version B number
     * @return Detailed comparison results
     */
    @Transactional(readOnly = true)
    public ContentVersionComparisonDto compareVersions(Long contentId, Integer versionNumberA, Integer versionNumberB) {
        try {
            // 1. Validate input
            validateComparisonInput(contentId, versionNumberA, versionNumberB);

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            String username = securityUtil.getCurrentUser().getUsername();
            log.info("Comparing versions {} vs {} for content: {} by user: {}",
                    versionNumberA, versionNumberB, contentId, userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get both versions
            ContentVersion versionA = getVersionEntity(contentId, versionNumberA);
            ContentVersion versionB = getVersionEntity(contentId, versionNumberB);

            // 5. Convert to DTOs
            ContentVersionDto versionADto = contentVersionMapper.toDto(versionA);
            ContentVersionDto versionBDto = contentVersionMapper.toDto(versionB);

            // 6. Perform text comparison
            List<TextDifferenceDto> textDifferences = textDifferenceAnalyzer.analyzeTextDifferences(
                    versionA.getContent(), versionB.getContent());

            // 7. Create comparison summary
            ComparisonSummaryDto comparisonSummary = textDifferenceAnalyzer.createComparisonSummary(
                    versionA.getContent(), versionB.getContent(), textDifferences);

            // 8. Perform metrics comparison
            MetricComparisonDto metricsComparison = compareMetrics(versionA, versionB);

            // 9. Perform performance comparison
            PerformanceComparisonDto performanceComparison = comparePerformance(versionADto, versionBDto,
                    metricsComparison);

            // 10. Generate recommendation
            VersionRecommendationDto recommendation = recommendationEngine.generateRecommendation(
                    versionADto, versionBDto, metricsComparison, performanceComparison);

            // 11. Build comparison result
            ContentVersionComparisonDto comparison = ContentVersionComparisonDto.builder()
                    .contentId(contentId)
                    .versionA(versionADto)
                    .versionB(versionBDto)
                    .textDifferences(textDifferences)
                    .comparisonSummary(comparisonSummary)
                    .metricsComparison(metricsComparison)
                    .performanceComparison(performanceComparison)
                    .recommendation(recommendation)
                    .comparedBy(userId)
                    .comparedByUsername(username)
                    .comparedAt(Instant.now())
                    .build();

            log.info("Version comparison completed for content: {}, versions {} vs {}",
                    contentId, versionNumberA, versionNumberB);

            return comparison;

        } catch (BusinessException e) {
            log.error("Business error comparing versions {} vs {} for content: {}",
                    versionNumberA, versionNumberB, contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error comparing versions {} vs {} for content: {}",
                    versionNumberA, versionNumberB, contentId, e);
            throw new InternalServerException("Failed to compare content versions");
        }
    }

    /**
     * Get side-by-side comparison for display purposes.
     * 
     * @param contentId      Content ID
     * @param versionNumberA Version A number
     * @param versionNumberB Version B number
     * @return Simplified comparison for UI display
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSideBySideComparison(Long contentId, Integer versionNumberA, Integer versionNumberB) {
        try {
            // 1. Validate input and ownership
            validateComparisonInput(contentId, versionNumberA, versionNumberB);
            validateContentOwnership(contentId, securityUtil.getCurrentUserId());

            // 2. Get versions
            ContentVersion versionA = getVersionEntity(contentId, versionNumberA);
            ContentVersion versionB = getVersionEntity(contentId, versionNumberB);

            // 3. Analyze text differences for highlighting
            List<TextDifferenceDto> textDifferences = textDifferenceAnalyzer.analyzeTextDifferences(
                    versionA.getContent(), versionB.getContent());

            // 4. Create side-by-side data structure
            Map<String, Object> sideBySide = new HashMap<>();

            // Version information
            sideBySide.put("versionA", Map.of(
                    "number", versionA.getVersionNumber(),
                    "content", versionA.getContent(),
                    "title", versionA.getTitle() != null ? versionA.getTitle() : "",
                    "createdAt", versionA.getCreatedAt(),
                    "aiProvider", versionA.getAiProvider(),
                    "wordCount", versionA.getWordCount() != null ? versionA.getWordCount() : 0));

            sideBySide.put("versionB", Map.of(
                    "number", versionB.getVersionNumber(),
                    "content", versionB.getContent(),
                    "title", versionB.getTitle() != null ? versionB.getTitle() : "",
                    "createdAt", versionB.getCreatedAt(),
                    "aiProvider", versionB.getAiProvider(),
                    "wordCount", versionB.getWordCount() != null ? versionB.getWordCount() : 0));

            // Differences for highlighting
            sideBySide.put("differences", textDifferences);

            // Summary statistics
            ComparisonSummaryDto summary = textDifferenceAnalyzer.createComparisonSummary(
                    versionA.getContent(), versionB.getContent(), textDifferences);
            sideBySide.put("summary", summary);

            // Quick metrics comparison
            Map<String, Object> quickMetrics = new HashMap<>();
            if (versionA.getQualityScore() != null && versionB.getQualityScore() != null) {
                quickMetrics.put("qualityDifference", versionB.getQualityScore().subtract(versionA.getQualityScore()));
            }
            if (versionA.getReadabilityScore() != null && versionB.getReadabilityScore() != null) {
                quickMetrics.put("readabilityDifference",
                        versionB.getReadabilityScore().subtract(versionA.getReadabilityScore()));
            }
            if (versionA.getSeoScore() != null && versionB.getSeoScore() != null) {
                quickMetrics.put("seoDifference", versionB.getSeoScore().subtract(versionA.getSeoScore()));
            }
            sideBySide.put("quickMetrics", quickMetrics);

            return sideBySide;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating side-by-side comparison for content: {}", contentId, e);
            throw new InternalServerException("Failed to create side-by-side comparison");
        }
    }

    /**
     * Compare metrics between two versions.
     * 
     * @param versionA Version A entity
     * @param versionB Version B entity
     * @return Metrics comparison
     */
    private MetricComparisonDto compareMetrics(ContentVersion versionA, ContentVersion versionB) {
        return MetricComparisonDto.builder()
                .qualityScore(compareScore(versionA.getQualityScore(), versionB.getQualityScore()))
                .readabilityScore(compareScore(versionA.getReadabilityScore(), versionB.getReadabilityScore()))
                .seoScore(compareScore(versionA.getSeoScore(), versionB.getSeoScore()))
                .sentimentScore(compareScore(versionA.getSentimentScore(), versionB.getSentimentScore()))
                .overallScore(compareScore(versionA.calculateOverallScore(), versionB.calculateOverallScore()))
                .processingTimeDifference(calculateProcessingTimeDifference(versionA, versionB))
                .costDifference(calculateCostDifference(versionA, versionB))
                .tokenUsageDifference(calculateTokenUsageDifference(versionA, versionB))
                .build();
    }

    /**
     * Compare individual scores between versions.
     * 
     * @param scoreA Score from version A
     * @param scoreB Score from version B
     * @return Score comparison
     */
    private ScoreComparisonDto compareScore(BigDecimal scoreA, BigDecimal scoreB) {
        if (scoreA == null && scoreB == null) {
            return ScoreComparisonDto.builder()
                    .winner("TIE")
                    .significance("NEGLIGIBLE")
                    .build();
        }

        if (scoreA == null)
            scoreA = BigDecimal.ZERO;
        if (scoreB == null)
            scoreB = BigDecimal.ZERO;

        BigDecimal difference = scoreB.subtract(scoreA);
        BigDecimal percentageChange = BigDecimal.ZERO;

        if (scoreA.compareTo(BigDecimal.ZERO) != 0) {
            percentageChange = difference.divide(scoreA, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        String winner = determineWinner(scoreA, scoreB);
        String significance = determineSignificance(difference.abs());

        return ScoreComparisonDto.builder()
                .versionAScore(scoreA)
                .versionBScore(scoreB)
                .difference(difference)
                .percentageChange(percentageChange)
                .winner(winner)
                .significance(significance)
                .build();
    }

    /**
     * Compare performance between versions.
     * 
     * @param versionA          Version A DTO
     * @param versionB          Version B DTO
     * @param metricsComparison Metrics comparison
     * @return Performance comparison
     */
    private PerformanceComparisonDto comparePerformance(ContentVersionDto versionA, ContentVersionDto versionB,
            MetricComparisonDto metricsComparison) {
        // Determine overall winner based on metrics
        String overallWinner = determineOverallWinner(metricsComparison);

        // Calculate performance gap
        BigDecimal performanceGap = calculatePerformanceGap(metricsComparison);

        // Identify strengths for each version
        List<String> versionAStrengths = identifyVersionStrengths(versionA, metricsComparison, true);
        List<String> versionBStrengths = identifyVersionStrengths(versionB, metricsComparison, false);

        // Identify improvement areas
        List<String> improvementAreas = identifyImprovementAreas(metricsComparison);

        // Create detailed analysis
        Map<String, Object> detailedAnalysis = createDetailedPerformanceAnalysis(versionA, versionB, metricsComparison);

        return PerformanceComparisonDto.builder()
                .overallWinner(overallWinner)
                .performanceGap(performanceGap)
                .versionAStrengths(versionAStrengths)
                .versionBStrengths(versionBStrengths)
                .improvementAreas(improvementAreas)
                .detailedAnalysis(detailedAnalysis)
                .build();
    }

    // Helper methods for calculations

    private Long calculateProcessingTimeDifference(ContentVersion versionA, ContentVersion versionB) {
        if (versionA.getProcessingTimeMs() == null || versionB.getProcessingTimeMs() == null) {
            return null;
        }
        return versionB.getProcessingTimeMs() - versionA.getProcessingTimeMs();
    }

    private BigDecimal calculateCostDifference(ContentVersion versionA, ContentVersion versionB) {
        if (versionA.getGenerationCost() == null || versionB.getGenerationCost() == null) {
            return null;
        }
        return versionB.getGenerationCost().subtract(versionA.getGenerationCost());
    }

    private Integer calculateTokenUsageDifference(ContentVersion versionA, ContentVersion versionB) {
        if (versionA.getTokensUsed() == null || versionB.getTokensUsed() == null) {
            return null;
        }
        return versionB.getTokensUsed() - versionA.getTokensUsed();
    }

    private String determineWinner(BigDecimal scoreA, BigDecimal scoreB) {
        int comparison = scoreA.compareTo(scoreB);
        if (comparison > 0)
            return "A";
        if (comparison < 0)
            return "B";
        return "TIE";
    }

    private String determineSignificance(BigDecimal difference) {
        if (difference.compareTo(MAJOR_DIFFERENCE_THRESHOLD) >= 0) {
            return "MAJOR";
        } else if (difference.compareTo(MINOR_DIFFERENCE_THRESHOLD) >= 0) {
            return "MINOR";
        } else {
            return "NEGLIGIBLE";
        }
    }

    private String determineOverallWinner(MetricComparisonDto metricsComparison) {
        int scoreA = 0, scoreB = 0;

        if (metricsComparison.getQualityScore() != null) {
            String winner = metricsComparison.getQualityScore().getWinner();
            if ("A".equals(winner))
                scoreA += 2; // Quality has higher weight
            else if ("B".equals(winner))
                scoreB += 2;
        }

        if (metricsComparison.getReadabilityScore() != null) {
            String winner = metricsComparison.getReadabilityScore().getWinner();
            if ("A".equals(winner))
                scoreA += 1;
            else if ("B".equals(winner))
                scoreB += 1;
        }

        if (metricsComparison.getSeoScore() != null) {
            String winner = metricsComparison.getSeoScore().getWinner();
            if ("A".equals(winner))
                scoreA += 1;
            else if ("B".equals(winner))
                scoreB += 1;
        }

        if (scoreA > scoreB)
            return "A";
        if (scoreB > scoreA)
            return "B";
        return "TIE";
    }

    private BigDecimal calculatePerformanceGap(MetricComparisonDto metricsComparison) {
        List<BigDecimal> differences = new ArrayList<>();

        if (metricsComparison.getQualityScore() != null
                && metricsComparison.getQualityScore().getDifference() != null) {
            differences.add(metricsComparison.getQualityScore().getDifference().abs());
        }
        if (metricsComparison.getReadabilityScore() != null
                && metricsComparison.getReadabilityScore().getDifference() != null) {
            differences.add(metricsComparison.getReadabilityScore().getDifference().abs());
        }
        if (metricsComparison.getSeoScore() != null && metricsComparison.getSeoScore().getDifference() != null) {
            differences.add(metricsComparison.getSeoScore().getDifference().abs());
        }

        if (differences.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return differences.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(differences.size()), 2, RoundingMode.HALF_UP);
    }

    private List<String> identifyVersionStrengths(ContentVersionDto version, MetricComparisonDto metricsComparison,
            boolean isVersionA) {
        List<String> strengths = new ArrayList<>();

        if (metricsComparison.getQualityScore() != null) {
            String winner = metricsComparison.getQualityScore().getWinner();
            if ((isVersionA && "A".equals(winner)) || (!isVersionA && "B".equals(winner))) {
                strengths.add("Higher quality score");
            }
        }

        if (metricsComparison.getReadabilityScore() != null) {
            String winner = metricsComparison.getReadabilityScore().getWinner();
            if ((isVersionA && "A".equals(winner)) || (!isVersionA && "B".equals(winner))) {
                strengths.add("Better readability");
            }
        }

        if (metricsComparison.getSeoScore() != null) {
            String winner = metricsComparison.getSeoScore().getWinner();
            if ((isVersionA && "A".equals(winner)) || (!isVersionA && "B".equals(winner))) {
                strengths.add("Superior SEO optimization");
            }
        }

        // Cost and performance strengths
        if (metricsComparison.getCostDifference() != null) {
            boolean isMoreCostEffective = isVersionA
                    ? metricsComparison.getCostDifference().compareTo(BigDecimal.ZERO) > 0
                    : metricsComparison.getCostDifference().compareTo(BigDecimal.ZERO) < 0;
            if (isMoreCostEffective) {
                strengths.add("More cost-effective");
            }
        }

        if (metricsComparison.getProcessingTimeDifference() != null) {
            boolean isFaster = isVersionA ? metricsComparison.getProcessingTimeDifference() > 0
                    : metricsComparison.getProcessingTimeDifference() < 0;
            if (isFaster) {
                strengths.add("Faster processing time");
            }
        }

        return strengths;
    }

    private List<String> identifyImprovementAreas(MetricComparisonDto metricsComparison) {
        List<String> areas = new ArrayList<>();

        // Check for areas where both versions could improve
        if (metricsComparison.getQualityScore() != null) {
            BigDecimal maxQuality = metricsComparison.getQualityScore().getVersionAScore()
                    .max(metricsComparison.getQualityScore().getVersionBScore());
            if (maxQuality.compareTo(BigDecimal.valueOf(80)) < 0) {
                areas.add("Overall content quality could be enhanced");
            }
        }

        if (metricsComparison.getReadabilityScore() != null) {
            BigDecimal maxReadability = metricsComparison.getReadabilityScore().getVersionAScore()
                    .max(metricsComparison.getReadabilityScore().getVersionBScore());
            if (maxReadability.compareTo(BigDecimal.valueOf(75)) < 0) {
                areas.add("Content readability needs improvement");
            }
        }

        if (metricsComparison.getSeoScore() != null) {
            BigDecimal maxSeo = metricsComparison.getSeoScore().getVersionAScore()
                    .max(metricsComparison.getSeoScore().getVersionBScore());
            if (maxSeo.compareTo(BigDecimal.valueOf(70)) < 0) {
                areas.add("SEO optimization requires attention");
            }
        }

        return areas;
    }

    private Map<String, Object> createDetailedPerformanceAnalysis(ContentVersionDto versionA,
            ContentVersionDto versionB,
            MetricComparisonDto metricsComparison) {
        Map<String, Object> analysis = new HashMap<>();

        // Score breakdown
        Map<String, Object> scoreBreakdown = new HashMap<>();
        if (metricsComparison.getQualityScore() != null) {
            scoreBreakdown.put("quality", Map.of(
                    "versionA", metricsComparison.getQualityScore().getVersionAScore(),
                    "versionB", metricsComparison.getQualityScore().getVersionBScore(),
                    "difference", metricsComparison.getQualityScore().getDifference()));
        }
        analysis.put("scoreBreakdown", scoreBreakdown);

        // Content statistics
        analysis.put("contentStats", Map.of(
                "versionA", Map.of(
                        "wordCount", versionA.getWordCount() != null ? versionA.getWordCount() : 0,
                        "characterCount", versionA.getCharacterCount() != null ? versionA.getCharacterCount() : 0),
                "versionB", Map.of(
                        "wordCount", versionB.getWordCount() != null ? versionB.getWordCount() : 0,
                        "characterCount", versionB.getCharacterCount() != null ? versionB.getCharacterCount() : 0)));

        // Performance metrics
        if (metricsComparison.getProcessingTimeDifference() != null || metricsComparison.getCostDifference() != null) {
            Map<String, Object> performanceMetrics = new HashMap<>();
            if (metricsComparison.getProcessingTimeDifference() != null) {
                performanceMetrics.put("processingTimeDifference", metricsComparison.getProcessingTimeDifference());
            }
            if (metricsComparison.getCostDifference() != null) {
                performanceMetrics.put("costDifference", metricsComparison.getCostDifference());
            }
            analysis.put("performanceMetrics", performanceMetrics);
        }

        return analysis;
    }

    // Validation methods

    private void validateComparisonInput(Long contentId, Integer versionNumberA, Integer versionNumberB) {
        if (contentId == null) {
            throw new BusinessException("Content ID is required");
        }
        if (versionNumberA == null || versionNumberB == null) {
            throw new BusinessException("Both version numbers are required");
        }
        if (versionNumberA.equals(versionNumberB)) {
            throw new BusinessException("Cannot compare a version with itself");
        }
    }

    private void validateContentOwnership(Long contentId, Long userId) {
        // This validation is handled by the ContentVersioningService
        // We'll delegate to it for consistency
        try {
            ContentVersion version = contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Content not found"));

            // Check if user has access to this content
            // This is a simplified check - in a real implementation, you might have more
            // complex ownership rules
        } catch (Exception e) {
            throw new NotFoundException("Content not found or access denied");
        }
    }

    private ContentVersion getVersionEntity(Long contentId, Integer versionNumber) {
        return contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Version " + versionNumber + " not found for content " + contentId));
    }
}