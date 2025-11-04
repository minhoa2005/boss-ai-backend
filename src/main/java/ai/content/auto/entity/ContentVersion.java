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
import java.util.Map;

/**
 * Entity representing a version of generated content.
 * Each content generation can have multiple versions for comparison and A/B
 * testing.
 * 
 * Requirements: 1.1, 1.2, 1.3
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_versions_content_version", columnNames = { "content_id",
                "version_number" })
})
public class ContentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @NotNull
    @Min(value = 1, message = "Version number must be positive")
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotNull
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Size(max = 500)
    @Column(name = "title", length = 500)
    private String title;

    // Generation parameters stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generation_params", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> generationParams = new java.util.HashMap<>();

    @NotNull
    @Size(max = 50)
    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider;

    @Size(max = 100)
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    // Metrics and performance data
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generation_cost", precision = 10, scale = 6)
    private BigDecimal generationCost;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // Quality metrics with validation
    @DecimalMin(value = "0.0", message = "Readability score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Readability score must be between 0 and 100")
    @Column(name = "readability_score", precision = 5, scale = 2)
    private BigDecimal readabilityScore;

    @DecimalMin(value = "0.0", message = "SEO score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "SEO score must be between 0 and 100")
    @Column(name = "seo_score", precision = 5, scale = 2)
    private BigDecimal seoScore;

    @DecimalMin(value = "0.0", message = "Quality score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Quality score must be between 0 and 100")
    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @DecimalMin(value = "-1.0", message = "Sentiment score must be between -1 and 1")
    @DecimalMax(value = "1.0", message = "Sentiment score must be between -1 and 1")
    @Column(name = "sentiment_score", precision = 5, scale = 2)
    private BigDecimal sentimentScore;

    // Word and character counts
    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "character_count")
    private Integer characterCount;

    // Additional metadata
    @Size(max = 100)
    @Column(name = "industry", length = 100)
    private String industry;

    @Size(max = 200)
    @Column(name = "target_audience", length = 200)
    private String targetAudience;

    @Size(max = 50)
    @Column(name = "tone", length = 50)
    private String tone;

    @Size(max = 10)
    @ColumnDefault("'vi'")
    @Column(name = "language", length = 10)
    private String language;

    // Version branching and tagging support
    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Size(max = 50)
    @Column(name = "branch_name", length = 50)
    private String branchName;

    @Column(name = "is_experimental")
    @ColumnDefault("false")
    private Boolean isExperimental;

    @Size(max = 100)
    @Column(name = "version_tag", length = 100)
    private String versionTag;

    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    // Audit fields
    @NotNull
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

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
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private User createdByUser;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (language == null) {
            language = "vi";
        }
        if (isExperimental == null) {
            isExperimental = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculates the overall performance score based on available metrics.
     * 
     * @return Combined performance score or null if no metrics available
     */
    public BigDecimal calculateOverallScore() {
        if (qualityScore == null && readabilityScore == null && seoScore == null) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        if (qualityScore != null) {
            total = total.add(qualityScore.multiply(BigDecimal.valueOf(0.5))); // 50% weight
            count++;
        }
        if (readabilityScore != null) {
            total = total.add(readabilityScore.multiply(BigDecimal.valueOf(0.3))); // 30% weight
            count++;
        }
        if (seoScore != null) {
            total = total.add(seoScore.multiply(BigDecimal.valueOf(0.2))); // 20% weight
            count++;
        }

        return count > 0 ? total : null;
    }

    /**
     * Checks if this version has better performance than another version.
     * 
     * @param other The other version to compare against
     * @return true if this version performs better
     */
    public boolean performsBetterThan(ContentVersion other) {
        BigDecimal thisScore = calculateOverallScore();
        BigDecimal otherScore = other.calculateOverallScore();

        if (thisScore == null || otherScore == null) {
            return false;
        }

        return thisScore.compareTo(otherScore) > 0;
    }
}