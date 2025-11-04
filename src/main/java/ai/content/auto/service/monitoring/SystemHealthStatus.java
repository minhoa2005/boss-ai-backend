package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * System health status information
 */
@Data
@Builder
public class SystemHealthStatus {

    private SystemMonitoringService.HealthLevel healthLevel;
    private Instant timestamp;

    // Key metrics
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private long systemUptime;

    // Health indicators
    private Map<String, Object> healthIndicators;

    // Status information
    private String message;
    private boolean isHealthy;
    private int consecutiveUnhealthyChecks;

    // Alerts
    private boolean hasActiveAlerts;
    private int activeAlertCount;
}