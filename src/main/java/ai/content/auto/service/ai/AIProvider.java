package ai.content.auto.service.ai;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.dtos.GenerateMetadataRequest;
import ai.content.auto.dtos.GenerateMetadataResponse;
import ai.content.auto.entity.User;

import java.math.BigDecimal;

/**
 * Interface for AI content generation providers
 * Provides a unified interface for different AI services (OpenAI, Claude,
 * Gemini, etc.)
 */
public interface AIProvider {

    /**
     * Get the provider name (e.g., "OpenAI", "Claude", "Gemini")
     */
    String getName();

    /**
     * Check if the provider is currently available and healthy
     */
    boolean isAvailable();

    /**
     * Get the cost per token for this provider
     */
    BigDecimal getCostPerToken();

    /**
     * Get the provider's health status with detailed information
     */
    ProviderHealthStatus getHealthStatus();

    /**
     * Get provider capabilities (supported content types, languages, etc.)
     */
    ProviderCapabilities getCapabilities();

    /**
     * Generate content using this provider
     * 
     * @param request The content generation request
     * @param user    The user making the request
     * @return The generated content response
     */
    ContentGenerateResponse generateContent(ContentGenerateRequest request, User user);

    /**
     * Get the average response time for this provider (in milliseconds)
     */
    long getAverageResponseTime();

    /**
     * Get the success rate for this provider (0.0 to 1.0)
     */
    double getSuccessRate();

    /**
     * Get the quality score for this provider (0.0 to 10.0)
     */
    double getQualityScore();

    /**
     * Get the current load/usage level (0.0 to 1.0)
     */
    double getCurrentLoad();

    GenerateMetadataResponse generateMetadata(GenerateMetadataRequest request, User user);
}