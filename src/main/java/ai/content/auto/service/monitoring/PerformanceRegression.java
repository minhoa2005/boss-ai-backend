package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Performance regression information
 */
@Data
@Builder
public class PerformanceRegression {
    private String metric;
    private double currentValue;
    private double baselineValue;
    private double regressionPercent;
    private String severity; // LOW, MEDIUM, HIGH
    private String recommendation;
}