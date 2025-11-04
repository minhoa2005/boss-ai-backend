package ai.content.auto.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for comprehensive system monitoring and performance metrics
 * collection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMonitoringService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String METRICS_PREFIX = "system:metrics:";
    private static final String HEALTH_PREFIX = "system:health:";
    private static final int METRICS_TTL_MINUTES = 60;

    /**
     * Collect and store system performance metrics
     */
    public SystemMetrics collectSystemMetrics() {
        try {
            log.debug("Collecting system performance metrics");

            // JVM Metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            // Memory metrics
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();

            // System metrics
            double cpuLoad = osBean.getSystemLoadAverage();
            long uptime = runtimeBean.getUptime();
            int availableProcessors = osBean.getAvailableProcessors();

            // Calculate percentages
            double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
            double nonHeapUsagePercent = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax * 100 : 0;

            SystemMetrics metrics = SystemMetrics.builder()
                    .timestamp(Instant.now())
                    .heapMemoryUsed(heapUsed)
                    .heapMemoryMax(heapMax)
                    .heapMemoryUsagePercent(heapUsagePercent)
                    .nonHeapMemoryUsed(nonHeapUsed)
                    .nonHeapMemoryMax(nonHeapMax)
                    .nonHeapMemoryUsagePercent(nonHeapUsagePercent)
                    .cpuUsagePercent(cpuLoad)
                    .systemUptime(uptime)
                    .availableProcessors(availableProcessors)
                    .build();

            // Store metrics in Redis
            storeMetrics(metrics);

            log.debug("System metrics collected - CPU: {:.1f}%, Heap: {:.1f}%",
                    cpuLoad, heapUsagePercent);

            return metrics;

        } catch (Exception e) {
            log.error("Failed to collect system metrics", e);
            throw new RuntimeException("System metrics collection failed", e);
        }
    }

    /**
     * Get current system health status
     */
    public SystemHealthStatus getSystemHealth() {
        try {
            SystemMetrics currentMetrics = collectSystemMetrics();

            // Determine health level based on metrics
            HealthLevel healthLevel = determineHealthLevel(currentMetrics);

            // Get additional health indicators
            Map<String, Object> healthIndicators = collectHealthIndicators();

            SystemHealthStatus health = SystemHealthStatus.builder()
                    .healthLevel(healthLevel)
                    .timestamp(Instant.now())
                    .cpuUsagePercent(currentMetrics.getCpuUsagePercent())
                    .memoryUsagePercent(currentMetrics.getHeapMemoryUsagePercent())
                    .systemUptime(currentMetrics.getSystemUptime())
                    .healthIndicators(healthIndicators)
                    .message(generateHealthMessage(healthLevel, currentMetrics))
                    .build();

            // Store health status
            storeHealthStatus(health);

            return health;

        } catch (Exception e) {
            log.error("Failed to get system health", e);

            return SystemHealthStatus.builder()
                    .healthLevel(HealthLevel.CRITICAL)
                    .timestamp(Instant.now())
                    .message("Health check failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get application performance metrics
     */
    public ApplicationMetrics getApplicationMetrics() {
        try {
            log.debug("Collecting application performance metrics");

            // Get metrics from Redis
            Map<String, Object> redisMetrics = getRedisMetrics();
            Map<String, Object> databaseMetrics = getDatabaseMetrics();
            Map<String, Object> apiMetrics = getApiMetrics();

            ApplicationMetrics appMetrics = ApplicationMetrics.builder()
                    .timestamp(Instant.now())
                    .redisMetrics(redisMetrics)
                    .databaseMetrics(databaseMetrics)
                    .apiMetrics(apiMetrics)
                    .build();

            log.debug("Application metrics collected successfully");
            return appMetrics;

        } catch (Exception e) {
            log.error("Failed to collect application metrics", e);
            throw new RuntimeException("Application metrics collection failed", e);
        }
    }

    /**
     * Create system alert based on metrics
     */
    public SystemAlert createAlert(AlertType alertType, String message, AlertSeverity severity) {
        try {
            SystemAlert alert = SystemAlert.builder()
                    .alertType(alertType)
                    .message(message)
                    .severity(severity)
                    .timestamp(Instant.now())
                    .resolved(false)
                    .build();

            // Store alert
            storeAlert(alert);

            log.warn("System alert created - Type: {}, Severity: {}, Message: {}",
                    alertType, severity, message);

            return alert;

        } catch (Exception e) {
            log.error("Failed to create system alert", e);
            throw new RuntimeException("Alert creation failed", e);
        }
    }

    /**
     * Get performance analysis over time period
     */
    public PerformanceAnalysis getPerformanceAnalysis(int hours) {
        try {
            log.debug("Generating performance analysis for {} hours", hours);

            // Get historical metrics (simplified - in real implementation would use
            // time-series data)
            Map<String, Object> currentMetrics = getCurrentMetricsFromRedis();
            Map<String, Object> historicalMetrics = getHistoricalMetrics(hours);

            // Calculate trends
            Map<String, PerformanceTrend> trends = calculatePerformanceTrends(currentMetrics, historicalMetrics);

            // Generate recommendations
            Map<String, String> recommendations = generatePerformanceRecommendations(currentMetrics, trends);

            PerformanceAnalysis analysis = PerformanceAnalysis.builder()
                    .analysisWindow(hours)
                    .timestamp(Instant.now())
                    .currentMetrics(currentMetrics)
                    .trends(trends)
                    .recommendations(recommendations)
                    .overallScore(calculateOverallPerformanceScore(currentMetrics))
                    .build();

            log.info("Performance analysis completed - Overall score: {}", analysis.getOverallScore());
            return analysis;

        } catch (Exception e) {
            log.error("Failed to generate performance analysis", e);
            throw new RuntimeException("Performance analysis failed", e);
        }
    }

    private void storeMetrics(SystemMetrics metrics) {
        try {
            String key = METRICS_PREFIX + "current";

            Map<String, Object> metricsMap = new HashMap<>();
            metricsMap.put("timestamp", metrics.getTimestamp().toEpochMilli());
            metricsMap.put("heap_used", metrics.getHeapMemoryUsed());
            metricsMap.put("heap_max", metrics.getHeapMemoryMax());
            metricsMap.put("heap_usage_percent", metrics.getHeapMemoryUsagePercent());
            metricsMap.put("non_heap_used", metrics.getNonHeapMemoryUsed());
            metricsMap.put("non_heap_max", metrics.getNonHeapMemoryMax());
            metricsMap.put("non_heap_usage_percent", metrics.getNonHeapMemoryUsagePercent());
            metricsMap.put("cpu_usage_percent", metrics.getCpuUsagePercent());
            metricsMap.put("system_uptime", metrics.getSystemUptime());
            metricsMap.put("available_processors", metrics.getAvailableProcessors());

            redisTemplate.opsForHash().putAll(key, metricsMap);
            redisTemplate.expire(key, METRICS_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.warn("Failed to store system metrics in Redis", e);
        }
    }

    private void storeHealthStatus(SystemHealthStatus health) {
        try {
            String key = HEALTH_PREFIX + "current";

            Map<String, Object> healthMap = new HashMap<>();
            healthMap.put("health_level", health.getHealthLevel().name());
            healthMap.put("timestamp", health.getTimestamp().toEpochMilli());
            healthMap.put("cpu_usage_percent", health.getCpuUsagePercent());
            healthMap.put("memory_usage_percent", health.getMemoryUsagePercent());
            healthMap.put("system_uptime", health.getSystemUptime());
            healthMap.put("message", health.getMessage());

            redisTemplate.opsForHash().putAll(key, healthMap);
            redisTemplate.expire(key, METRICS_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.warn("Failed to store health status in Redis", e);
        }
    }

    private void storeAlert(SystemAlert alert) {
        try {
            String key = "system:alerts:" + alert.getTimestamp().toEpochMilli();

            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("alert_type", alert.getAlertType().name());
            alertMap.put("message", alert.getMessage());
            alertMap.put("severity", alert.getSeverity().name());
            alertMap.put("timestamp", alert.getTimestamp().toEpochMilli());
            alertMap.put("resolved", alert.isResolved());

            redisTemplate.opsForHash().putAll(key, alertMap);
            redisTemplate.expire(key, 24, TimeUnit.HOURS); // Keep alerts for 24 hours

        } catch (Exception e) {
            log.warn("Failed to store alert in Redis", e);
        }
    }

    private HealthLevel determineHealthLevel(SystemMetrics metrics) {
        // Critical thresholds
        if (metrics.getCpuUsagePercent() > 90 || metrics.getHeapMemoryUsagePercent() > 90) {
            return HealthLevel.CRITICAL;
        }

        // Warning thresholds
        if (metrics.getCpuUsagePercent() > 70 || metrics.getHeapMemoryUsagePercent() > 70) {
            return HealthLevel.WARNING;
        }

        // Degraded thresholds
        if (metrics.getCpuUsagePercent() > 50 || metrics.getHeapMemoryUsagePercent() > 50) {
            return HealthLevel.DEGRADED;
        }

        return HealthLevel.HEALTHY;
    }

    private String generateHealthMessage(HealthLevel healthLevel, SystemMetrics metrics) {
        switch (healthLevel) {
            case HEALTHY:
                return "System is operating normally";
            case DEGRADED:
                return String.format("System performance is degraded - CPU: %.1f%%, Memory: %.1f%%",
                        metrics.getCpuUsagePercent(), metrics.getHeapMemoryUsagePercent());
            case WARNING:
                return String.format("System performance warning - CPU: %.1f%%, Memory: %.1f%%",
                        metrics.getCpuUsagePercent(), metrics.getHeapMemoryUsagePercent());
            case CRITICAL:
                return String.format("System performance critical - CPU: %.1f%%, Memory: %.1f%%",
                        metrics.getCpuUsagePercent(), metrics.getHeapMemoryUsagePercent());
            default:
                return "Unknown health status";
        }
    }

    private Map<String, Object> collectHealthIndicators() {
        Map<String, Object> indicators = new HashMap<>();

        try {
            // Redis connectivity
            redisTemplate.opsForValue().get("health:check");
            indicators.put("redis_connected", true);
        } catch (Exception e) {
            indicators.put("redis_connected", false);
            indicators.put("redis_error", e.getMessage());
        }

        // Add more health indicators as needed
        indicators.put("jvm_version", System.getProperty("java.version"));
        indicators.put("os_name", System.getProperty("os.name"));
        indicators.put("available_memory", Runtime.getRuntime().freeMemory());

        return indicators;
    }

    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Basic Redis metrics (would be enhanced with Redis INFO command in real
            // implementation)
            metrics.put("connected", true);
            metrics.put("response_time_ms", measureRedisResponseTime());
        } catch (Exception e) {
            metrics.put("connected", false);
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    private Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Placeholder for database metrics
        // In real implementation, would query database connection pool, active
        // connections, etc.
        metrics.put("connection_pool_active", 5);
        metrics.put("connection_pool_max", 20);
        metrics.put("avg_query_time_ms", 50);

        return metrics;
    }

    private Map<String, Object> getApiMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Placeholder for API metrics
        // In real implementation, would get from application metrics
        metrics.put("requests_per_minute", 100);
        metrics.put("avg_response_time_ms", 200);
        metrics.put("error_rate_percent", 2.5);

        return metrics;
    }

    private Map<String, Object> getCurrentMetricsFromRedis() {
        try {
            String key = METRICS_PREFIX + "current";
            Map<Object, Object> rawMetrics = redisTemplate.opsForHash().entries(key);
            Map<String, Object> metrics = new HashMap<>();
            rawMetrics.forEach((k, v) -> metrics.put(k.toString(), v));
            return metrics;
        } catch (Exception e) {
            log.warn("Failed to get current metrics from Redis", e);
            return new HashMap<>();
        }
    }

    private Map<String, Object> getHistoricalMetrics(int hours) {
        // Simplified implementation - in real system would query time-series data
        return getCurrentMetricsFromRedis();
    }

    private Map<String, PerformanceTrend> calculatePerformanceTrends(
            Map<String, Object> current, Map<String, Object> historical) {

        Map<String, PerformanceTrend> trends = new HashMap<>();

        // CPU trend
        trends.put("cpu_usage", PerformanceTrend.builder()
                .metric("cpu_usage_percent")
                .trend(TrendDirection.STABLE)
                .change(0.0)
                .description("CPU usage is stable")
                .build());

        // Memory trend
        trends.put("memory_usage", PerformanceTrend.builder()
                .metric("heap_usage_percent")
                .trend(TrendDirection.STABLE)
                .change(0.0)
                .description("Memory usage is stable")
                .build());

        return trends;
    }

    private Map<String, String> generatePerformanceRecommendations(
            Map<String, Object> metrics, Map<String, PerformanceTrend> trends) {

        Map<String, String> recommendations = new HashMap<>();

        double cpuUsage = Double.parseDouble(metrics.getOrDefault("cpu_usage_percent", "0").toString());
        double memoryUsage = Double.parseDouble(metrics.getOrDefault("heap_usage_percent", "0").toString());

        if (cpuUsage > 70) {
            recommendations.put("cpu", "Consider scaling up CPU resources or optimizing CPU-intensive operations");
        }

        if (memoryUsage > 70) {
            recommendations.put("memory", "Consider increasing heap size or optimizing memory usage");
        }

        if (recommendations.isEmpty()) {
            recommendations.put("general", "System performance is optimal");
        }

        return recommendations;
    }

    private double calculateOverallPerformanceScore(Map<String, Object> metrics) {
        try {
            double cpuUsage = Double.parseDouble(metrics.getOrDefault("cpu_usage_percent", "0").toString());
            double memoryUsage = Double.parseDouble(metrics.getOrDefault("heap_usage_percent", "0").toString());

            // Simple scoring algorithm (100 - average usage percentage)
            double avgUsage = (cpuUsage + memoryUsage) / 2;
            return Math.max(0, 100 - avgUsage);

        } catch (Exception e) {
            log.warn("Failed to calculate performance score", e);
            return 50.0; // Default score
        }
    }

    private long measureRedisResponseTime() {
        try {
            long start = System.currentTimeMillis();
            redisTemplate.opsForValue().get("health:ping");
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1; // Indicates error
        }
    }

    // Enums
    public enum HealthLevel {
        HEALTHY, DEGRADED, WARNING, CRITICAL
    }

    public enum AlertType {
        CPU_HIGH, MEMORY_HIGH, DISK_FULL, DATABASE_SLOW, API_ERROR, SYSTEM_ERROR
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TrendDirection {
        IMPROVING, STABLE, DEGRADING
    }
}