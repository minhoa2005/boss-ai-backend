package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Cost optimization opportunity
 */
@Data
@Builder
public class CostOptimizationOpportunity {
    private String resourceType;
    private double currentUtilization;
    private String recommendedAction;
    private double estimatedSavings;
    private String riskLevel; // LOW, MEDIUM, HIGH
}