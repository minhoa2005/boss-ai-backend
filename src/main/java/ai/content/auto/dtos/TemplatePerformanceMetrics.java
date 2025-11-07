package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Detailed performance metrics for a template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePerformanceMetrics {

    private Integer totalUsageCount;
    private Integer last30DaysUsage;
    private Integer last7DaysUsage;
    private BigDecimal successRate;
    private Long averageGenerationTimeMs;
    private BigDecimal averageQualityScore;
    private BigDecimal averageRating;
    private BigDecimal completionRate;
    private BigDecimal retryRate;
    private BigDecimal popularityScore;
}
