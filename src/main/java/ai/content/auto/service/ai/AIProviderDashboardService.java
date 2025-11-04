package ai.content.auto.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for AI provider performance dashboards
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderDashboardService {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;
    private final AIProviderAlertingService alertingService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DASHBOARD_PREFIX = "ai:provider:dashboard:";

    /**
     * Get comprehensive provider dashboard data
     */
    public ProviderDashboard getProviderDashboard() {
        log.info("Generating comprehensive provider dashboard");

        try {
            List<ProviderDashboardItem> providerItems = new ArrayList<>();

            for (AIProvider provider : providers) {
                ProviderDashboardItem item = createProviderDashboardItem(provider);
                providerItems.add(item);
            }

            // Calculate overall statistics
            ProviderOverallStats overallStats = calculateOverallStats(providerItems);

            // Get cost summary
            Map<String, ProviderCostSummary> costSummary = alertingService.getProviderCostSummary();

            return ProviderDashboard.builder()
                    .providers(providerItems)
                    .overallStats(overallStats)
                    .costSummary(costSummary)
                    .lastUpdated(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate provider dashboard", e);
            throw new RuntimeException("Failed to generate provider dashboard", e);
        }
    }

    /**
     * Create dashboard item for a single provider
     */
    private ProviderDashboardItem createProviderDashboardItem(AIProvider provider) {
        String providerName = provider.getName();

        // Get health status
        ProviderHealthStatus health = provider.getHealthStatus();

        // Get metrics
        Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);

        // Get recent alerts
        List<Map<String, Object>> recentAlerts = alertingService.getProviderAlertHistory(providerName, 5);

        // Get performance trends
        List<ProviderPerformancePoint> performanceTrend = getPerformanceTrend(providerName, 24);

        // Calculate availability percentage
        double availabilityPercent = calculateAvailabilityPercent(providerName, 24);

        return ProviderDashboardItem.builder()
                .providerName(providerName)
                .healthStatus(health)
                .metrics(metrics)
                .recentAlerts(recentAlerts)
                .performanceTrend(performanceTrend)
                .availabilityPercent(availabilityPercent)
                .isAvailable(provider.isAvailable())
                .capabilities(provider.getCapabilities())
                .costPerToken(provider.getCostPerToken())
                .build();
    }

    /**
     * Calculate overall statistics across all providers
     */
    private ProviderOverallStats calculateOverallStats(List<ProviderDashboardItem> providerItems) {
        int totalProviders = providerItems.size();
        int availableProviders = (int) providerItems.stream()
                .mapToInt(item -> item.isAvailable() ? 1 : 0)
                .sum();

        // Calculate average metrics
        double avgSuccessRate = providerItems.stream()
                .mapToDouble(item -> {
                    Object successRate = item.getMetrics().get("success_rate");
                    return successRate != null ? Double.parseDouble(successRate.toString()) : 0.0;
                })
                .average()
                .orElse(0.0);

        double avgResponseTime = providerItems.stream()
                .mapToDouble(item -> {
                    Object responseTime = item.getMetrics().get("avg_response_time");
                    return responseTime != null ? Double.parseDouble(responseTime.toString()) : 0.0;
                })
                .average()
                .orElse(0.0);

        double avgQualityScore = providerItems.stream()
                .mapToDouble(item -> {
                    Object qualityScore = item.getMetrics().get("avg_quality_score");
                    return qualityScore != null ? Double.parseDouble(qualityScore.toString()) : 0.0;
                })
                .average()
                .orElse(0.0);

        // Count total requests across all providers
        long totalRequests = providerItems.stream()
                .mapToLong(item -> {
                    Object requests = item.getMetrics().get("total_requests");
                    return requests != null ? Long.parseLong(requests.toString()) : 0L;
                })
                .sum();

        // Count alerts by severity
        Map<String, Integer> alertCounts = new HashMap<>();
        alertCounts.put("CRITICAL", 0);
        alertCounts.put("HIGH", 0);
        alertCounts.put("MEDIUM", 0);
        alertCounts.put("LOW", 0);

        for (ProviderDashboardItem item : providerItems) {
            for (Map<String, Object> alert : item.getRecentAlerts()) {
                String severity = alert.getOrDefault("severity", "LOW").toString();
                alertCounts.put(severity, alertCounts.getOrDefault(severity, 0) + 1);
            }
        }

        return ProviderOverallStats.builder()
                .totalProviders(totalProviders)
                .availableProviders(availableProviders)
                .unavailableProviders(totalProviders - availableProviders)
                .overallAvailabilityPercent((double) availableProviders / totalProviders * 100)
                .avgSuccessRate(avgSuccessRate)
                .avgResponseTime(avgResponseTime)
                .avgQualityScore(avgQualityScore)
                .totalRequests(totalRequests)
                .alertCounts(alertCounts)
                .build();
    }

    /**
     * Get performance trend for a provider over specified hours
     */
    private List<ProviderPerformancePoint> getPerformanceTrend(String providerName, int hours) {
        List<ProviderPerformancePoint> trend = new ArrayList<>();

        try {
            Instant now = Instant.now();

            for (int i = hours; i >= 0; i--) {
                Instant timestamp = now.minus(i, ChronoUnit.HOURS);
                String hourKey = DASHBOARD_PREFIX + providerName + ":hourly:" +
                        timestamp.truncatedTo(ChronoUnit.HOURS).toEpochMilli();

                Map<Object, Object> hourlyData = redisTemplate.opsForHash().entries(hourKey);

                if (!hourlyData.isEmpty()) {
                    ProviderPerformancePoint point = ProviderPerformancePoint.builder()
                            .timestamp(timestamp)
                            .successRate(Double.parseDouble(hourlyData.getOrDefault("success_rate", "0.0").toString()))
                            .responseTime(Long.parseLong(hourlyData.getOrDefault("avg_response_time", "0").toString()))
                            .requestCount(Long.parseLong(hourlyData.getOrDefault("request_count", "0").toString()))
                            .errorCount(Long.parseLong(hourlyData.getOrDefault("error_count", "0").toString()))
                            .build();

                    trend.add(point);
                } else {
                    // Add empty data point for missing hours
                    ProviderPerformancePoint point = ProviderPerformancePoint.builder()
                            .timestamp(timestamp)
                            .successRate(0.0)
                            .responseTime(0L)
                            .requestCount(0L)
                            .errorCount(0L)
                            .build();

                    trend.add(point);
                }
            }

        } catch (Exception e) {
            log.error("Failed to get performance trend for provider: {}", providerName, e);
        }

        return trend;
    }

    /**
     * Calculate availability percentage over specified hours
     */
    private double calculateAvailabilityPercent(String providerName, int hours) {
        try {
            int availableHours = 0;
            Instant now = Instant.now();

            for (int i = 0; i < hours; i++) {
                Instant timestamp = now.minus(i, ChronoUnit.HOURS);
                String hourKey = DASHBOARD_PREFIX + providerName + ":hourly:" +
                        timestamp.truncatedTo(ChronoUnit.HOURS).toEpochMilli();

                Map<Object, Object> hourlyData = redisTemplate.opsForHash().entries(hourKey);

                if (!hourlyData.isEmpty()) {
                    double successRate = Double.parseDouble(hourlyData.getOrDefault("success_rate", "0.0").toString());
                    if (successRate > 0.5) { // Consider available if success rate > 50%
                        availableHours++;
                    }
                }
            }

            return (double) availableHours / hours * 100;

        } catch (Exception e) {
            log.error("Failed to calculate availability for provider: {}", providerName, e);
            return 0.0;
        }
    }

    /**
     * Record hourly performance data for dashboard trends
     */
    public void recordHourlyPerformance(String providerName, Map<String, Object> metrics) {
        try {
            Instant now = Instant.now();
            String hourKey = DASHBOARD_PREFIX + providerName + ":hourly:" +
                    now.truncatedTo(ChronoUnit.HOURS).toEpochMilli();

            // Store hourly aggregated data
            Map<String, Object> hourlyData = new HashMap<>();
            hourlyData.put("success_rate", metrics.getOrDefault("success_rate", 0.0));
            hourlyData.put("avg_response_time", metrics.getOrDefault("avg_response_time", 0L));
            hourlyData.put("request_count", metrics.getOrDefault("total_requests", 0L));
            hourlyData.put("error_count", metrics.getOrDefault("failed_requests", 0L));
            hourlyData.put("timestamp", now.toEpochMilli());

            redisTemplate.opsForHash().putAll(hourKey, hourlyData);
            redisTemplate.expire(hourKey, 7, java.util.concurrent.TimeUnit.DAYS); // Keep for a week

        } catch (Exception e) {
            log.error("Failed to record hourly performance for provider: {}", providerName, e);
        }
    }

    /**
     * Get provider comparison data
     */
    public ProviderComparisonData getProviderComparison() {
        log.info("Generating provider comparison data");

        List<ProviderComparisonItem> comparisons = new ArrayList<>();

        for (AIProvider provider : providers) {
            String providerName = provider.getName();
            Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);
            ProviderHealthStatus health = provider.getHealthStatus();

            ProviderComparisonItem item = ProviderComparisonItem.builder()
                    .providerName(providerName)
                    .successRate(Double.parseDouble(metrics.getOrDefault("success_rate", "0.0").toString()))
                    .avgResponseTime(Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString()))
                    .avgQualityScore(Double.parseDouble(metrics.getOrDefault("avg_quality_score", "0.0").toString()))
                    .costPerToken(provider.getCostPerToken())
                    .isAvailable(provider.isAvailable())
                    .healthLevel(health.getHealthLevel())
                    .totalRequests(Long.parseLong(metrics.getOrDefault("total_requests", "0").toString()))
                    .build();

            comparisons.add(item);
        }

        // Sort by success rate descending
        comparisons.sort((a, b) -> Double.compare(b.getSuccessRate(), a.getSuccessRate()));

        return ProviderComparisonData.builder()
                .providers(comparisons)
                .generatedAt(Instant.now())
                .build();
    }

    /**
     * Get provider performance insights
     */
    public ProviderPerformanceInsights getPerformanceInsights() {
        log.info("Generating provider performance insights");

        List<String> insights = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // Analyze each provider
        for (AIProvider provider : providers) {
            String providerName = provider.getName();
            Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);
            ProviderHealthStatus health = provider.getHealthStatus();

            // Success rate insights
            double successRate = Double.parseDouble(metrics.getOrDefault("success_rate", "0.0").toString());
            if (successRate < 0.9) {
                insights.add(
                        String.format("Provider %s has low success rate: %.1f%%", providerName, successRate * 100));
                recommendations.add(String.format("Consider investigating %s provider issues or reducing its priority",
                        providerName));
            }

            // Response time insights
            long avgResponseTime = Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString());
            if (avgResponseTime > 10000) { // 10 seconds
                insights.add(String.format("Provider %s has slow response time: %dms", providerName, avgResponseTime));
                recommendations.add(String.format(
                        "Consider optimizing %s provider configuration or using it for non-urgent requests",
                        providerName));
            }

            // Cost insights
            if (provider.getCostPerToken().compareTo(java.math.BigDecimal.valueOf(0.01)) > 0) {
                insights.add(String.format("Provider %s has high cost per token: $%.4f", providerName,
                        provider.getCostPerToken()));
                recommendations.add(
                        String.format("Consider using %s provider only for high-quality requirements", providerName));
            }

            // Health insights
            if (health.getConsecutiveFailures() > 3) {
                insights.add(String.format("Provider %s has %d consecutive failures", providerName,
                        health.getConsecutiveFailures()));
                recommendations.add(String.format("Check %s provider configuration and API status", providerName));
            }
        }

        // Overall insights
        long totalRequests = providers.stream()
                .mapToLong(p -> {
                    Map<String, Object> metrics = metricsService.getProviderMetrics(p.getName());
                    return Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
                })
                .sum();

        if (totalRequests > 10000) {
            insights.add("High request volume detected - consider implementing request caching");
            recommendations.add("Implement response caching for frequently requested content types");
        }

        return ProviderPerformanceInsights.builder()
                .insights(insights)
                .recommendations(recommendations)
                .generatedAt(Instant.now())
                .build();
    }
}