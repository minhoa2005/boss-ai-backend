package ai.content.auto.service.ai;

import ai.content.auto.service.monitoring.SystemAlert;
import ai.content.auto.service.monitoring.SystemMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for AI provider alerting and monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderAlertingService {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;
    private final SystemMonitoringService systemMonitoringService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.provider.alert.cost.daily-budget:100.00}")
    private BigDecimal dailyCostBudget;

    @Value("${ai.provider.alert.cost.monthly-budget:2500.00}")
    private BigDecimal monthlyCostBudget;

    @Value("${ai.provider.alert.performance.response-time-threshold:30000}")
    private long responseTimeThreshold;

    @Value("${ai.provider.alert.performance.error-rate-threshold:0.05}")
    private double errorRateThreshold;

    @Value("${ai.provider.alert.performance.consecutive-failures-threshold:5}")
    private int consecutiveFailuresThreshold;

    private static final String ALERT_PREFIX = "ai:provider:alert:";
    private static final String COST_PREFIX = "ai:provider:cost:";
    private static final int ALERT_COOLDOWN_MINUTES = 30;

    /**
     * Monitor provider performance and trigger alerts every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void monitorProviderPerformance() {
        log.debug("Starting provider performance monitoring");

        for (AIProvider provider : providers) {
            try {
                monitorProviderHealth(provider);
                monitorProviderPerformance(provider);
                monitorProviderCosts(provider);
            } catch (Exception e) {
                log.error("Error monitoring provider: {}", provider.getName(), e);
            }
        }
    }

    /**
     * Monitor provider health and trigger alerts for failures
     */
    private void monitorProviderHealth(AIProvider provider) {
        String providerName = provider.getName();
        ProviderHealthStatus health = provider.getHealthStatus();

        // Check for provider down
        if (health.getHealthLevel() == ProviderHealthStatus.HealthLevel.DOWN) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.PROVIDER_DOWN,
                    SystemMonitoringService.AlertSeverity.CRITICAL,
                    String.format("Provider %s is DOWN: %s", providerName, health.getMessage()));
        }

        // Check for consecutive failures
        if (health.getConsecutiveFailures() >= consecutiveFailuresThreshold) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.HIGH_FAILURE_RATE,
                    SystemMonitoringService.AlertSeverity.HIGH,
                    String.format("Provider %s has %d consecutive failures",
                            providerName, health.getConsecutiveFailures()));
        }

        // Check for degraded performance
        if (health.getHealthLevel() == ProviderHealthStatus.HealthLevel.DEGRADED) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.PERFORMANCE_DEGRADED,
                    SystemMonitoringService.AlertSeverity.MEDIUM,
                    String.format("Provider %s performance is degraded: error rate %.1f%%",
                            providerName, health.getErrorRate() * 100));
        }
    }

    /**
     * Monitor provider performance metrics
     */
    private void monitorProviderPerformance(AIProvider provider) {
        String providerName = provider.getName();
        Map<String, Object> metrics = metricsService.getProviderMetrics(providerName);

        // Check response time
        Object avgResponseTimeObj = metrics.get("avg_response_time");
        if (avgResponseTimeObj != null) {
            long avgResponseTime = Long.parseLong(avgResponseTimeObj.toString());
            if (avgResponseTime > responseTimeThreshold) {
                createProviderAlert(
                        providerName,
                        ProviderAlertType.SLOW_RESPONSE,
                        SystemMonitoringService.AlertSeverity.MEDIUM,
                        String.format("Provider %s average response time is %dms (threshold: %dms)",
                                providerName, avgResponseTime, responseTimeThreshold));
            }
        }

        // Check error rate
        Object errorRateObj = metrics.get("error_rate");
        if (errorRateObj != null) {
            double errorRate = Double.parseDouble(errorRateObj.toString());
            if (errorRate > errorRateThreshold) {
                createProviderAlert(
                        providerName,
                        ProviderAlertType.HIGH_ERROR_RATE,
                        SystemMonitoringService.AlertSeverity.HIGH,
                        String.format("Provider %s error rate is %.1f%% (threshold: %.1f%%)",
                                providerName, errorRate * 100, errorRateThreshold * 100));
            }
        }
    }

    /**
     * Monitor provider costs and budget alerts
     */
    private void monitorProviderCosts(AIProvider provider) {
        String providerName = provider.getName();

        // Check daily costs
        BigDecimal dailyCost = getDailyCost(providerName);
        if (dailyCost.compareTo(dailyCostBudget) > 0) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.DAILY_BUDGET_EXCEEDED,
                    SystemMonitoringService.AlertSeverity.HIGH,
                    String.format("Provider %s daily cost $%.2f exceeds budget $%.2f",
                            providerName, dailyCost, dailyCostBudget));
        } else if (dailyCost.compareTo(dailyCostBudget.multiply(BigDecimal.valueOf(0.8))) > 0) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.DAILY_BUDGET_WARNING,
                    SystemMonitoringService.AlertSeverity.MEDIUM,
                    String.format("Provider %s daily cost $%.2f is 80%% of budget $%.2f",
                            providerName, dailyCost, dailyCostBudget));
        }

        // Check monthly costs
        BigDecimal monthlyCost = getMonthlyCost(providerName);
        if (monthlyCost.compareTo(monthlyCostBudget) > 0) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.MONTHLY_BUDGET_EXCEEDED,
                    SystemMonitoringService.AlertSeverity.CRITICAL,
                    String.format("Provider %s monthly cost $%.2f exceeds budget $%.2f",
                            providerName, monthlyCost, monthlyCostBudget));
        } else if (monthlyCost.compareTo(monthlyCostBudget.multiply(BigDecimal.valueOf(0.9))) > 0) {
            createProviderAlert(
                    providerName,
                    ProviderAlertType.MONTHLY_BUDGET_WARNING,
                    SystemMonitoringService.AlertSeverity.HIGH,
                    String.format("Provider %s monthly cost $%.2f is 90%% of budget $%.2f",
                            providerName, monthlyCost, monthlyCostBudget));
        }
    }

    /**
     * Create provider alert with cooldown to prevent spam
     */
    private void createProviderAlert(String providerName, ProviderAlertType alertType,
            SystemMonitoringService.AlertSeverity severity, String message) {

        String alertKey = ALERT_PREFIX + providerName + ":" + alertType.name();

        // Check if alert is in cooldown period
        if (isAlertInCooldown(alertKey)) {
            log.debug("Alert {} is in cooldown period, skipping", alertKey);
            return;
        }

        try {
            // Create system alert
            SystemAlert alert = systemMonitoringService.createAlert(
                    SystemMonitoringService.AlertType.AI_PROVIDER,
                    String.format("[%s] %s", providerName, message),
                    severity);

            // Set cooldown period
            redisTemplate.opsForValue().set(alertKey, Instant.now().toEpochMilli(),
                    ALERT_COOLDOWN_MINUTES, TimeUnit.MINUTES);

            log.warn("Created provider alert: {} - {} - {}", providerName, alertType, message);

            // Store alert details for dashboard
            storeProviderAlert(providerName, alertType, severity, message, alert);

        } catch (Exception e) {
            log.error("Failed to create provider alert for {}: {}", providerName, message, e);
        }
    }

    /**
     * Check if alert is in cooldown period
     */
    private boolean isAlertInCooldown(String alertKey) {
        Object lastAlertTime = redisTemplate.opsForValue().get(alertKey);
        if (lastAlertTime == null) {
            return false;
        }

        long lastAlert = Long.parseLong(lastAlertTime.toString());
        long cooldownEnd = lastAlert + (ALERT_COOLDOWN_MINUTES * 60 * 1000);

        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Store provider alert for dashboard and reporting
     */
    private void storeProviderAlert(String providerName, ProviderAlertType alertType,
            SystemMonitoringService.AlertSeverity severity, String message, SystemAlert alert) {
        try {
            String alertHistoryKey = ALERT_PREFIX + providerName + ":history";

            Map<String, Object> alertData = new HashMap<>();
            alertData.put("type", alertType.name());
            alertData.put("severity", severity.name());
            alertData.put("message", message);
            alertData.put("timestamp", Instant.now().toEpochMilli());
            alertData.put("alert_id", alert.getId());

            // Store in Redis list (keep last 100 alerts per provider)
            redisTemplate.opsForList().leftPush(alertHistoryKey, alertData);
            redisTemplate.opsForList().trim(alertHistoryKey, 0, 99);
            redisTemplate.expire(alertHistoryKey, 30, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("Failed to store provider alert history for {}", providerName, e);
        }
    }

    /**
     * Get daily cost for a provider
     */
    private BigDecimal getDailyCost(String providerName) {
        try {
            String costKey = COST_PREFIX + providerName + ":daily:" +
                    Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli();

            Object cost = redisTemplate.opsForValue().get(costKey);
            if (cost == null) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(cost.toString());
        } catch (Exception e) {
            log.error("Failed to get daily cost for provider: {}", providerName, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get monthly cost for a provider
     */
    private BigDecimal getMonthlyCost(String providerName) {
        try {
            // Sum costs for current month
            Instant startOfMonth = Instant.now().truncatedTo(ChronoUnit.DAYS)
                    .minus(Instant.now().atZone(java.time.ZoneOffset.UTC).getDayOfMonth() - 1, ChronoUnit.DAYS);

            BigDecimal totalCost = BigDecimal.ZERO;

            for (int day = 0; day < 31; day++) {
                String costKey = COST_PREFIX + providerName + ":daily:" +
                        startOfMonth.plus(day, ChronoUnit.DAYS).toEpochMilli();

                Object cost = redisTemplate.opsForValue().get(costKey);
                if (cost != null) {
                    totalCost = totalCost.add(new BigDecimal(cost.toString()));
                }
            }

            return totalCost;
        } catch (Exception e) {
            log.error("Failed to get monthly cost for provider: {}", providerName, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Record cost for a provider request
     */
    public void recordProviderCost(String providerName, BigDecimal cost) {
        try {
            String dailyCostKey = COST_PREFIX + providerName + ":daily:" +
                    Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli();

            // Increment daily cost
            redisTemplate.opsForValue().increment(dailyCostKey, cost.doubleValue());
            redisTemplate.expire(dailyCostKey, 32, TimeUnit.DAYS); // Keep for a month + buffer

            // Also track total cost
            String totalCostKey = COST_PREFIX + providerName + ":total";
            redisTemplate.opsForValue().increment(totalCostKey, cost.doubleValue());

            log.debug("Recorded cost for provider {}: ${}", providerName, cost);

        } catch (Exception e) {
            log.error("Failed to record cost for provider: {}", providerName, e);
        }
    }

    /**
     * Get provider alert history
     */
    public List<Map<String, Object>> getProviderAlertHistory(String providerName, int limit) {
        try {
            String alertHistoryKey = ALERT_PREFIX + providerName + ":history";

            List<Object> alerts = redisTemplate.opsForList().range(alertHistoryKey, 0, limit - 1);
            if (alerts == null) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> alertHistory = new ArrayList<>();
            for (Object alert : alerts) {
                if (alert instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> alertMap = (Map<String, Object>) alert;
                    alertHistory.add(alertMap);
                }
            }

            return alertHistory;

        } catch (Exception e) {
            log.error("Failed to get alert history for provider: {}", providerName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get cost summary for all providers
     */
    public Map<String, ProviderCostSummary> getProviderCostSummary() {
        Map<String, ProviderCostSummary> costSummary = new HashMap<>();

        for (AIProvider provider : providers) {
            String providerName = provider.getName();

            BigDecimal dailyCost = getDailyCost(providerName);
            BigDecimal monthlyCost = getMonthlyCost(providerName);

            // Get total cost
            BigDecimal totalCost = BigDecimal.ZERO;
            try {
                String totalCostKey = COST_PREFIX + providerName + ":total";
                Object cost = redisTemplate.opsForValue().get(totalCostKey);
                if (cost != null) {
                    totalCost = new BigDecimal(cost.toString());
                }
            } catch (Exception e) {
                log.error("Failed to get total cost for provider: {}", providerName, e);
            }

            ProviderCostSummary summary = ProviderCostSummary.builder()
                    .providerName(providerName)
                    .dailyCost(dailyCost)
                    .monthlyCost(monthlyCost)
                    .totalCost(totalCost)
                    .dailyBudget(dailyCostBudget)
                    .monthlyBudget(monthlyCostBudget)
                    .dailyBudgetUsagePercent(dailyCost.divide(dailyCostBudget, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)))
                    .monthlyBudgetUsagePercent(monthlyCost.divide(monthlyCostBudget, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)))
                    .build();

            costSummary.put(providerName, summary);
        }

        return costSummary;
    }

    /**
     * Provider alert types
     */
    public enum ProviderAlertType {
        PROVIDER_DOWN,
        HIGH_FAILURE_RATE,
        PERFORMANCE_DEGRADED,
        SLOW_RESPONSE,
        HIGH_ERROR_RATE,
        DAILY_BUDGET_EXCEEDED,
        DAILY_BUDGET_WARNING,
        MONTHLY_BUDGET_EXCEEDED,
        MONTHLY_BUDGET_WARNING
    }
}