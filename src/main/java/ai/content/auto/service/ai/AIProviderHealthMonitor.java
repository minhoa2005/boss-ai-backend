package ai.content.auto.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring AI provider health with periodic checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderHealthMonitor {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;

    /**
     * Perform health checks every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void performHealthChecks() {
        log.debug("Starting periodic health checks for {} providers", providers.size());

        // Run health checks in parallel for better performance
        List<CompletableFuture<Void>> healthCheckFutures = providers.stream()
                .map(this::performHealthCheckAsync)
                .toList();

        // Wait for all health checks to complete (with timeout)
        CompletableFuture<Void> allHealthChecks = CompletableFuture.allOf(
                healthCheckFutures.toArray(new CompletableFuture[0]));

        try {
            allHealthChecks.get(10, TimeUnit.SECONDS); // 10 second timeout
            log.debug("Completed health checks for all providers");
        } catch (Exception e) {
            log.warn("Health check timeout or error occurred", e);
        }
    }

    /**
     * Perform health check for a single provider asynchronously
     */
    private CompletableFuture<Void> performHealthCheckAsync(AIProvider provider) {
        return CompletableFuture.runAsync(() -> {
            try {
                performHealthCheck(provider);
            } catch (Exception e) {
                log.error("Error during health check for provider: {}", provider.getName(), e);
            }
        });
    }

    /**
     * Perform health check for a single provider
     */
    private void performHealthCheck(AIProvider provider) {
        String providerName = provider.getName();

        try {
            log.debug("Performing health check for provider: {}", providerName);

            // Get current health status (this will trigger the provider's health check
            // logic)
            ProviderHealthStatus healthStatus = provider.getHealthStatus();

            // Update metrics with the health status
            metricsService.updateHealthStatus(providerName, healthStatus);

            // Log health status changes
            logHealthStatusChange(providerName, healthStatus);

        } catch (Exception e) {
            log.error("Health check failed for provider: {}", providerName, e);

            // Create a failure health status
            ProviderHealthStatus failureStatus = ProviderHealthStatus.builder()
                    .healthLevel(ProviderHealthStatus.HealthLevel.DOWN)
                    .isAvailable(false)
                    .message("Health check failed: " + e.getMessage())
                    .build();

            metricsService.updateHealthStatus(providerName, failureStatus);
        }
    }

    /**
     * Log significant health status changes
     */
    private void logHealthStatusChange(String providerName, ProviderHealthStatus healthStatus) {
        // Get previous health status from cache/metrics to detect changes
        ProviderHealthStatus previousStatus = metricsService.getProviderHealthStatus(providerName);

        if (previousStatus == null ||
                previousStatus.getHealthLevel() != healthStatus.getHealthLevel()) {

            log.info("Provider {} health status changed: {} -> {} ({})",
                    providerName,
                    previousStatus != null ? previousStatus.getHealthLevel() : "UNKNOWN",
                    healthStatus.getHealthLevel(),
                    healthStatus.getMessage());
        }

        // Log warnings for degraded or unhealthy providers
        if (healthStatus.getHealthLevel() == ProviderHealthStatus.HealthLevel.DEGRADED ||
                healthStatus.getHealthLevel() == ProviderHealthStatus.HealthLevel.UNHEALTHY) {

            log.warn("Provider {} is {} - consecutive failures: {}, error rate: {:.1f}%",
                    providerName,
                    healthStatus.getHealthLevel().name().toLowerCase(),
                    healthStatus.getConsecutiveFailures(),
                    healthStatus.getErrorRate() * 100);
        }

        // Log errors for down providers
        if (healthStatus.getHealthLevel() == ProviderHealthStatus.HealthLevel.DOWN) {
            log.error("Provider {} is DOWN - {}", providerName, healthStatus.getMessage());
        }
    }

    /**
     * Get health summary for all providers
     */
    public ProviderHealthSummary getHealthSummary() {
        int totalProviders = providers.size();
        int healthyProviders = 0;
        int degradedProviders = 0;
        int unhealthyProviders = 0;
        int downProviders = 0;

        for (AIProvider provider : providers) {
            try {
                ProviderHealthStatus health = provider.getHealthStatus();
                switch (health.getHealthLevel()) {
                    case HEALTHY -> healthyProviders++;
                    case DEGRADED -> degradedProviders++;
                    case UNHEALTHY -> unhealthyProviders++;
                    case DOWN -> downProviders++;
                }
            } catch (Exception e) {
                log.warn("Error getting health status for provider: {}", provider.getName(), e);
                downProviders++; // Count as down if we can't get status
            }
        }

        return ProviderHealthSummary.builder()
                .totalProviders(totalProviders)
                .healthyProviders(healthyProviders)
                .degradedProviders(degradedProviders)
                .unhealthyProviders(unhealthyProviders)
                .downProviders(downProviders)
                .overallHealthy(downProviders == 0 && unhealthyProviders == 0)
                .build();
    }

    /**
     * Force health check for all providers (for manual triggering)
     */
    public void forceHealthCheck() {
        log.info("Forcing health check for all providers");
        performHealthChecks();
    }

    /**
     * Force health check for a specific provider
     */
    public void forceHealthCheck(String providerName) {
        log.info("Forcing health check for provider: {}", providerName);

        AIProvider provider = providers.stream()
                .filter(p -> p.getName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerName));

        performHealthCheck(provider);
    }
}