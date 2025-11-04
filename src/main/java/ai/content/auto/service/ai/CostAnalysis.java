package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cost analysis for AI providers
 */
@Data
@Builder
public class CostAnalysis {
    private String providerName;
    private BigDecimal costPerToken;
    private BigDecimal costPerRequest;
    private BigDecimal totalCost;
    private long totalRequests;
    private double marketShare;
    private double averageQualityScore;
    private long averageResponseTime;

    public BigDecimal getCostPerRequest() {
        return costPerRequest;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public double getMarketShare() {
        return marketShare;
    }

    public void setMarketShare(double marketShare) {
        this.marketShare = marketShare;
    }
}

/**
 * Cost optimization action
 */
@Data
@Builder
class CostOptimizationAction {
    private String actionType; // SWITCH_PROVIDER, ADJUST_ROUTING, OPTIMIZE_USAGE
    private String description;
    private BigDecimal potentialSavings;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String implementation;
}