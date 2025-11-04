package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Cost optimization analysis
 */
@Data
@Builder
public class CostOptimizationAnalysis {
    private Instant timestamp;
    private List<CostOptimizationOpportunity> opportunities;
    private double totalPotentialSavings;
    private String implementationComplexity; // LOW, MEDIUM, HIGH
    private String riskAssessment; // LOW, MEDIUM, HIGH
}