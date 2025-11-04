package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Performance regression analysis
 */
@Data
@Builder
public class PerformanceRegressionAnalysis {
    private Instant timestamp;
    private List<PerformanceRegression> regressions;
    private double overallRegressionScore;
    private String alertLevel; // LOW, MEDIUM, HIGH
    private List<String> recommendations;
}