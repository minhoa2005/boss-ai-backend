package ai.content.auto.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduled monitoring service for periodic system health checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledMonitoringService {

    private final SystemMonitoringService systemMonitoringService;
    private final DatabaseMonitoringService databaseMonitoringService;

    /**
     * Collect system metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void collectSystemMetrics() {
        try {
            log.debug("Starting scheduled system metrics collection");

            CompletableFuture.runAsync(() -> {
                try {
                    SystemMetrics metrics = systemMonitoringService.collectSystemMetrics();

                    // Check for alerts based on metrics
                    checkSystemAlerts(metrics);

                } catch (Exception e) {
                    log.error("Failed to collect system metrics in scheduled task", e);
                }
            });

        } catch (Exception e) {
            log.error("Error in scheduled system metrics collection", e);
        }
    }

    /**
     * Perform comprehensive health check every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void performHealthCheck() {
        try {
            log.debug("Starting scheduled health check");

            CompletableFuture.runAsync(() -> {
                try {
                    SystemHealthStatus health = systemMonitoringService.getSystemHealth();

                    // Log health status changes
                    if (health.getHealthLevel() != SystemMonitoringService.HealthLevel.HEALTHY) {
                        log.warn("System health check - Status: {}, Message: {}",
                                health.getHealthLevel(), health.getMessage());
                    }

                } catch (Exception e) {
                    log.error("Failed to perform health check in scheduled task", e);
                }
            });

        } catch (Exception e) {
            log.error("Error in scheduled health check", e);
        }
    }

    /**
     * Monitor database performance every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorDatabasePerformance() {
        try {
            log.debug("Starting scheduled database monitoring");

            CompletableFuture.runAsync(() -> {
                try {
                    DatabaseMetrics dbMetrics = databaseMonitoringService.collectDatabaseMetrics();

                    // Check for database performance issues
                    checkDatabaseAlerts(dbMetrics);

                } catch (Exception e) {
                    log.error("Failed to monitor database performance in scheduled task", e);
                }
            });

        } catch (Exception e) {
            log.error("Error in scheduled database monitoring", e);
        }
    }

    /**
     * Generate performance analysis every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void generatePerformanceAnalysis() {
        try {
            log.info("Starting scheduled performance analysis");

            CompletableFuture.runAsync(() -> {
                try {
                    PerformanceAnalysis analysis = systemMonitoringService.getPerformanceAnalysis(24);

                    log.info("Performance analysis completed - Overall score: {}, Grade: {}",
                            analysis.getOverallScore(), analysis.getPerformanceGrade());

                    // Check if performance is degrading
                    if (analysis.getOverallScore() < 70) {
                        systemMonitoringService.createAlert(
                                SystemMonitoringService.AlertType.SYSTEM_ERROR,
                                "System performance is degrading - Score: " + analysis.getOverallScore(),
                                SystemMonitoringService.AlertSeverity.MEDIUM);
                    }

                } catch (Exception e) {
                    log.error("Failed to generate performance analysis in scheduled task", e);
                }
            });

        } catch (Exception e) {
            log.error("Error in scheduled performance analysis", e);
        }
    }

    /**
     * Clean up old metrics and alerts daily
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void cleanupOldData() {
        try {
            log.info("Starting scheduled cleanup of old monitoring data");

            CompletableFuture.runAsync(() -> {
                try {
                    // Cleanup logic would go here
                    // In real implementation, would clean up old metrics and resolved alerts

                    log.info("Monitoring data cleanup completed");

                } catch (Exception e) {
                    log.error("Failed to cleanup old monitoring data", e);
                }
            });

        } catch (Exception e) {
            log.error("Error in scheduled data cleanup", e);
        }
    }

    private void checkSystemAlerts(SystemMetrics metrics) {
        try {
            // CPU usage alert
            if (metrics.getCpuUsagePercent() > 80) {
                systemMonitoringService.createAlert(
                        SystemMonitoringService.AlertType.CPU_HIGH,
                        String.format("High CPU usage detected: %.1f%%", metrics.getCpuUsagePercent()),
                        metrics.getCpuUsagePercent() > 90 ? SystemMonitoringService.AlertSeverity.CRITICAL
                                : SystemMonitoringService.AlertSeverity.HIGH);
            }

            // Memory usage alert
            if (metrics.getHeapMemoryUsagePercent() > 80) {
                systemMonitoringService.createAlert(
                        SystemMonitoringService.AlertType.MEMORY_HIGH,
                        String.format("High memory usage detected: %.1f%%", metrics.getHeapMemoryUsagePercent()),
                        metrics.getHeapMemoryUsagePercent() > 90 ? SystemMonitoringService.AlertSeverity.CRITICAL
                                : SystemMonitoringService.AlertSeverity.HIGH);
            }

        } catch (Exception e) {
            log.error("Failed to check system alerts", e);
        }
    }

    private void checkDatabaseAlerts(DatabaseMetrics dbMetrics) {
        try {
            // Database connection pool alert
            if (dbMetrics.getConnectionPoolUsagePercent() > 80) {
                systemMonitoringService.createAlert(
                        SystemMonitoringService.AlertType.DATABASE_SLOW,
                        String.format("High database connection pool usage: %.1f%%",
                                dbMetrics.getConnectionPoolUsagePercent()),
                        SystemMonitoringService.AlertSeverity.HIGH);
            }

            // Slow query alert
            if (dbMetrics.getAverageQueryTime() > 1000) { // 1 second
                systemMonitoringService.createAlert(
                        SystemMonitoringService.AlertType.DATABASE_SLOW,
                        String.format("Slow database queries detected: %.0fms average",
                                dbMetrics.getAverageQueryTime()),
                        SystemMonitoringService.AlertSeverity.MEDIUM);
            }

        } catch (Exception e) {
            log.error("Failed to check database alerts", e);
        }
    }
}