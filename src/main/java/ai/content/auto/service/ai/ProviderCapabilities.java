package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Represents the capabilities of an AI provider
 */
@Data
@Builder
public class ProviderCapabilities {

    /**
     * Supported content types
     */
    private Set<String> supportedContentTypes;

    /**
     * Supported languages
     */
    private Set<String> supportedLanguages;

    /**
     * Supported tones
     */
    private Set<String> supportedTones;

    /**
     * Maximum tokens per request
     */
    private int maxTokensPerRequest;

    /**
     * Maximum requests per minute
     */
    private int maxRequestsPerMinute;

    /**
     * Whether the provider supports streaming responses
     */
    private boolean supportsStreaming;

    /**
     * Whether the provider supports function calling
     */
    private boolean supportsFunctionCalling;

    /**
     * Whether the provider supports image generation
     */
    private boolean supportsImageGeneration;

    /**
     * Whether the provider supports image analysis
     */
    private boolean supportsImageAnalysis;

    /**
     * Minimum quality score this provider typically achieves
     */
    private double minQualityScore;

    /**
     * Maximum quality score this provider typically achieves
     */
    private double maxQualityScore;
}