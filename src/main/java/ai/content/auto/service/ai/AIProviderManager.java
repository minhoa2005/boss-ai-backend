package ai.content.auto.service.ai;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for managing AI providers with transparent selection and failover
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderManager {

    private final List<AIProvider> providers;
    private final AIProviderMetricsService metricsService;
    private final AIProviderLoadBalancer loadBalancer;
    private final AIProviderAlertingService alertingService;

    // Circuit breaker state for each provider
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    // Provider scoring weights
    private static final double COST_WEIGHT = 0.40;
    private static final double AVAILABILITY_WEIGHT = 0.30;
    private static final double QUALITY_WEIGHT = 0.20;
    private static final double RESPONSE_TIME_WEIGHT = 0.10;

    // Circuit breaker configuration
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000; // 30 seconds
    private static final long EXPONENTIAL_BACKOFF_BASE_MS = 1000; // 1 second
    private static final int MAX_BACKOFF_ATTEMPTS = 5;

    /**
     * Generate content using the optimal available provider with automatic failover
     */
    public ContentGenerateResponse generateContent(ContentGenerateRequest request, User user) {
        log.info("Starting content generation for user: {} with provider selection", user.getId());

        // Use load balancer to select initial provider
        AIProvider primaryProvider;
        try {
            primaryProvider = loadBalancer.selectProvider(request);
        } catch (Exception e) {
            log.warn("Load balancer failed, falling back to score-based selection: {}", e.getMessage());
            List<AIProvider> availableProviders = getAvailableProvidersSortedByScore(request);
            if (availableProviders.isEmpty()) {
                throw new BusinessException("No AI providers are currently available");
            }
            primaryProvider = availableProviders.get(0);
        }

        // Get fallback providers (excluding primary)
        final AIProvider selectedPrimary = primaryProvider;
        List<AIProvider> availableProviders = getAvailableProvidersSortedByScore(request).stream()
                .filter(provider -> !provider.equals(selectedPrimary))
                .collect(Collectors.toList());

        // Add primary provider at the beginning
        availableProviders.add(0, selectedPrimary);

        if (availableProviders.isEmpty()) {
            throw new BusinessException("No AI providers are currently available");
        }

        Exception lastException = null;

        // Try providers in order of preference
        for (int i = 0; i < availableProviders.size(); i++) {
            AIProvider provider = availableProviders.get(i);
            String providerName = provider.getName();

            // Check circuit breaker
            if (isCircuitBreakerOpen(providerName)) {
                log.warn("Circuit breaker is open for provider: {}, skipping", providerName);
                continue;
            }

            try {
                log.info("Attempting content generation with provider: {} (attempt {} of {})",
                        providerName, i + 1, availableProviders.size());

                ContentGenerateResponse response = provider.generateContent(request, user);

                // Reset circuit breaker on success
                resetCircuitBreaker(providerName);

                // Record metrics based on whether this was a fallback
                if (i > 0) {
                    metricsService.recordFallbackSuccess(providerName,
                            response.getProcessingTimeMs() != null ? response.getProcessingTimeMs() : 0L,
                            calculateQualityScore(response));
                    log.info("Content generation succeeded with fallback provider: {}", providerName);
                } else {
                    log.info("Content generation succeeded with primary provider: {}", providerName);
                }

                // Record cost for monitoring and budget tracking
                if (response.getGenerationCost() != null
                        && response.getGenerationCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    alertingService.recordProviderCost(providerName, response.getGenerationCost());
                    log.debug("Recorded cost for provider {}: ${}", providerName, response.getGenerationCost());
                }

                return response;

            } catch (Exception e) {
                lastException = e;
                log.warn("Provider {} failed for user {}: {}", providerName, user.getId(), e.getMessage());

                // Record failure and update circuit breaker
                recordProviderFailure(providerName, e);

                // If this is not the last provider, continue to next
                if (i < availableProviders.size() - 1) {
                    log.info("Attempting failover to next provider...");

                    // Apply exponential backoff before trying next provider
                    applyExponentialBackoff(i);
                } else {
                    log.error("All providers failed for user: {}", user.getId());
                }
            }
        }

        // All providers failed
        throw new BusinessException("All AI providers failed to generate content. Last error: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    /**
     * Get the optimal provider for a request (without actually using it)
     */
    public AIProvider selectOptimalProvider(ContentGenerateRequest request) {
        List<AIProvider> availableProviders = getAvailableProvidersSortedByScore(request);

        if (availableProviders.isEmpty()) {
            throw new BusinessException("No AI providers are currently available");
        }

        AIProvider selectedProvider = availableProviders.get(0);
        log.debug("Selected optimal provider: {} with score: {}",
                selectedProvider.getName(), calculateProviderScore(selectedProvider, request));

        return selectedProvider;
    }

    /**
     * Get all available providers with their current status
     */
    public List<ProviderStatus> getAllProviderStatuses() {
        return providers.stream()
                .map(this::buildProviderStatus)
                .toList();
    }

    /**
     * Get detailed metrics for all providers
     */
    public Map<String, Map<String, Object>> getAllProviderMetrics() {
        Map<String, Map<String, Object>> allMetrics = new ConcurrentHashMap<>();

        for (AIProvider provider : providers) {
            Map<String, Object> metrics = metricsService.getProviderMetrics(provider.getName());

            // Add current provider info
            metrics.put("provider_name", provider.getName());
            metrics.put("is_available", provider.isAvailable());
            metrics.put("cost_per_token", provider.getCostPerToken());
            metrics.put("current_load", provider.getCurrentLoad());
            metrics.put("health_status", provider.getHealthStatus());
            metrics.put("capabilities", provider.getCapabilities());

            // Add circuit breaker status
            CircuitBreakerState cbState = circuitBreakers.get(provider.getName());
            if (cbState != null) {
                metrics.put("circuit_breaker_open", cbState.isOpen());
                metrics.put("circuit_breaker_failures", cbState.getFailureCount());
                metrics.put("circuit_breaker_last_failure", cbState.getLastFailureTime());
            }

            allMetrics.put(provider.getName(), metrics);
        }

        return allMetrics;
    }

    private List<AIProvider> getAvailableProvidersSortedByScore(ContentGenerateRequest request) {
        return providers.stream()
                .filter(provider -> {
                    // Check basic availability
                    if (!provider.isAvailable()) {
                        log.debug("Provider {} is not available", provider.getName());
                        return false;
                    }

                    // Check circuit breaker
                    if (isCircuitBreakerOpen(provider.getName())) {
                        log.debug("Provider {} circuit breaker is open", provider.getName());
                        return false;
                    }

                    // Check if provider supports the requested content type
                    ProviderCapabilities capabilities = provider.getCapabilities();
                    if (request.getContentType() != null &&
                            !capabilities.getSupportedContentTypes().contains(request.getContentType())) {
                        log.debug("Provider {} does not support content type: {}",
                                provider.getName(), request.getContentType());
                        return false;
                    }

                    return true;
                })
                .sorted(Comparator.comparingDouble((AIProvider p) -> calculateProviderScore(p, request)).reversed())
                .toList();
    }

    private double calculateProviderScore(AIProvider provider, ContentGenerateRequest request) {
        try {
            // Cost score (lower cost = higher score)
            double costScore = calculateCostScore(provider);

            // Availability score (higher success rate = higher score)
            double availabilityScore = provider.getSuccessRate();

            // Quality score (higher quality = higher score, normalized to 0-1)
            double qualityScore = Math.min(provider.getQualityScore() / 10.0, 1.0);

            // Response time score (faster = higher score, normalized)
            double responseTimeScore = calculateResponseTimeScore(provider);

            // Calculate weighted score
            double totalScore = (costScore * COST_WEIGHT) +
                    (availabilityScore * AVAILABILITY_WEIGHT) +
                    (qualityScore * QUALITY_WEIGHT) +
                    (responseTimeScore * RESPONSE_TIME_WEIGHT);

            log.debug(
                    "Provider {} score breakdown - Cost: {:.3f}, Availability: {:.3f}, Quality: {:.3f}, ResponseTime: {:.3f}, Total: {:.3f}",
                    provider.getName(), costScore, availabilityScore, qualityScore, responseTimeScore, totalScore);

            return totalScore;

        } catch (Exception e) {
            log.error("Error calculating score for provider: {}", provider.getName(), e);
            return 0.0; // Lowest possible score
        }
    }

    private double calculateCostScore(AIProvider provider) {
        // Convert cost per token to a score (lower cost = higher score)
        // Assuming cost range from $0.0001 to $0.01 per token
        double cost = provider.getCostPerToken().doubleValue();
        double maxCost = 0.01;
        double minCost = 0.0001;

        // Normalize and invert (lower cost = higher score)
        return Math.max(0.0, (maxCost - cost) / (maxCost - minCost));
    }

    private double calculateResponseTimeScore(AIProvider provider) {
        // Convert response time to a score (faster = higher score)
        // Assuming response time range from 1s to 30s
        long responseTime = provider.getAverageResponseTime();
        double maxResponseTime = 30000.0; // 30 seconds
        double minResponseTime = 1000.0; // 1 second

        if (responseTime <= 0) {
            return 1.0; // No data, assume best case
        }

        // Normalize and invert (faster = higher score)
        return Math.max(0.0, (maxResponseTime - responseTime) / (maxResponseTime - minResponseTime));
    }

    private boolean isCircuitBreakerOpen(String providerName) {
        CircuitBreakerState state = circuitBreakers.get(providerName);
        if (state == null) {
            return false;
        }

        if (!state.isOpen()) {
            return false;
        }

        // Check if timeout has passed
        long now = System.currentTimeMillis();
        if (now - state.getLastFailureTime() > CIRCUIT_BREAKER_TIMEOUT_MS) {
            // Reset circuit breaker to half-open state
            state.setOpen(false);
            state.setFailureCount(0);
            log.info("Circuit breaker for provider {} reset to closed state", providerName);
            return false;
        }

        return true;
    }

    private void recordProviderFailure(String providerName, Exception exception) {
        CircuitBreakerState state = circuitBreakers.computeIfAbsent(providerName,
                k -> new CircuitBreakerState());

        state.incrementFailures();
        state.setLastFailureTime(System.currentTimeMillis());

        // Open circuit breaker if threshold reached
        if (state.getFailureCount() >= FAILURE_THRESHOLD) {
            state.setOpen(true);
            log.warn("Circuit breaker opened for provider: {} after {} failures",
                    providerName, state.getFailureCount());
        }

        // Record in metrics
        String errorType = exception instanceof BusinessException ? "BUSINESS_ERROR" : "SYSTEM_ERROR";
        metricsService.recordFailure(providerName, errorType, exception.getMessage());
    }

    private void resetCircuitBreaker(String providerName) {
        CircuitBreakerState state = circuitBreakers.get(providerName);
        if (state != null && (state.isOpen() || state.getFailureCount() > 0)) {
            state.setOpen(false);
            state.setFailureCount(0);
            log.info("Circuit breaker reset for provider: {}", providerName);
        }
    }

    private void applyExponentialBackoff(int attemptNumber) {
        if (attemptNumber >= MAX_BACKOFF_ATTEMPTS) {
            return; // No backoff after max attempts
        }

        long backoffMs = EXPONENTIAL_BACKOFF_BASE_MS * (1L << attemptNumber); // 2^attemptNumber
        backoffMs = Math.min(backoffMs, 10000); // Max 10 seconds

        try {
            log.debug("Applying exponential backoff: {}ms before next provider attempt", backoffMs);
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Backoff interrupted");
        }
    }

    private double calculateQualityScore(ContentGenerateResponse response) {
        // Simple quality calculation based on response characteristics
        if (response.getGeneratedContent() == null || response.getGeneratedContent().trim().isEmpty()) {
            return 0.0;
        }

        // Base score
        double score = 5.0;

        // Length factor
        int length = response.getGeneratedContent().length();
        if (length >= 100 && length <= 2000) {
            score += 1.0;
        }

        // Word count factor
        if (response.getWordCount() != null && response.getWordCount() > 20) {
            score += 1.0;
        }

        return Math.min(score, 10.0);
    }

    private ProviderStatus buildProviderStatus(AIProvider provider) {
        ProviderHealthStatus health = provider.getHealthStatus();
        CircuitBreakerState cbState = circuitBreakers.get(provider.getName());

        return ProviderStatus.builder()
                .name(provider.getName())
                .isAvailable(provider.isAvailable())
                .healthStatus(health)
                .capabilities(provider.getCapabilities())
                .costPerToken(provider.getCostPerToken())
                .averageResponseTime(provider.getAverageResponseTime())
                .successRate(provider.getSuccessRate())
                .qualityScore(provider.getQualityScore())
                .currentLoad(provider.getCurrentLoad())
                .circuitBreakerOpen(cbState != null && cbState.isOpen())
                .consecutiveFailures(cbState != null ? cbState.getFailureCount() : 0)
                .build();
    }

    /**
     * Circuit breaker state for a provider
     */
    private static class CircuitBreakerState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile boolean open = false;
        private volatile long lastFailureTime = 0;

        public int getFailureCount() {
            return failureCount.get();
        }

        public void incrementFailures() {
            failureCount.incrementAndGet();
        }

        public void setFailureCount(int count) {
            failureCount.set(count);
        }

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public long getLastFailureTime() {
            return lastFailureTime;
        }

        public void setLastFailureTime(long lastFailureTime) {
            this.lastFailureTime = lastFailureTime;
        }
    }
}