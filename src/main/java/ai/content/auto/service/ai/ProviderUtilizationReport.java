package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider utilization analysis report
 */
@Data
@Builder
public class ProviderUtilizationReport {

    private Map<String, ProviderUtilization> utilizations;
    private Instant reportGeneratedAt;
    private List<String> recommendations;

    // Summary statistics
    private double averageUtilization;
    private double utilizationVariance;
    private String mostEfficientProvider;
    private String leastEfficientProvider;
}

@Data
@Builder
class ProviderUtilization {
    private String providerName;
    private long requestCount;
    private double currentLoad;
    private double successRate;
    private long averageResponseTime;
    private Instant lastUsed;

    // Calculated metrics
    private double efficiency;
    private boolean isUnderUtilized;
    private boolean isOverUtilized;
    private String utilizationLevel; // LOW, OPTIMAL, HIGH, CRITICAL
}