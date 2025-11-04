package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Summary of health status across all AI providers
 */
@Data
@Builder
public class ProviderHealthSummary {

    /**
     * Total number of providers
     */
    private int totalProviders;

    /**
     * Number of healthy providers
     */
    private int healthyProviders;

    /**
     * Number of degraded providers
     */
    private int degradedProviders;

    /**
     * Number of unhealthy providers
     */
    private int unhealthyProviders;

    /**
     * Number of down providers
     */
    private int downProviders;

    /**
     * Whether the overall system is healthy (no down or unhealthy providers)
     */
    private boolean overallHealthy;

    /**
     * Get the percentage of healthy providers
     */
    public double getHealthyPercentage() {
        if (totalProviders == 0) {
            return 0.0;
        }
        return (double) healthyProviders / totalProviders * 100.0;
    }

    /**
     * Get the percentage of available providers (healthy + degraded)
     */
    public double getAvailablePercentage() {
        if (totalProviders == 0) {
            return 0.0;
        }
        return (double) (healthyProviders + degradedProviders) / totalProviders * 100.0;
    }
}