package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Cache health status information
 */
@Data
@Builder
public class CacheHealthStatus {
    private CacheMonitoringService.CacheHealthLevel healthLevel;
    private Instant timestamp;
    private boolean isConnected;
    private long responseTime;
    private double memoryUsagePercent;
    private double hitRatio;
    private Map<String, Object> healthIndicators;
    private String message;
}