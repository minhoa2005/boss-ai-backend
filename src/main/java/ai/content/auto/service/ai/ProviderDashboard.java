package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive provider dashboard data
 */
@Data
@Builder
public class ProviderDashboard {
    private List<ProviderDashboardItem> providers;
    private ProviderOverallStats overallStats;
    private Map<String, ProviderCostSummary> costSummary;
    private Instant lastUpdated;
}

/**
 * Individual provider dashboard item
 */
@Data
@Builder
class ProviderDashboardItem {
    private String providerName;
    private ProviderHealthStatus healthStatus;
    private Map<String, Object> metrics;
    private List<Map<String, Object>> recentAlerts;
    private List<ProviderPerformancePoint> performanceTrend;
    private double availabilityPercent;
    private boolean isAvailable;
    private ProviderCapabilities capabilities;
    private java.math.BigDecimal costPerToken;
}

/**
 * Overall statistics across all providers
 */
@Data
@Builder
class ProviderOverallStats {
    private int totalProviders;
    private int availableProviders;
    private int unavailableProviders;
    private double overallAvailabilityPercent;
    private double avgSuccessRate;
    private double avgResponseTime;
    private double avgQualityScore;
    private long totalRequests;
    private Map<String, Integer> alertCounts;
}

/**
 * Performance data point for trending
 */
@Data
@Builder
class ProviderPerformancePoint {
    private Instant timestamp;
    private double successRate;
    private long responseTime;
    private long requestCount;
    private long errorCount;
}

/**
 * Provider comparison data
 */
@Data
@Builder
class ProviderComparisonData {
    private List<ProviderComparisonItem> providers;
    private Instant generatedAt;
}

/**
 * Individual provider comparison item
 */
@Data
@Builder
class ProviderComparisonItem {
    private String providerName;
    private double successRate;
    private long avgResponseTime;
    private double avgQualityScore;
    private java.math.BigDecimal costPerToken;
    private boolean isAvailable;
    private ProviderHealthStatus.HealthLevel healthLevel;
    private long totalRequests;
}

/**
 * Performance insights and recommendations
 */
@Data
@Builder
class ProviderPerformanceInsights {
    private List<String> insights;
    private List<String> recommendations;
    private Instant generatedAt;
}