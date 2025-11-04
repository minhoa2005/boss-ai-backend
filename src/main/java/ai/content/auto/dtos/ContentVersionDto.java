package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * DTO for ContentVersion entity.
 * Used for transferring content version data between layers.
 * 
 * Requirements: 1.1, 1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionDto {

    private Long id;
    private Long contentId;
    private Integer versionNumber;
    private String content;
    private String title;

    // Generation parameters
    private Map<String, Object> generationParams;
    private String aiProvider;
    private String aiModel;

    // Metrics
    private Integer tokensUsed;
    private BigDecimal generationCost;
    private Long processingTimeMs;
    private BigDecimal readabilityScore;
    private BigDecimal seoScore;
    private BigDecimal qualityScore;
    private BigDecimal sentimentScore;

    // Content statistics
    private Integer wordCount;
    private Integer characterCount;

    // Metadata
    private String industry;
    private String targetAudience;
    private String tone;
    private String language;

    // Version branching and tagging
    private Long parentVersionId;
    private String branchName;
    private Boolean isExperimental;
    private String versionTag;
    private String annotation;

    // Audit fields
    private Long createdBy;
    private String createdByUsername;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    // Calculated fields
    private BigDecimal overallScore;
    private Boolean isLatestVersion;
    private Integer totalVersions;

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
}