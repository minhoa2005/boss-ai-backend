package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing content performance metrics and analytics data.
 * Tracks comprehensive performance metrics for content across different time
 * periods.
 * 
 * Requirements: 4.1, 4.2
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_performance")
public class ContentPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Min(value = 1, message = "Version number must be positive")
    @Column(name = "version_number")
    private Integer versionNumber;

    // Performance metrics
    @Min(value = 0, message = "View count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Min(value = 0, message = "Unique view count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "unique_view_count", nullable = false)
    private Integer uniqueViewCount = 0;

    @DecimalMin(value = "0.0", message = "Engagement rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Engagement rate must be between 0 and 1")
    @ColumnDefault("0.0000")
    @Column(name = "engagement_rate", precision = 5, scale = 4)
    private BigDecimal engagementRate = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Click through rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Click through rate must be between 0 and 1")
    @ColumnDefault("0.0000")
    @Column(name = "click_through_rate", precision = 5, scale = 4)
    private BigDecimal clickThroughRate = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Conversion rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Conversion rate must be between 0 and 1")
    @ColumnDefault("0.0000")
    @Column(name = "conversion_rate", precision = 5, scale = 4)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Bounce rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Bounce rate must be between 0 and 1")
    @ColumnDefault("0.0000")
    @Column(name = "bounce_rate", precision = 5, scale = 4)
    private BigDecimal bounceRate = BigDecimal.ZERO;

    // Time-based metrics
    @Column(name = "average_time_on_page")
    private Integer averageTimeOnPage; // in seconds

    @ColumnDefault("0")
    @Column(name = "total_time_spent", nullable = false)
    private Integer totalTimeSpent = 0; // in seconds

    @Column(name = "session_duration_avg")
    private Integer sessionDurationAvg; // in seconds

    // Social and sharing metrics
    @Min(value = 0, message = "Share count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "share_count", nullable = false)
    private Integer shareCount = 0;

    @Min(value = 0, message = "Like count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Min(value = 0, message = "Comment count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @ColumnDefault("0.0000")
    @Column(name = "social_engagement_score", precision = 8, scale = 4)
    private BigDecimal socialEngagementScore = BigDecimal.ZERO;

    // SEO and search metrics
    @Min(value = 0, message = "Search impressions must be non-negative")
    @ColumnDefault("0")
    @Column(name = "search_impressions", nullable = false)
    private Integer searchImpressions = 0;

    @Min(value = 0, message = "Search clicks must be non-negative")
    @ColumnDefault("0")
    @Column(name = "search_clicks", nullable = false)
    private Integer searchClicks = 0;

    @Column(name = "search_position_avg", precision = 5, scale = 2)
    private BigDecimal searchPositionAvg;

    @Min(value = 0, message = "Organic traffic count must be non-negative")
    @ColumnDefault("0")
    @Column(name = "organic_traffic_count", nullable = false)
    private Integer organicTrafficCount = 0;

    // Content quality metrics
    @DecimalMin(value = "0.0", message = "Readability score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Readability score must be between 0 and 100")
    @Column(name = "readability_score", precision = 5, scale = 2)
    private BigDecimal readabilityScore;

    @DecimalMin(value = "0.0", message = "SEO score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "SEO score must be between 0 and 100")
    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @DecimalMin(value = "0.0", message = "Content quality score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Content quality score must be between 0 and 100")
    @Column(name = "content_quality_score", precision = 5, scale = 2)
    private BigDecimal contentQualityScore;

    @DecimalMin(value = "0.0", message = "User satisfaction score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "User satisfaction score must be between 0 and 100")
    @Column(name = "user_satisfaction_score", precision = 5, scale = 2)
    private BigDecimal userSatisfactionScore;

    // Performance benchmarks
    @Column(name = "industry_benchmark_score", precision = 5, scale = 2)
    private BigDecimal industryBenchmarkScore;

    @Column(name = "competitor_comparison_score", precision = 5, scale = 2)
    private BigDecimal competitorComparisonScore;

    @Size(max = 20)
    @ColumnDefault("'STABLE'")
    @Column(name = "performance_trend", length = 20)
    private String performanceTrend = "STABLE";

    // Geographic and demographic data stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_countries", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> topCountries = new java.util.ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_cities", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> topCities = new java.util.ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "age_demographics", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> ageDemographics = new java.util.HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_breakdown", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> deviceBreakdown = new java.util.HashMap<>();

    // Revenue and conversion metrics
    @ColumnDefault("0.00")
    @Column(name = "revenue_generated", precision = 12, scale = 2)
    private BigDecimal revenueGenerated = BigDecimal.ZERO;

    @Column(name = "cost_per_acquisition", precision = 10, scale = 4)
    private BigDecimal costPerAcquisition;

    @Column(name = "return_on_investment", precision = 8, scale = 4)
    private BigDecimal returnOnInvestment;

    @ColumnDefault("0.00")
    @Column(name = "conversion_value", precision = 12, scale = 2)
    private BigDecimal conversionValue = BigDecimal.ZERO;

    // Time period for metrics
    @NotNull
    @Size(max = 20)
    @ColumnDefault("'DAILY'")
    @Column(name = "measurement_period", nullable = false, length = 20)
    private String measurementPeriod = "DAILY";

    @NotNull
    @Column(name = "period_start_date", nullable = false)
    private Instant periodStartDate;

    @NotNull
    @Column(name = "period_end_date", nullable = false)
    private Instant periodEndDate;

    // Data collection metadata
    @NotNull
    @Size(max = 50)
    @ColumnDefault("'SYSTEM'")
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource = "SYSTEM";

    @NotNull
    @Size(max = 50)
    @ColumnDefault("'AUTOMATIC'")
    @Column(name = "collection_method", nullable = false, length = 50)
    private String collectionMethod = "AUTOMATIC";

    @DecimalMin(value = "0.0", message = "Data quality score must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Data quality score must be between 0 and 1")
    @ColumnDefault("1.00")
    @Column(name = "data_quality_score", precision = 3, scale = 2)
    private BigDecimal dataQualityScore = BigDecimal.ONE;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    // Audit fields
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (lastUpdatedAt == null) {
            lastUpdatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastUpdatedAt = Instant.now();
    }

    /**
     * Calculates the overall performance score based on available metrics.
     * 
     * @return Combined performance score or null if no metrics available
     */
    public BigDecimal calculateOverallPerformanceScore() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        // Engagement rate (30% weight)
        if (engagementRate != null) {
            total = total.add(engagementRate.multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(0.30)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(0.30));
        }

        // Conversion rate (25% weight)
        if (conversionRate != null) {
            total = total.add(conversionRate.multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(0.25)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(0.25));
        }

        // Content quality score (20% weight)
        if (contentQualityScore != null) {
            total = total.add(contentQualityScore.multiply(BigDecimal.valueOf(0.20)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(0.20));
        }

        // Social engagement (15% weight)
        if (socialEngagementScore != null) {
            total = total.add(socialEngagementScore.multiply(BigDecimal.valueOf(0.15)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(0.15));
        }

        // SEO score (10% weight)
        if (seoScore != null) {
            total = total.add(seoScore.multiply(BigDecimal.valueOf(0.10)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(0.10));
        }

        return totalWeight.compareTo(BigDecimal.ZERO) > 0 ? total.divide(totalWeight, 2, java.math.RoundingMode.HALF_UP)
                : null;
    }

    /**
     * Checks if the performance is trending upward based on the performance trend.
     * 
     * @return true if performance is improving
     */
    public boolean isPerformanceImproving() {
        return "IMPROVING".equals(performanceTrend);
    }
}