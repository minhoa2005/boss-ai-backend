package ai.content.auto.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing A/B test comparisons between different content versions.
 * Stores performance metrics and user preferences for version comparison
 * analysis.
 * 
 * Requirements: 1.3
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_version_comparisons")
public class ContentVersionComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @NotNull
    @Min(value = 1, message = "Version A must be positive")
    @Column(name = "version_a", nullable = false)
    private Integer versionA;

    @NotNull
    @Min(value = 1, message = "Version B must be positive")
    @Column(name = "version_b", nullable = false)
    private Integer versionB;

    // Comparison results
    @Enumerated(EnumType.STRING)
    @Column(name = "user_preference", length = 10)
    private UserPreference userPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_winner", length = 10)
    private PerformanceWinner performanceWinner;

    @Column(name = "comparison_notes", columnDefinition = "TEXT")
    private String comparisonNotes;

    // Metrics comparison stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_comparison", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metricsComparison = new java.util.HashMap<>();

    // Performance metrics for A/B testing
    @DecimalMin(value = "0.0", message = "Engagement rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Engagement rate must be between 0 and 1")
    @Column(name = "version_a_engagement_rate", precision = 5, scale = 4)
    private BigDecimal versionAEngagementRate;

    @DecimalMin(value = "0.0", message = "Engagement rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Engagement rate must be between 0 and 1")
    @Column(name = "version_b_engagement_rate", precision = 5, scale = 4)
    private BigDecimal versionBEngagementRate;

    @DecimalMin(value = "0.0", message = "Conversion rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Conversion rate must be between 0 and 1")
    @Column(name = "version_a_conversion_rate", precision = 5, scale = 4)
    private BigDecimal versionAConversionRate;

    @DecimalMin(value = "0.0", message = "Conversion rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Conversion rate must be between 0 and 1")
    @Column(name = "version_b_conversion_rate", precision = 5, scale = 4)
    private BigDecimal versionBConversionRate;

    @DecimalMin(value = "0.0", message = "Click-through rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Click-through rate must be between 0 and 1")
    @Column(name = "version_a_click_through_rate", precision = 5, scale = 4)
    private BigDecimal versionAClickThroughRate;

    @DecimalMin(value = "0.0", message = "Click-through rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Click-through rate must be between 0 and 1")
    @Column(name = "version_b_click_through_rate", precision = 5, scale = 4)
    private BigDecimal versionBClickThroughRate;

    // Statistical significance
    @DecimalMin(value = "0.0", message = "Statistical significance must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Statistical significance must be between 0 and 1")
    @Column(name = "statistical_significance", precision = 5, scale = 4)
    private BigDecimal statisticalSignificance;

    @DecimalMin(value = "0.0", message = "Confidence level must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Confidence level must be between 0 and 1")
    @ColumnDefault("0.95")
    @Column(name = "confidence_level", precision = 5, scale = 4)
    private BigDecimal confidenceLevel;

    @Column(name = "sample_size_a")
    private Integer sampleSizeA;

    @Column(name = "sample_size_b")
    private Integer sampleSizeB;

    // Test duration and status
    @Column(name = "test_start_date")
    private Instant testStartDate;

    @Column(name = "test_end_date")
    private Instant testEndDate;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "test_status", length = 20)
    private TestStatus testStatus;

    // Audit fields
    @NotNull
    @Column(name = "compared_by", nullable = false)
    private Long comparedBy;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "compared_at", nullable = false)
    private Instant comparedAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ContentGeneration contentGeneration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compared_by", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private User comparedByUser;

    // Enums for type safety
    public enum UserPreference {
        A, B, NONE
    }

    public enum PerformanceWinner {
        A, B, TIE
    }

    public enum TestStatus {
        ACTIVE, COMPLETED, PAUSED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (comparedAt == null) {
            comparedAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (testStatus == null) {
            testStatus = TestStatus.ACTIVE;
        }
        if (confidenceLevel == null) {
            confidenceLevel = BigDecimal.valueOf(0.95);
        }
        // Consolidated validations on persist
        validateVersions();
        validateTestDates();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        // Consolidated validations on update
        validateVersions();
        validateTestDates();
    }

    /**
     * Calculates the test duration in days.
     * 
     * @return Test duration in days, or current duration if test is still active
     */
    public Double getTestDurationDays() {
        if (testStartDate == null) {
            return null;
        }

        Instant endTime = testEndDate != null ? testEndDate : Instant.now();
        long durationSeconds = endTime.getEpochSecond() - testStartDate.getEpochSecond();
        return durationSeconds / 86400.0; // Convert to days
    }

    /**
     * Calculates the engagement rate improvement percentage.
     * 
     * @return Improvement percentage (positive means B is better, negative means A
     *         is better)
     */
    public BigDecimal getEngagementImprovementPercent() {
        if (versionAEngagementRate == null || versionBEngagementRate == null ||
                versionAEngagementRate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal difference = versionBEngagementRate.subtract(versionAEngagementRate);
        return difference.divide(versionAEngagementRate, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculates the conversion rate improvement percentage.
     * 
     * @return Improvement percentage (positive means B is better, negative means A
     *         is better)
     */
    public BigDecimal getConversionImprovementPercent() {
        if (versionAConversionRate == null || versionBConversionRate == null ||
                versionAConversionRate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal difference = versionBConversionRate.subtract(versionAConversionRate);
        return difference.divide(versionAConversionRate, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Determines if the test results are statistically significant.
     * 
     * @return true if results are statistically significant
     */
    public boolean isStatisticallySignificant() {
        return statisticalSignificance != null &&
                confidenceLevel != null &&
                statisticalSignificance.compareTo(confidenceLevel) >= 0;
    }

    /**
     * Validates that version A and version B are different.
     */
    private void validateVersions() {
        if (versionA != null && versionB != null && versionA.equals(versionB)) {
            throw new IllegalArgumentException("Version A and Version B must be different");
        }
    }

    /**
     * Validates that test end date is after start date.
     */
    private void validateTestDates() {
        if (testStartDate != null && testEndDate != null &&
                testEndDate.isBefore(testStartDate)) {
            throw new IllegalArgumentException("Test end date must be after start date");
        }
    }
}