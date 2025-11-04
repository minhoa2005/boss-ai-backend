package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.service.monitoring.*;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for system monitoring and performance metrics
 */
@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final SystemMonitoringService systemMonitoringService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final CacheMonitoringService cacheMonitoringService;
    private final PredictiveScalingService predictiveScalingService;

    /**
     * Get current system metrics
     */
    @GetMapping("/system/metrics")
    public ResponseEntity<BaseResponse<SystemMetrics>> getSystemMetrics() {
        log.info("Getting current system metrics");

        SystemMetrics metrics = systemMonitoringService.collectSystemMetrics();

        BaseResponse<SystemMetrics> response = new BaseResponse<SystemMetrics>()
                .setErrorMessage("System metrics retrieved successfully")
                .setData(metrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get system health status
     */
    @GetMapping("/system/health")
    public ResponseEntity<BaseResponse<SystemHealthStatus>> getSystemHealth() {
        log.info("Getting system health status");

        SystemHealthStatus health = systemMonitoringService.getSystemHealth();

        BaseResponse<SystemHealthStatus> response = new BaseResponse<SystemHealthStatus>()
                .setErrorMessage("System health status retrieved successfully")
                .setData(health);

        return ResponseEntity.ok(response);
    }

    /**
     * Get application performance metrics
     */
    @GetMapping("/application/metrics")
    public ResponseEntity<BaseResponse<ApplicationMetrics>> getApplicationMetrics() {
        log.info("Getting application performance metrics");

        ApplicationMetrics metrics = systemMonitoringService.getApplicationMetrics();

        BaseResponse<ApplicationMetrics> response = new BaseResponse<ApplicationMetrics>()
                .setErrorMessage("Application metrics retrieved successfully")
                .setData(metrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get performance analysis
     */
    @GetMapping("/system/analysis")
    public ResponseEntity<BaseResponse<PerformanceAnalysis>> getPerformanceAnalysis(
            @RequestParam(defaultValue = "24") int hours) {
        log.info("Getting performance analysis for {} hours", hours);

        PerformanceAnalysis analysis = systemMonitoringService.getPerformanceAnalysis(hours);

        BaseResponse<PerformanceAnalysis> response = new BaseResponse<PerformanceAnalysis>()
                .setErrorMessage("Performance analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Create system alert
     */
    @PostMapping("/alerts")
    public ResponseEntity<BaseResponse<SystemAlert>> createAlert(
            @RequestParam SystemMonitoringService.AlertType alertType,
            @RequestParam String message,
            @RequestParam SystemMonitoringService.AlertSeverity severity) {
        log.info("Creating system alert - Type: {}, Severity: {}", alertType, severity);

        SystemAlert alert = systemMonitoringService.createAlert(alertType, message, severity);

        BaseResponse<SystemAlert> response = new BaseResponse<SystemAlert>()
                .setErrorMessage("System alert created successfully")
                .setData(alert);

        return ResponseEntity.ok(response);
    }

    /**
     * Get database metrics
     */
    @GetMapping("/database/metrics")
    public ResponseEntity<BaseResponse<DatabaseMetrics>> getDatabaseMetrics() {
        log.info("Getting database performance metrics");

        DatabaseMetrics metrics = databaseMonitoringService.collectDatabaseMetrics();

        BaseResponse<DatabaseMetrics> response = new BaseResponse<DatabaseMetrics>()
                .setErrorMessage("Database metrics retrieved successfully")
                .setData(metrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Analyze slow queries
     */
    @GetMapping("/database/slow-queries")
    public ResponseEntity<BaseResponse<SlowQueryAnalysis>> analyzeSlowQueries() {
        log.info("Analyzing database slow queries");

        SlowQueryAnalysis analysis = databaseMonitoringService.analyzeSlowQueries();

        BaseResponse<SlowQueryAnalysis> response = new BaseResponse<SlowQueryAnalysis>()
                .setErrorMessage("Slow query analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Get database optimization recommendations
     */
    @GetMapping("/database/optimization")
    public ResponseEntity<BaseResponse<DatabaseOptimizationRecommendations>> getDatabaseOptimization() {
        log.info("Getting database optimization recommendations");

        DatabaseOptimizationRecommendations recommendations = databaseMonitoringService
                .getOptimizationRecommendations();

        BaseResponse<DatabaseOptimizationRecommendations> response = new BaseResponse<DatabaseOptimizationRecommendations>()
                .setErrorMessage("Database optimization recommendations generated successfully")
                .setData(recommendations);

        return ResponseEntity.ok(response);
    }

    /**
     * Get database backup status
     */
    @GetMapping("/database/backup")
    public ResponseEntity<BaseResponse<BackupMonitoringStatus>> getBackupStatus() {
        log.info("Getting database backup status");

        BackupMonitoringStatus status = databaseMonitoringService.getBackupStatus();

        BaseResponse<BackupMonitoringStatus> response = new BaseResponse<BackupMonitoringStatus>()
                .setErrorMessage("Backup status retrieved successfully")
                .setData(status);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache metrics
     */
    @GetMapping("/cache/metrics")
    public ResponseEntity<BaseResponse<CacheMetrics>> getCacheMetrics() {
        log.info("Getting cache performance metrics");

        CacheMetrics metrics = cacheMonitoringService.collectCacheMetrics();

        BaseResponse<CacheMetrics> response = new BaseResponse<CacheMetrics>()
                .setErrorMessage("Cache metrics retrieved successfully")
                .setData(metrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Analyze cache performance
     */
    @GetMapping("/cache/analysis")
    public ResponseEntity<BaseResponse<CacheOptimizationAnalysis>> analyzeCachePerformance() {
        log.info("Analyzing cache performance");

        CacheOptimizationAnalysis analysis = cacheMonitoringService.analyzeCachePerformance();

        BaseResponse<CacheOptimizationAnalysis> response = new BaseResponse<CacheOptimizationAnalysis>()
                .setErrorMessage("Cache performance analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cache health status
     */
    @GetMapping("/cache/health")
    public ResponseEntity<BaseResponse<CacheHealthStatus>> getCacheHealth() {
        log.info("Getting cache health status");

        CacheHealthStatus health = cacheMonitoringService.getCacheHealth();

        BaseResponse<CacheHealthStatus> response = new BaseResponse<CacheHealthStatus>()
                .setErrorMessage("Cache health status retrieved successfully")
                .setData(health);

        return ResponseEntity.ok(response);
    }

    /**
     * Optimize cache performance
     */
    @PostMapping("/cache/optimize")
    public ResponseEntity<BaseResponse<CacheOptimizationResult>> optimizeCache() {
        log.info("Starting cache optimization");

        CacheOptimizationResult result = cacheMonitoringService.optimizeCache();

        BaseResponse<CacheOptimizationResult> response = new BaseResponse<CacheOptimizationResult>()
                .setErrorMessage("Cache optimization completed")
                .setData(result);

        return ResponseEntity.ok(response);
    }

    /**
     * Get comprehensive monitoring dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<BaseResponse<MonitoringDashboard>> getMonitoringDashboard() {
        log.info("Getting comprehensive monitoring dashboard data");

        try {
            // Collect all monitoring data
            SystemHealthStatus systemHealth = systemMonitoringService.getSystemHealth();
            DatabaseMetrics databaseMetrics = databaseMonitoringService.collectDatabaseMetrics();
            CacheHealthStatus cacheHealth = cacheMonitoringService.getCacheHealth();

            MonitoringDashboard dashboard = MonitoringDashboard.builder()
                    .systemHealth(systemHealth)
                    .databaseMetrics(databaseMetrics)
                    .cacheHealth(cacheHealth)
                    .overallStatus(determineOverallStatus(systemHealth, databaseMetrics, cacheHealth))
                    .lastUpdated(java.time.Instant.now())
                    .build();

            BaseResponse<MonitoringDashboard> response = new BaseResponse<MonitoringDashboard>()
                    .setErrorMessage("Monitoring dashboard data retrieved successfully")
                    .setData(dashboard);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get monitoring dashboard data", e);

            BaseResponse<MonitoringDashboard> response = new BaseResponse<MonitoringDashboard>()
                    .setErrorCode("MONITORING_ERROR")
                    .setErrorMessage("Failed to retrieve monitoring dashboard data: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String determineOverallStatus(SystemHealthStatus systemHealth,
            DatabaseMetrics databaseMetrics,
            CacheHealthStatus cacheHealth) {

        // Simple logic to determine overall system status
        if (systemHealth.getHealthLevel() == SystemMonitoringService.HealthLevel.CRITICAL ||
                !cacheHealth.isConnected()) {
            return "CRITICAL";
        }

        if (systemHealth.getHealthLevel() == SystemMonitoringService.HealthLevel.WARNING ||
                databaseMetrics.getConnectionPoolUsagePercent() > 80) {
            return "WARNING";
        }

        if (systemHealth.getHealthLevel() == SystemMonitoringService.HealthLevel.DEGRADED ||
                cacheHealth.getHitRatio() < 0.7) {
            return "DEGRADED";
        }

        return "HEALTHY";
    }

    /**
     * Analyze usage patterns for predictive scaling
     */
    @GetMapping("/predictive/usage-patterns")
    public ResponseEntity<BaseResponse<UsagePatternAnalysis>> analyzeUsagePatterns(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Analyzing usage patterns for {} days", days);

        UsagePatternAnalysis analysis = predictiveScalingService.analyzeUsagePatterns(days);

        BaseResponse<UsagePatternAnalysis> response = new BaseResponse<UsagePatternAnalysis>()
                .setErrorMessage("Usage pattern analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Generate scaling recommendations
     */
    @GetMapping("/predictive/scaling-recommendations")
    public ResponseEntity<BaseResponse<ScalingRecommendations>> getScalingRecommendations() {
        log.info("Generating scaling recommendations");

        ScalingRecommendations recommendations = predictiveScalingService.generateScalingRecommendations();

        BaseResponse<ScalingRecommendations> response = new BaseResponse<ScalingRecommendations>()
                .setErrorMessage("Scaling recommendations generated successfully")
                .setData(recommendations);

        return ResponseEntity.ok(response);
    }

    /**
     * Create capacity planning forecast
     */
    @GetMapping("/predictive/capacity-forecast")
    public ResponseEntity<BaseResponse<CapacityPlanningForecast>> getCapacityForecast(
            @RequestParam(defaultValue = "12") int months) {
        log.info("Creating capacity planning forecast for {} months", months);

        CapacityPlanningForecast forecast = predictiveScalingService.createCapacityForecast(months);

        BaseResponse<CapacityPlanningForecast> response = new BaseResponse<CapacityPlanningForecast>()
                .setErrorMessage("Capacity planning forecast created successfully")
                .setData(forecast);

        return ResponseEntity.ok(response);
    }

    /**
     * Detect performance regression
     */
    @GetMapping("/predictive/regression-analysis")
    public ResponseEntity<BaseResponse<PerformanceRegressionAnalysis>> detectPerformanceRegression() {
        log.info("Detecting performance regression");

        PerformanceRegressionAnalysis analysis = predictiveScalingService.detectPerformanceRegression();

        BaseResponse<PerformanceRegressionAnalysis> response = new BaseResponse<PerformanceRegressionAnalysis>()
                .setErrorMessage("Performance regression analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }

    /**
     * Generate cost optimization analysis
     */
    @GetMapping("/predictive/cost-optimization")
    public ResponseEntity<BaseResponse<CostOptimizationAnalysis>> getCostOptimization() {
        log.info("Generating cost optimization analysis");

        CostOptimizationAnalysis analysis = predictiveScalingService.generateCostOptimization();

        BaseResponse<CostOptimizationAnalysis> response = new BaseResponse<CostOptimizationAnalysis>()
                .setErrorMessage("Cost optimization analysis completed successfully")
                .setData(analysis);

        return ResponseEntity.ok(response);
    }
}

/**
 * Monitoring dashboard data
 */
@lombok.Data
@lombok.Builder
class MonitoringDashboard {
    private SystemHealthStatus systemHealth;
    private DatabaseMetrics databaseMetrics;
    private CacheHealthStatus cacheHealth;
    private String overallStatus;
    private java.time.Instant lastUpdated;
}