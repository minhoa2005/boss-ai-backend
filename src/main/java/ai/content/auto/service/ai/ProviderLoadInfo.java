package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Provider load information
 */
@Data
@Builder
public class ProviderLoadInfo {
    private String providerName;
    private double currentLoad; // 0.0 to 1.0
    private int activeRequests;
    private int queuedRequests;
    private long averageResponseTime;
    private double successRate;
    private boolean isOverloaded;

    public boolean isOverloaded() {
        return currentLoad > 0.8 || isOverloaded;
    }

    public double getCurrentLoad() {
        return currentLoad;
    }
}