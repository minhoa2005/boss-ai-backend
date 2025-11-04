package ai.content.auto.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Scheduled service for AI provider monitoring and data collection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderScheduledService {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;
    private final AIProviderDashboardService dashboardService;

    /**
     * Record hourly performance data for dashboard trends
     * Runs at the top of every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void recordHourlyPerformanceData() {
        log.info("Recording hourly performance data for {} providers", providers.size());

        for (AIProvider provider : providers) {
            try {
                String providerName = provider.getName();
                Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);

                if (!metrics.isEmpty()) {
                    dashboardService.recordHourlyPerformance(providerName, metrics);
                    log.debug("Recorded hourly performance for provider: {}", providerName);
                }
            } catch (Exception e) {
                log.error("Failed to record hourly performance for provider: {}", provider.getName(), e);
            }
        }

        log.info("Completed recording hourly performance data");
    }

    /**
     * Clean up old performance data
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldPerformanceData() {
        log.info("Starting cleanup of old performance data");

        try {
            // This would typically involve cleaning up Redis keys older than retention
            // period
            // For now, we rely on Redis TTL to handle cleanup automatically
            log.info("Performance data cleanup relies on Redis TTL - no manual cleanup needed");
        } catch (Exception e) {
            log.error("Failed to cleanup old performance data", e);
        }
    }

    /**
     * Generate daily performance summary
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    public void generateDailyPerformanceSummary() {
        log.info("Generating daily performance summary");

        try {
            for (AIProvider provider : providers) {
                String providerName = provider.getName();
                Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);

                // Log daily summary
                if (!metrics.isEmpty()) {
                    long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());
                    double successRate = Double.parseDouble(metrics.getOrDefault("success_rate", "0.0").toString());
                    long avgResponseTime = Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString());

                    log.info("Daily summary for {}: {} requests, {:.1f}% success rate, {}ms avg response time",
                            providerName, totalRequests, successRate * 100, avgResponseTime);
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate daily performance summary", e);
        }
    }
}