package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Cost optimization recommendations for AI providers
 */
@Data
@Builder
public class CostOptimizationRecommendation {

    // Current state
    private double currentAverageCost;
    private double currentTotalCost;
    private long totalRequests;

    // Optimization opportunities
    private String cheapestProvider;
    private double cheapestCost;
    private double potentialSavingsPerRequest;
    private double potentialMonthlySavings;
    private double potentialAnnualSavings;

    // Recommendations
    private List<CostOptimizationAction> recommendations;
    private Map<String, CostAnalysis> providerCostAnalysis;

    // Risk assessment
    private String riskLevel;
    private List<String> riskFactors;

    // Implementation
    private String implementationComplexity;
    private int estimatedImplementationDays;
}

@Data
@Builder
class CostOptimizationAction {
    private String actionType; // SWITCH_PROVIDER, OPTIMIZE_ROUTING, etc.
    private String description;
    private double estimatedSavings;
    private String impact; // LOW, MEDIUM, HIGH
    private String effort; // LOW, MEDIUM, HIGH
    private List<String> requirements;
    private List<String> risks;
}

@Data
@Builder
class CostAnalysis {
    private String providerName;
    private double costPerToken;
    private double avgTokensPerRequest;
    private double costPerRequest;
    private long totalRequests;
    private double totalCost;
    private double marketShare; // percentage of total requests
}