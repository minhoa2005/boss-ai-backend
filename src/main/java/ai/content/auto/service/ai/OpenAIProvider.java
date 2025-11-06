package ai.content.auto.service.ai;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI provider implementation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIProvider implements AIProvider {

    private final OpenAiService openAiService;
    private final AIProviderMetricsService metricsService;

    private static final String PROVIDER_NAME = "OpenAI";
    private static final BigDecimal COST_PER_TOKEN = new BigDecimal("0.0004"); // GPT-4o-mini rate
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Rate limit

    private final AtomicLong lastHealthCheck = new AtomicLong(0);
    private volatile ProviderHealthStatus cachedHealthStatus;

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        ProviderHealthStatus health = getHealthStatus();
        return health.isAvailable() &&
                health.getHealthLevel() != ProviderHealthStatus.HealthLevel.DOWN;
    }

    @Override
    public BigDecimal getCostPerToken() {
        return COST_PER_TOKEN;
    }

    @Override
    public ProviderHealthStatus getHealthStatus() {
        long now = System.currentTimeMillis();
        long lastCheck = lastHealthCheck.get();

        // Cache health status for 30 seconds
        if (cachedHealthStatus != null && (now - lastCheck) < 30000) {
            return cachedHealthStatus;
        }

        try {
            // Get metrics from Redis
            Map<String, Object> metrics = metricsService.getProviderMetrics(PROVIDER_NAME);

            // Calculate health based on recent performance
            ProviderHealthStatus.HealthLevel healthLevel = calculateHealthLevel(metrics);
            boolean isAvailable = healthLevel != ProviderHealthStatus.HealthLevel.DOWN;

            // Get recent failure info
            int consecutiveFailures = Integer.parseInt(
                    metrics.getOrDefault("consecutive_failures", "0").toString());
            double errorRate = Double.parseDouble(
                    metrics.getOrDefault("error_rate", "0.0").toString());
            long avgResponseTime = Long.parseLong(
                    metrics.getOrDefault("avg_response_time", "0").toString());

            // Build health status
            ProviderHealthStatus healthStatus = ProviderHealthStatus.builder()
                    .healthLevel(healthLevel)
                    .isAvailable(isAvailable)
                    .consecutiveFailures(consecutiveFailures)
                    .currentResponseTime(avgResponseTime)
                    .errorRate(errorRate)
                    .message(buildHealthMessage(healthLevel, consecutiveFailures, errorRate))
                    .lastHealthCheck(Instant.now())
                    .build();

            // Get last success/failure timestamps
            if (metrics.containsKey("last_success")) {
                long lastSuccessMs = Long.parseLong(metrics.get("last_success").toString());
                healthStatus.setLastSuccessfulRequest(Instant.ofEpochMilli(lastSuccessMs));
            }

            if (metrics.containsKey("last_failure")) {
                long lastFailureMs = Long.parseLong(metrics.get("last_failure").toString());
                healthStatus.setLastFailedRequest(Instant.ofEpochMilli(lastFailureMs));
            }

            // Cache the result
            cachedHealthStatus = healthStatus;
            lastHealthCheck.set(now);

            // Update health status in Redis
            metricsService.updateHealthStatus(PROVIDER_NAME, healthStatus);

            return healthStatus;

        } catch (Exception e) {
            log.error("Error checking OpenAI provider health", e);

            ProviderHealthStatus errorStatus = ProviderHealthStatus.builder()
                    .healthLevel(ProviderHealthStatus.HealthLevel.DOWN)
                    .isAvailable(false)
                    .message("Health check failed: " + e.getMessage())
                    .lastHealthCheck(Instant.now())
                    .build();

            cachedHealthStatus = errorStatus;
            lastHealthCheck.set(now);

            return errorStatus;
        }
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .supportedContentTypes(Set.of(
                        ContentConstants.CONTENT_TYPE_BLOG,
                        ContentConstants.CONTENT_TYPE_ARTICLE,
                        ContentConstants.CONTENT_TYPE_SOCIAL,
                        ContentConstants.CONTENT_TYPE_FACEBOOK,
                        ContentConstants.CONTENT_TYPE_INSTAGRAM,
                        ContentConstants.CONTENT_TYPE_EMAIL,
                        ContentConstants.CONTENT_TYPE_NEWSLETTER,
                        ContentConstants.CONTENT_TYPE_PRODUCT,
                        ContentConstants.CONTENT_TYPE_AD))
                .supportedLanguages(Set.of(
                        ContentConstants.LANGUAGE_VIETNAMESE,
                        ContentConstants.LANGUAGE_ENGLISH))
                .supportedTones(Set.of(
                        ContentConstants.TONE_PROFESSIONAL,
                        ContentConstants.TONE_FRIENDLY,
                        ContentConstants.TONE_ENTHUSIASTIC,
                        ContentConstants.TONE_HUMOROUS,
                        ContentConstants.TONE_AUTHORITATIVE,
                        ContentConstants.TONE_CASUAL))
                .maxTokensPerRequest(4000)
                .maxRequestsPerMinute(MAX_REQUESTS_PER_MINUTE)
                .supportsStreaming(false)
                .supportsFunctionCalling(true)
                .supportsImageGeneration(false)
                .supportsImageAnalysis(false)
                .minQualityScore(3.0)
                .maxQualityScore(9.0)
                .build();
    }

    @Override
    public ContentGenerateResponse generateContent(ContentGenerateRequest request, User user) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Generating content with OpenAI for user: {} - content type: {}",
                    user.getId(), request.getContentType());

            // Call the existing OpenAI service
            Map<String, Object> result = openAiService.generateContent(request, user);

            // Convert to ContentGenerateResponse
            ContentGenerateResponse response = convertToResponse(result);

            // Calculate metrics
            long responseTime = System.currentTimeMillis() - startTime;
            double qualityScore = (Double) result.getOrDefault("qualityScore", 5.0);

            // Record success metrics
            metricsService.recordSuccess(PROVIDER_NAME, responseTime, qualityScore);

            log.info("OpenAI content generation completed for user: {} - response time: {}ms, quality: {}",
                    user.getId(), responseTime, qualityScore);

            return response;

        } catch (BusinessException e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // Record failure metrics
            metricsService.recordFailure(PROVIDER_NAME, "BUSINESS_ERROR", e.getMessage());

            log.error("OpenAI content generation failed for user: {} - error: {}",
                    user.getId(), e.getMessage());

            // Re-throw the exception
            throw e;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // Record failure metrics
            metricsService.recordFailure(PROVIDER_NAME, "SYSTEM_ERROR", e.getMessage());

            log.error("Unexpected error in OpenAI content generation for user: {}", user.getId(), e);

            throw new BusinessException("OpenAI content generation failed: " + e.getMessage());
        }
    }

    @Override
    public long getAverageResponseTime() {
        return metricsService.calculateAverageResponseTime(PROVIDER_NAME);
    }

    @Override
    public double getSuccessRate() {
        return metricsService.calculateSuccessRate(PROVIDER_NAME);
    }

    @Override
    public double getQualityScore() {
        return metricsService.calculateAverageQualityScore(PROVIDER_NAME);
    }

    @Override
    public double getCurrentLoad() {
        // Simple load calculation based on recent request rate
        // In a real implementation, this could be more sophisticated
        Map<String, Object> metrics = metricsService.getProviderMetrics(PROVIDER_NAME);

        // Get requests in the last minute (simplified)
        long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());

        // Calculate load as percentage of max capacity
        double currentRpm = Math.min(totalRequests, MAX_REQUESTS_PER_MINUTE);
        return currentRpm / MAX_REQUESTS_PER_MINUTE;
    }

    private ProviderHealthStatus.HealthLevel calculateHealthLevel(Map<String, Object> metrics) {
        double errorRate = Double.parseDouble(metrics.getOrDefault("error_rate", "0.0").toString());
        int consecutiveFailures = Integer.parseInt(metrics.getOrDefault("consecutive_failures", "0").toString());
        long avgResponseTime = Long.parseLong(metrics.getOrDefault("avg_response_time", "0").toString());

        // Down if too many consecutive failures
        if (consecutiveFailures >= 5) {
            return ProviderHealthStatus.HealthLevel.DOWN;
        }

        // Unhealthy if high error rate
        if (errorRate > 0.5) {
            return ProviderHealthStatus.HealthLevel.UNHEALTHY;
        }

        // Degraded if moderate error rate or slow response
        if (errorRate > 0.2 || avgResponseTime > 10000) {
            return ProviderHealthStatus.HealthLevel.DEGRADED;
        }

        // Healthy otherwise
        return ProviderHealthStatus.HealthLevel.HEALTHY;
    }

    private String buildHealthMessage(ProviderHealthStatus.HealthLevel healthLevel,
            int consecutiveFailures, double errorRate) {
        switch (healthLevel) {
            case HEALTHY:
                return "Provider is operating normally";
            case DEGRADED:
                return String.format("Provider is degraded - error rate: %.1f%%", errorRate * 100);
            case UNHEALTHY:
                return String.format("Provider is unhealthy - error rate: %.1f%%, consecutive failures: %d",
                        errorRate * 100, consecutiveFailures);
            case DOWN:
                return String.format("Provider is down - %d consecutive failures", consecutiveFailures);
            default:
                return "Unknown health status";
        }
    }

    private ContentGenerateResponse convertToResponse(Map<String, Object> result) {
        ContentGenerateResponse response = new ContentGenerateResponse();

        response.setGeneratedContent((String) result.get("generatedContent"));
        response.setStatus((String) result.get("status"));
        response.setErrorMessage((String) result.get("errorMessage"));

        // Handle numeric fields safely
        if (result.get("wordCount") != null) {
            response.setWordCount((Integer) result.get("wordCount"));
        }

        if (result.get("characterCount") != null) {
            response.setCharacterCount((Integer) result.get("characterCount"));
        }

        if (result.get("tokensUsed") != null) {
            response.setTokensUsed((Integer) result.get("tokensUsed"));
        }

        if (result.get("processingTimeMs") != null) {
            response.setProcessingTimeMs((Long) result.get("processingTimeMs"));
        }

        if (result.get("estimatedCost") != null) {
            Object cost = result.get("estimatedCost");
            if (cost instanceof Double) {
                response.setGenerationCost(BigDecimal.valueOf((Double) cost));
            } else if (cost instanceof BigDecimal) {
                response.setGenerationCost((BigDecimal) cost);
            }
        }

        // Set OpenAI response ID
        if (result.get("openaiResponseId") != null) {
            response.setOpenaiResponseId((String) result.get("openaiResponseId"));
        }

        return response;
    }
}