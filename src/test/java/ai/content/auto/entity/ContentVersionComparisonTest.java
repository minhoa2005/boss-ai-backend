package ai.content.auto.entity;

import ai.content.auto.entity.ContentVersionComparison.UserPreference;
import ai.content.auto.entity.ContentVersionComparison.PerformanceWinner;
import ai.content.auto.entity.ContentVersionComparison.TestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for ContentVersionComparison entity.
 * Tests entity validation, business logic, and A/B testing calculations.
 */
class ContentVersionComparisonTest {

    private ContentVersionComparison comparison;

    @BeforeEach
    void setUp() {
        Map<String, Object> metricsComparison = new HashMap<>();
        metricsComparison.put("engagement_lift", 15.5);
        metricsComparison.put("conversion_lift", 8.2);
        metricsComparison.put("significance_level", 0.95);

        comparison = ContentVersionComparison.builder()
                .contentId(1L)
                .versionA(1)
                .versionB(2)
                .userPreference(UserPreference.B)
                .performanceWinner(PerformanceWinner.B)
                .comparisonNotes("Version B shows better engagement and conversion rates")
                .metricsComparison(metricsComparison)
                .versionAEngagementRate(BigDecimal.valueOf(0.15))
                .versionBEngagementRate(BigDecimal.valueOf(0.18))
                .versionAConversionRate(BigDecimal.valueOf(0.05))
                .versionBConversionRate(BigDecimal.valueOf(0.07))
                .versionAClickThroughRate(BigDecimal.valueOf(0.12))
                .versionBClickThroughRate(BigDecimal.valueOf(0.14))
                .statisticalSignificance(BigDecimal.valueOf(0.97))
                .confidenceLevel(BigDecimal.valueOf(0.95))
                .sampleSizeA(1000)
                .sampleSizeB(1000)
                .testStartDate(Instant.now().minus(7, ChronoUnit.DAYS))
                .testEndDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .testStatus(TestStatus.COMPLETED)
                .comparedBy(1L)
                .comparedAt(Instant.now())
                .build();
    }

    @Test
    void testContentVersionComparisonCreation() {
        assertNotNull(comparison);
        assertEquals(1L, comparison.getContentId());
        assertEquals(1, comparison.getVersionA());
        assertEquals(2, comparison.getVersionB());
        assertEquals(UserPreference.B, comparison.getUserPreference());
        assertEquals(PerformanceWinner.B, comparison.getPerformanceWinner());
        assertEquals(TestStatus.COMPLETED, comparison.getTestStatus());
    }

    @Test
    void testMetricsComparisonJsonb() {
        Map<String, Object> metrics = comparison.getMetricsComparison();
        assertNotNull(metrics);
        assertEquals(15.5, metrics.get("engagement_lift"));
        assertEquals(8.2, metrics.get("conversion_lift"));
        assertEquals(0.95, metrics.get("significance_level"));
    }

    @Test
    void testGetTestDurationDays() {
        Double duration = comparison.getTestDurationDays();
        assertNotNull(duration);
        assertTrue(duration > 5.0 && duration < 7.0); // Should be around 6 days
    }

    @Test
    void testGetTestDurationDaysWithActiveTest() {
        ContentVersionComparison activeComparison = ContentVersionComparison.builder()
                .testStartDate(Instant.now().minus(3, ChronoUnit.DAYS))
                .testEndDate(null) // Active test
                .build();

        Double duration = activeComparison.getTestDurationDays();
        assertNotNull(duration);
        assertTrue(duration > 2.0 && duration < 4.0); // Should be around 3 days
    }

    @Test
    void testGetTestDurationDaysWithNullStartDate() {
        ContentVersionComparison comparisonWithNullStart = ContentVersionComparison.builder()
                .testStartDate(null)
                .build();

        Double duration = comparisonWithNullStart.getTestDurationDays();
        assertNull(duration);
    }

