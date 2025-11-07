package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for template performance summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePerformanceSummary {
    private Long templateId;
    private String templateName;
    private Integer totalUsageCount;
    private Integer recentUsageCount;
    private BigDecimal averageRating;
    private BigDecimal successRate;
    private BigDecimal popularityScore;
    private BigDecimal recencyScore;
    private Long averageGenerationTimeMs;
}
