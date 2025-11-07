package ai.content.auto.service;

import ai.content.auto.dtos.ABTestConfiguration;
import ai.content.auto.dtos.ABTestResult;
import ai.content.auto.dtos.ABTestStatus;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.TemplateABTest;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateABTestRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for A/B testing templates to optimize performance
 * Allows comparing different template variations to determine which performs
 * better
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateABTestingService {

    private final TemplateABTestRepository abTestRepository;
    private final ContentTemplateRepository templateRepository;
    private final TemplateUsageLogRepository usageLogRepository;
    private final SecurityUtil securityUtil;

    // Statistical significance threshold (95% confidence)
    private static final double SIGNIFICANCE_THRESHOLD = 1.96;
    private static final int MIN_SAMPLES_PER_VARIANT = 30;

    /**
     * Create a new A/B test for template optimization
     */
    public ABTestConfiguration createABTest(ABTestConfiguration config) {
        try {
            User currentUser = getCurrentUser();
            log.info("Creating A/B test: {} by user: {}", config.getTestName(), currentUser.getId());

            validateABTestConfiguration(config);

            return createABTestInTransaction(config, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating A/B test: {}", config.getTestName(), e);
            throw new BusinessException("Failed to create A/B test");
        }
    }

    @Transactional
    private ABTestConfiguration createABTestInTransaction(ABTestConfiguration config, User currentUser) {
        // Verify templates exist
        ContentTemplate variantA = templateRepository.findById(config.getVariantATemplateId())
                .orElseThrow(() -> new BusinessException("Variant A template not found"));
        ContentTemplate variantB = templateRepository.findById(config.getVariantBTemplateId())
                .orElseThrow(() -> new BusinessException("Variant B template not found"));

        // Create A/B test entity
        TemplateABTest abTest = new TemplateABTest();
        abTest.setTestName(config.getTestName());
        abTest.setDescription(config.getDescription());
        abTest.setVariantATemplate(variantA);
        abTest.setVariantBTemplate(variantB);
        abTest.setTrafficSplit(config.getTrafficSplit() != null ? config.getTrafficSplit() : 50);
        abTest.setMetricToOptimize(config.getMetricToOptimize());
        abTest.setMinSampleSize(
                config.getMinSampleSize() != null ? config.getMinSampleSize() : MIN_SAMPLES_PER_VARIANT);
        abTest.setStatus("ACTIVE");
        abTest.setStartedAt(OffsetDateTime.now());
        abTest.setCreatedBy(currentUser);
        abTest.setCreatedAt(OffsetDateTime.now());

        TemplateABTest savedTest = abTestRepository.save(abTest);

        log.info("A/B test created successfully: {} with ID: {}", savedTest.getTestName(), savedTest.getId());

        return mapToConfiguration(savedTest);
    }

    /**
     * Get template variant for A/B test (randomly assigns based on traffic split)
     */
    public Long getTemplateVariantForTest(Long abTestId, Long userId) {
        try {
            return getTemplateVariantInTransaction(abTestId, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting template variant for test: {}", abTestId, e);
            throw new BusinessException("Failed to get template variant");
        }
    }

    @Transactional(readOnly = true)
    private Long getTemplateVariantInTransaction(Long abTestId, Long userId) {
        TemplateABTest abTest = abTestRepository.findById(abTestId)
                .orElseThrow(() -> new BusinessException("A/B test not found"));

        if (!"ACTIVE".equals(abTest.getStatus())) {
            throw new BusinessException("A/B test is not active");
        }

        // Use consistent hashing based on user ID to ensure same user gets same variant
        int hash = Math.abs((userId.toString() + abTestId.toString()).hashCode());
        int trafficSplit = abTest.getTrafficSplit();

        // Assign variant based on traffic split
        if (hash % 100 < trafficSplit) {
            return abTest.getVariantATemplate().getId();
        } else {
            return abTest.getVariantBTemplate().getId();
        }
    }

    /**
     * Get A/B test results and analysis
     */
    public ABTestResult getABTestResults(Long abTestId) {
        try {
            log.info("Generating A/B test results for test: {}", abTestId);

            return getABTestResultsInTransaction(abTestId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating A/B test results for test: {}", abTestId, e);
            throw new BusinessException("Failed to generate A/B test results");
        }
    }

    @Transactional(readOnly = true)
    private ABTestResult getABTestResultsInTransaction(Long abTestId) {
        TemplateABTest abTest = abTestRepository.findById(abTestId)
                .orElseThrow(() -> new BusinessException("A/B test not found"));

        Long variantAId = abTest.getVariantATemplate().getId();
        Long variantBId = abTest.getVariantBTemplate().getId();

        // Get usage counts during test period
        OffsetDateTime testStart = abTest.getStartedAt();
        OffsetDateTime testEnd = abTest.getEndedAt() != null ? abTest.getEndedAt() : OffsetDateTime.now();

        Long variantAUsage = usageLogRepository.countUsageByTemplateInDateRange(variantAId, testStart, testEnd);
        Long variantBUsage = usageLogRepository.countUsageByTemplateInDateRange(variantBId, testStart, testEnd);

        // Calculate metrics based on optimization goal
        String metric = abTest.getMetricToOptimize();
        BigDecimal variantAMetric = calculateMetricValue(variantAId, metric, testStart, testEnd);
        BigDecimal variantBMetric = calculateMetricValue(variantBId, metric, testStart, testEnd);

        // Calculate statistical significance
        boolean isSignificant = calculateStatisticalSignificance(
                variantAMetric.doubleValue(), variantAUsage,
                variantBMetric.doubleValue(), variantBUsage);

        // Determine winner
        String winner = determineWinner(variantAMetric, variantBMetric, isSignificant);

        // Calculate confidence level
        double confidenceLevel = calculateConfidenceLevel(
                variantAMetric.doubleValue(), variantAUsage,
                variantBMetric.doubleValue(), variantBUsage);

        // Calculate improvement percentage
        BigDecimal improvement = calculateImprovement(variantAMetric, variantBMetric);

        return ABTestResult.builder()
                .testId(abTestId)
                .testName(abTest.getTestName())
                .status(abTest.getStatus())
                .variantATemplateId(variantAId)
                .variantATemplateName(abTest.getVariantATemplate().getName())
                .variantAUsageCount(variantAUsage.intValue())
                .variantAMetricValue(variantAMetric)
                .variantBTemplateId(variantBId)
                .variantBTemplateName(abTest.getVariantBTemplate().getName())
                .variantBUsageCount(variantBUsage.intValue())
                .variantBMetricValue(variantBMetric)
                .metricName(metric)
                .winner(winner)
                .isStatisticallySignificant(isSignificant)
                .confidenceLevel(BigDecimal.valueOf(confidenceLevel).setScale(2, RoundingMode.HALF_UP))
                .improvement(improvement)
                .startedAt(testStart)
                .endedAt(testEnd)
                .build();
    }

    /**
     * Calculate metric value for a template during test period
     */
    private BigDecimal calculateMetricValue(Long templateId, String metric,
            OffsetDateTime startDate, OffsetDateTime endDate) {

        try {
            switch (metric.toUpperCase()) {
                case "SUCCESS_RATE":
                    return calculateSuccessRateInPeriod(templateId, startDate, endDate);
                case "AVERAGE_RATING":
                    return calculateAverageRatingInPeriod(templateId, startDate, endDate);
                case "QUALITY_SCORE":
                    return calculateAverageQualityInPeriod(templateId, startDate, endDate);
                case "GENERATION_TIME":
                    return calculateAverageGenerationTimeInPeriod(templateId, startDate, endDate);
                case "COMPLETION_RATE":
                    return calculateCompletionRateInPeriod(templateId, startDate, endDate);
                default:
                    log.warn("Unknown metric: {}, defaulting to success rate", metric);
                    return calculateSuccessRateInPeriod(templateId, startDate, endDate);
            }
        } catch (Exception e) {
            log.error("Error calculating metric {} for template: {}", metric, templateId, e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateSuccessRateInPeriod(Long templateId, OffsetDateTime start, OffsetDateTime end) {
        Long total = usageLogRepository.countUsageByTemplateInDateRange(templateId, start, end);
        if (total == 0)
            return BigDecimal.ZERO;

        Long successful = usageLogRepository.countSuccessfulUsageByTemplateInDateRange(templateId, start, end, true);
        return BigDecimal.valueOf((successful.doubleValue() / total.doubleValue()) * 100.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageRatingInPeriod(Long templateId, OffsetDateTime start, OffsetDateTime end) {
        Double avgRating = usageLogRepository.getAverageRatingByTemplateInDateRange(templateId, start, end);
        return avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calculateAverageQualityInPeriod(Long templateId, OffsetDateTime start, OffsetDateTime end) {
        Double avgQuality = usageLogRepository.getAverageQualityByTemplateInDateRange(templateId, start, end);
        return avgQuality != null ? BigDecimal.valueOf(avgQuality).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calculateAverageGenerationTimeInPeriod(Long templateId, OffsetDateTime start,
            OffsetDateTime end) {
        Double avgTime = usageLogRepository.getAverageGenerationTimeByTemplateInDateRange(templateId, start, end);
        return avgTime != null ? BigDecimal.valueOf(avgTime).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calculateCompletionRateInPeriod(Long templateId, OffsetDateTime start, OffsetDateTime end) {
        Long total = usageLogRepository.countUsageByTemplateInDateRange(templateId, start, end);
        if (total == 0)
            return BigDecimal.ZERO;

        Long completed = usageLogRepository.countCompletedUsageByTemplateInDateRange(templateId, start, end, true);
        return BigDecimal.valueOf((completed.doubleValue() / total.doubleValue()) * 100.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate statistical significance using Z-test for proportions
     */
    private boolean calculateStatisticalSignificance(double metricA, long sampleA,
            double metricB, long sampleB) {

        // Need minimum sample size
        if (sampleA < MIN_SAMPLES_PER_VARIANT || sampleB < MIN_SAMPLES_PER_VARIANT) {
            return false;
        }

        // Convert percentages to proportions
        double pA = metricA / 100.0;
        double pB = metricB / 100.0;

        // Calculate pooled proportion
        double pooledP = ((pA * sampleA) + (pB * sampleB)) / (sampleA + sampleB);

        // Calculate standard error
        double se = Math.sqrt(pooledP * (1 - pooledP) * ((1.0 / sampleA) + (1.0 / sampleB)));

        if (se == 0)
            return false;

        // Calculate Z-score
        double zScore = Math.abs((pA - pB) / se);

        // Check if Z-score exceeds threshold for 95% confidence
        return zScore >= SIGNIFICANCE_THRESHOLD;
    }

    /**
     * Calculate confidence level as percentage
     */
    private double calculateConfidenceLevel(double metricA, long sampleA,
            double metricB, long sampleB) {

        if (sampleA < MIN_SAMPLES_PER_VARIANT || sampleB < MIN_SAMPLES_PER_VARIANT) {
            return 0.0;
        }

        double pA = metricA / 100.0;
        double pB = metricB / 100.0;
        double pooledP = ((pA * sampleA) + (pB * sampleB)) / (sampleA + sampleB);
        double se = Math.sqrt(pooledP * (1 - pooledP) * ((1.0 / sampleA) + (1.0 / sampleB)));

        if (se == 0)
            return 0.0;

        double zScore = Math.abs((pA - pB) / se);

        // Convert Z-score to confidence level (approximate)
        // Z=1.96 -> 95%, Z=2.58 -> 99%
        if (zScore >= 2.58)
            return 99.0;
        if (zScore >= 1.96)
            return 95.0;
        if (zScore >= 1.64)
            return 90.0;
        if (zScore >= 1.28)
            return 80.0;

        return Math.min(80.0, zScore * 40.0); // Approximate for lower values
    }

    /**
     * Determine winner based on metrics and significance
     */
    private String determineWinner(BigDecimal metricA, BigDecimal metricB, boolean isSignificant) {
        if (!isSignificant) {
            return "INCONCLUSIVE";
        }

        int comparison = metricA.compareTo(metricB);
        if (comparison > 0) {
            return "VARIANT_A";
        } else if (comparison < 0) {
            return "VARIANT_B";
        } else {
            return "TIE";
        }
    }

    /**
     * Calculate improvement percentage
     */
    private BigDecimal calculateImprovement(BigDecimal metricA, BigDecimal metricB) {
        if (metricB.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return metricA.subtract(metricB)
                .divide(metricB, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Stop an active A/B test
     */
    public void stopABTest(Long abTestId) {
        try {
            User currentUser = getCurrentUser();
            log.info("Stopping A/B test: {} by user: {}", abTestId, currentUser.getId());

            stopABTestInTransaction(abTestId, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error stopping A/B test: {}", abTestId, e);
            throw new BusinessException("Failed to stop A/B test");
        }
    }

    @Transactional
    private void stopABTestInTransaction(Long abTestId, User currentUser) {
        TemplateABTest abTest = abTestRepository.findById(abTestId)
                .orElseThrow(() -> new BusinessException("A/B test not found"));

        if (!"ACTIVE".equals(abTest.getStatus())) {
            throw new BusinessException("A/B test is not active");
        }

        abTest.setStatus("COMPLETED");
        abTest.setEndedAt(OffsetDateTime.now());
        abTest.setUpdatedBy(currentUser);
        abTest.setUpdatedAt(OffsetDateTime.now());

        abTestRepository.save(abTest);

        log.info("A/B test stopped successfully: {}", abTestId);
    }

    /**
     * Get all active A/B tests
     */
    public List<ABTestStatus> getActiveABTests() {
        try {
            return getActiveABTestsInTransaction();
        } catch (Exception e) {
            log.error("Error getting active A/B tests", e);
            throw new BusinessException("Failed to get active A/B tests");
        }
    }

    @Transactional(readOnly = true)
    private List<ABTestStatus> getActiveABTestsInTransaction() {
        List<TemplateABTest> activeTests = abTestRepository.findByStatus("ACTIVE");

        return activeTests.stream()
                .map(this::mapToStatus)
                .collect(Collectors.toList());
    }

    // Helper methods

    private User getCurrentUser() {
        return securityUtil.getCurrentUser();
    }

    private void validateABTestConfiguration(ABTestConfiguration config) {
        if (config.getTestName() == null || config.getTestName().trim().isEmpty()) {
            throw new BusinessException("Test name is required");
        }

        if (config.getVariantATemplateId() == null || config.getVariantBTemplateId() == null) {
            throw new BusinessException("Both template variants are required");
        }

        if (config.getVariantATemplateId().equals(config.getVariantBTemplateId())) {
            throw new BusinessException("Template variants must be different");
        }

        if (config.getTrafficSplit() != null && (config.getTrafficSplit() < 10 || config.getTrafficSplit() > 90)) {
            throw new BusinessException("Traffic split must be between 10 and 90");
        }

        if (config.getMetricToOptimize() == null || config.getMetricToOptimize().trim().isEmpty()) {
            throw new BusinessException("Metric to optimize is required");
        }
    }

    private ABTestConfiguration mapToConfiguration(TemplateABTest abTest) {
        return ABTestConfiguration.builder()
                .testId(abTest.getId())
                .testName(abTest.getTestName())
                .description(abTest.getDescription())
                .variantATemplateId(abTest.getVariantATemplate().getId())
                .variantBTemplateId(abTest.getVariantBTemplate().getId())
                .trafficSplit(abTest.getTrafficSplit())
                .metricToOptimize(abTest.getMetricToOptimize())
                .minSampleSize(abTest.getMinSampleSize())
                .status(abTest.getStatus())
                .build();
    }

    private ABTestStatus mapToStatus(TemplateABTest abTest) {
        return ABTestStatus.builder()
                .testId(abTest.getId())
                .testName(abTest.getTestName())
                .status(abTest.getStatus())
                .variantATemplateName(abTest.getVariantATemplate().getName())
                .variantBTemplateName(abTest.getVariantBTemplate().getName())
                .startedAt(abTest.getStartedAt())
                .build();
    }
}
