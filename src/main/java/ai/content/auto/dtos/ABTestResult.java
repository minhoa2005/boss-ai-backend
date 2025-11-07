package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Results and analysis of an A/B test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABTestResult {

    private Long testId;
    private String testName;
    private String status;

    // Variant A metrics
    private Long variantATemplateId;
    private String variantATemplateName;
    private Integer variantAUsageCount;
    private BigDecimal variantAMetricValue;

    // Variant B metrics
    private Long variantBTemplateId;
    private String variantBTemplateName;
    private Integer variantBUsageCount;
    private BigDecimal variantBMetricValue;

    // Test results
    private String metricName;
    private String winner; // VARIANT_A, VARIANT_B, TIE, INCONCLUSIVE
    private Boolean isStatisticallySignificant;
    private BigDecimal confidenceLevel; // Percentage (0-100)
    private BigDecimal improvement; // Percentage improvement of winner over loser

    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
