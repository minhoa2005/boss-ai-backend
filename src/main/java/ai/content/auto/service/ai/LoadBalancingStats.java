package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Load balancing statistics
 */
@Data
@Builder
public class LoadBalancingStats {

    private long totalRequests;
    private Map<String, Long> requestCounts;
    private Map<String, Double> distributionPercentages;
    private Map<String, Long> lastUsedTimes;

    private String currentStrategy;
    private long statsCollectedAt;

    // Distribution analysis
    private double distributionVariance;
    private String mostUsedProvider;
    private String leastUsedProvider;
    private boolean isBalanced; // true if distribution is relatively even
}