package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for creating an A/B test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestConfiguration {

    private Long testId;
    private String testName;
    private String description;
    private Long variantATemplateId;
    private Long variantBTemplateId;
    private Integer trafficSplit; // Percentage of traffic to variant A (0-100)
    private String metricToOptimize; // SUCCESS_RATE, AVERAGE_RATING, QUALITY_SCORE, etc.
    private Integer minSampleSize; // Minimum samples per variant before declaring winner
    private String status; // ACTIVE, COMPLETED, CANCELLED
}