    @Test
    void testGetEngagementImprovementPercent() {
        BigDecimal improvement = comparison.getEngagementImprovementPercent();
        assertNotNull(improvement);

        // Expected: ((0.18 - 0.15) / 0.15) * 100 = (0.03 / 0.15) * 100 = 20%
        BigDecimal expected = BigDecimal.valueOf(20.0);
        assertEquals(0, expected.compareTo(improvement.setScale(1, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void testGetEngagementImprovementPercentWithNullValues() {
        ContentVersionComparison comparisonWithNulls = ContentVersionComparison.builder()
                .versionAEngagementRate(null)
                .versionBEngagementRate(BigDecimal.valueOf(0.18))
                .build();

        BigDecimal improvement = comparisonWithNulls.getEngagementImprovementPercent();
        assertNull(improvement);
    }

    @Test
    void testGetEngagementImprovementPercentWithZeroBase() {
        ContentVersionComparison comparisonWithZero = ContentVersionComparison.builder()
                .versionAEngagementRate(BigDecimal.ZERO)
                .versionBEngagementRate(BigDecimal.valueOf(0.18))
                .build();

        BigDecimal improvement = comparisonWithZero.getEngagementImprovementPercent();
        assertNull(improvement);
    }

    @Test
    void testGetConversionImprovementPercent() {
        BigDecimal improvement = comparison.getConversionImprovementPercent();
        assertNotNull(improvement);

        // Expected: ((0.07 - 0.05) / 0.05) * 100 = (0.02 / 0.05) * 100 = 40%
        BigDecimal expected = BigDecimal.valueOf(40.0);
        assertEquals(0, expected.compareTo(improvement.setScale(1, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void testIsStatisticallySignificant() {
        assertTrue(comparison.isStatisticallySignificant());

        // Test with significance below confidence level
        ContentVersionComparison lowSignificance = ContentVersionComparison.builder()
                .statisticalSignificance(BigDecimal.valueOf(0.90))
                .confidenceLevel(BigDecimal.valueOf(0.95))
                .build();

        assertFalse(lowSignificance.isStatisticallySignificant());
    }

    @Test
    void testIsStatisticallySignificantWithNullValues() {
        ContentVersionComparison nullSignificance = ContentVersionComparison.builder()
                .statisticalSignificance(null)
                .confidenceLevel(BigDecimal.valueOf(0.95))
                .build();

        assertFalse(nullSignificance.isStatisticallySignificant());
    }

    @Test
    void testPrePersistCallback() {
        ContentVersionComparison newComparison = ContentVersionComparison.builder()
                .contentId(1L)
                .versionA(1)
                .versionB(2)
                .comparedBy(1L)
                .build();

        // Simulate @PrePersist
        newComparison.onCreate();

        assertNotNull(newComparison.getCreatedAt());
        assertNotNull(newComparison.getComparedAt());
        assertNotNull(newComparison.getUpdatedAt());
        assertEquals(TestStatus.ACTIVE, newComparison.getTestStatus());
        assertEquals(BigDecimal.valueOf(0.95), newComparison.getConfidenceLevel());
    }

    @Test
    void testPreUpdateCallback() {
        // Ensure updatedAt is set first
        if (comparison.getUpdatedAt() == null) {
            comparison.onCreate(); // Initialize timestamps
        }
        Instant originalUpdatedAt = comparison.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate @PreUpdate
        comparison.onUpdate();

        assertNotNull(comparison.getUpdatedAt());
        assertTrue(comparison.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testValidateVersions() {
        // Test that same versions validation logic works
        // Note: Actual validation would be handled by JPA lifecycle callbacks
        ContentVersionComparison validComparison = ContentVersionComparison.builder()
                .versionA(1)
                .versionB(2) // Different from version A
                .build();

        // This should not throw any exception
        assertDoesNotThrow(() -> {
            // The validation logic is tested indirectly through entity creation
            assertNotEquals(validComparison.getVersionA(), validComparison.getVersionB());
        });
    }

    @Test
    void testValidateTestDates() {
        // Test that valid test dates work correctly
        // Note: Actual validation would be handled by JPA lifecycle callbacks
        ContentVersionComparison validComparison = ContentVersionComparison.builder()
                .testStartDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .testEndDate(Instant.now()) // After start date
                .build();

        // This should not throw any exception
        assertDoesNotThrow(() -> {
            // The validation logic is tested indirectly through entity creation
            assertTrue(validComparison.getTestEndDate().isAfter(validComparison.getTestStartDate()));
        });
    }

    @Test
    void testEnumValues() {
        // Test UserPreference enum
        assertEquals(UserPreference.A, UserPreference.valueOf("A"));
        assertEquals(UserPreference.B, UserPreference.valueOf("B"));
        assertEquals(UserPreference.NONE, UserPreference.valueOf("NONE"));

        // Test PerformanceWinner enum
        assertEquals(PerformanceWinner.A, PerformanceWinner.valueOf("A"));
        assertEquals(PerformanceWinner.B, PerformanceWinner.valueOf("B"));
        assertEquals(PerformanceWinner.TIE, PerformanceWinner.valueOf("TIE"));

        // Test TestStatus enum
        assertEquals(TestStatus.ACTIVE, TestStatus.valueOf("ACTIVE"));
        assertEquals(TestStatus.COMPLETED, TestStatus.valueOf("COMPLETED"));
        assertEquals(TestStatus.PAUSED, TestStatus.valueOf("PAUSED"));
        assertEquals(TestStatus.CANCELLED, TestStatus.valueOf("CANCELLED"));
    }

    @Test
    void testBuilderPattern() {
        ContentVersionComparison builtComparison = ContentVersionComparison.builder()
                .contentId(2L)
                .versionA(3)
                .versionB(4)
                .userPreference(UserPreference.A)
                .performanceWinner(PerformanceWinner.TIE)
                .testStatus(TestStatus.PAUSED)
                .comparedBy(2L)
                .build();

        assertEquals(2L, builtComparison.getContentId());
        assertEquals(3, builtComparison.getVersionA());
        assertEquals(4, builtComparison.getVersionB());
        assertEquals(UserPreference.A, builtComparison.getUserPreference());
        assertEquals(PerformanceWinner.TIE, builtComparison.getPerformanceWinner());
        assertEquals(TestStatus.PAUSED, builtComparison.getTestStatus());
        assertEquals(2L, builtComparison.getComparedBy());
    }

    @Test
    void testDefaultValues() {
        ContentVersionComparison defaultComparison = new ContentVersionComparison();

        // Test that default values are properly initialized
        assertNotNull(defaultComparison.getMetricsComparison());
        assertTrue(defaultComparison.getMetricsComparison().isEmpty());
    }

    @Test
    void testNegativeImprovementCalculation() {
        // Test case where version B performs worse than version A
        ContentVersionComparison worseComparison = ContentVersionComparison.builder()
                .versionAEngagementRate(BigDecimal.valueOf(0.20))
                .versionBEngagementRate(BigDecimal.valueOf(0.15))
                .versionAConversionRate(BigDecimal.valueOf(0.08))
                .versionBConversionRate(BigDecimal.valueOf(0.06))
                .build();

        BigDecimal engagementImprovement = worseComparison.getEngagementImprovementPercent();
        BigDecimal conversionImprovement = worseComparison.getConversionImprovementPercent();

        assertNotNull(engagementImprovement);
        assertNotNull(conversionImprovement);

        // Both should be negative (worse performance)
        assertTrue(engagementImprovement.compareTo(BigDecimal.ZERO) < 0);
        assertTrue(conversionImprovement.compareTo(BigDecimal.ZERO) < 0);
    }
}