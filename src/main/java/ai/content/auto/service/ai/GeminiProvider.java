package ai.content.auto.service.ai;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.entity.N8nConfig;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.N8nConfigRepository;
import ai.content.auto.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Google Gemini AI provider implementation
 * Provides content generation using Google's Gemini API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiProvider implements AIProvider {

    private final N8nConfigRepository n8nConfigRepository;
    private final RestTemplate restTemplate;
    private final AIProviderMetricsService metricsService;

    private static final String PROVIDER_NAME = "Gemini";
    private static final String GEMINI_AGENT_NAME = "gemini";
    private static final BigDecimal COST_PER_TOKEN = new BigDecimal("0.0002"); // Gemini Pro rate
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Rate limit
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_BASE_MS = 1000;

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
            log.error("Error checking Gemini provider health", e);

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
                .maxTokensPerRequest(8192) // Gemini Pro supports up to 8K tokens
                .maxRequestsPerMinute(MAX_REQUESTS_PER_MINUTE)
                .supportsStreaming(false)
                .supportsFunctionCalling(true)
                .supportsImageGeneration(false)
                .supportsImageAnalysis(true) // Gemini supports image analysis
                .minQualityScore(3.5)
                .maxQualityScore(9.5)
                .build();
    }

    @Override
    public ContentGenerateResponse generateContent(ContentGenerateRequest request, User user) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate input first
            validateGenerateRequest(request, user);

            log.info("Generating content with Gemini for user: {} - content type: {}",
                    user.getId(), request.getContentType());

            // Get Gemini configuration
            N8nConfig geminiConfig = getGeminiConfig();

            // Generate content with retry logic
            Map<String, Object> result = generateContentWithRetry(request, user, geminiConfig);

            // Convert to ContentGenerateResponse
            ContentGenerateResponse response = convertToResponse(result);

            // Calculate metrics
            long responseTime = System.currentTimeMillis() - startTime;
            double qualityScore = (Double) result.getOrDefault("qualityScore", 5.0);

            // Record success metrics
            metricsService.recordSuccess(PROVIDER_NAME, responseTime, qualityScore);

            log.info("Gemini content generation completed for user: {} - response time: {}ms, quality: {}",
                    user.getId(), responseTime, qualityScore);

            return response;

        } catch (BusinessException e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // Record failure metrics
            metricsService.recordFailure(PROVIDER_NAME, "BUSINESS_ERROR", e.getMessage());

            log.error("Gemini content generation failed for user: {} - error: {}",
                    user != null ? user.getId() : "null", e.getMessage());

            // Re-throw the exception
            throw e;

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // Record failure metrics
            metricsService.recordFailure(PROVIDER_NAME, "SYSTEM_ERROR", e.getMessage());

            log.error("Unexpected error in Gemini content generation for user: {}",
                    user != null ? user.getId() : "null", e);

            throw new BusinessException("Gemini content generation failed: " + e.getMessage());
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
        Map<String, Object> metrics = metricsService.getProviderMetrics(PROVIDER_NAME);

        // Get requests in the last minute (simplified)
        long totalRequests = Long.parseLong(metrics.getOrDefault("total_requests", "0").toString());

        // Calculate load as percentage of max capacity
        double currentRpm = Math.min(totalRequests, MAX_REQUESTS_PER_MINUTE);
        return currentRpm / MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Get Gemini configuration from database
     */
    private N8nConfig getGeminiConfig() {
        return n8nConfigRepository.findN8nConfigByAgentName(GEMINI_AGENT_NAME)
                .orElseThrow(() -> new BusinessException("Gemini configuration not found"));
    }

    /**
     * Generate content with retry logic
     */
    private Map<String, Object> generateContentWithRetry(
            ContentGenerateRequest request, User user, N8nConfig geminiConfig) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("Gemini API call attempt {} of {}", attempt, MAX_RETRIES);
                return generateContentInternal(request, user, geminiConfig);

            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini API call attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(RETRY_DELAY_BASE_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException("Request interrupted during retry");
                    }
                }
            }
        }

        throw new BusinessException("Gemini API failed after " + MAX_RETRIES + " attempts: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    /**
     * Internal content generation method
     */
    private Map<String, Object> generateContentInternal(
            ContentGenerateRequest request, User user, N8nConfig geminiConfig) {

        // Check API key
        if (StringUtil.isBlank(geminiConfig.getXApiKey())) {
            throw new BusinessException("Gemini API key is not configured");
        }

        long startTime = System.currentTimeMillis();

        // Calculate optimal max tokens based on content type
        Long maxTokens = calculateOptimalMaxTokens(request.getContentType());

        // Build Gemini request
        Map<String, Object> geminiRequest = buildGeminiRequest(request, geminiConfig, maxTokens);

        // Validate request
        validateGeminiRequest(geminiRequest);

        // Call Gemini API
        Map<String, Object> responseBody = callGeminiApi(geminiRequest, geminiConfig);

        // Process response
        Map<String, Object> result = processGeminiResponse(responseBody, startTime);

        // Validate generated content
        validateGeneratedContent(result);

        return result;
    }

    /**
     * Calculate optimal max tokens based on content type
     */
    private Long calculateOptimalMaxTokens(String contentType) {
        if (StringUtil.isBlank(contentType)) {
            return ContentConstants.MAX_TOKENS_DEFAULT;
        }

        String normalizedType = StringUtil.toLowerCase(contentType);

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_BLOG) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_ARTICLE)) {
            return ContentConstants.MAX_TOKENS_BLOG;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_SOCIAL) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_FACEBOOK)) {
            return ContentConstants.MAX_TOKENS_FACEBOOK;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_INSTAGRAM)) {
            return ContentConstants.MAX_TOKENS_INSTAGRAM;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_EMAIL)) {
            return ContentConstants.MAX_TOKENS_EMAIL;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_NEWSLETTER)) {
            return ContentConstants.MAX_TOKENS_NEWSLETTER;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_PRODUCT) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_DESCRIPTION)) {
            return ContentConstants.MAX_TOKENS_PRODUCT;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_AD) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_ADVERTISEMENT)) {
            return ContentConstants.MAX_TOKENS_AD;
        }

        return ContentConstants.MAX_TOKENS_DEFAULT;
    }

    /**
     * Build Gemini API request
     */
    private Map<String, Object> buildGeminiRequest(
            ContentGenerateRequest request, N8nConfig geminiConfig, Long maxTokens) {

        Map<String, Object> geminiRequest = new HashMap<>();

        // Build the prompt for Gemini
        String prompt = buildGeminiPrompt(request);

        // Gemini API structure
        Map<String, Object> contents = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        contents.put("parts", List.of(parts));

        geminiRequest.put("contents", List.of(contents));

        // Generation configuration
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", geminiConfig.getTemperature());
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);

        geminiRequest.put("generationConfig", generationConfig);

        // Safety settings (optional)
        List<Map<String, Object>> safetySettings = new ArrayList<>();
        Map<String, Object> safetySetting = new HashMap<>();
        safetySetting.put("category", "HARM_CATEGORY_HARASSMENT");
        safetySetting.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
        safetySettings.add(safetySetting);

        geminiRequest.put("safetySettings", safetySettings);

        return geminiRequest;
    }

    /**
     * Build comprehensive prompt for Gemini
     */
    private String buildGeminiPrompt(ContentGenerateRequest request) {
        StringBuilder prompt = new StringBuilder();

        // System instructions
        prompt.append(
                "Bạn là một chuyên gia viết nội dung AI với khả năng tạo ra nội dung chất lượng cao cho nhiều mục đích khác nhau. ");

        // Content type specific instructions
        String contentType = StringUtil.defaultIfBlank(
                StringUtil.toLowerCase(request.getContentType()),
                ContentConstants.CONTENT_TYPE_GENERAL);

        if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_BLOG) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_ARTICLE)) {
            prompt.append("Bạn chuyên viết blog và bài viết với cấu trúc rõ ràng, hấp dẫn và tối ưu SEO. ");
        } else if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_SOCIAL) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_FACEBOOK) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_INSTAGRAM)) {
            prompt.append("Bạn chuyên tạo nội dung mạng xã hội viral với khả năng thu hút tương tác cao. ");
        }

        // Language and tone
        String language = StringUtil.defaultIfBlank(request.getLanguage(), ContentConstants.LANGUAGE_VIETNAMESE);
        if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_VIETNAMESE)) {
            prompt.append("Viết bằng tiếng Việt tự nhiên và dễ hiểu. ");
        } else if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_ENGLISH)) {
            prompt.append("Write in clear, natural English. ");
        }

        // Tone instructions
        if (StringUtil.isNotBlank(request.getTone())) {
            String tone = StringUtil.toLowerCase(request.getTone());
            if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_PROFESSIONAL)) {
                prompt.append("Sử dụng giọng điệu chuyên nghiệp và trang trọng. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FRIENDLY)) {
                prompt.append("Sử dụng giọng điệu thân thiện và gần gũi. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_ENTHUSIASTIC)) {
                prompt.append("Sử dụng giọng điệu nhiệt huyết và hứng khởi. ");
            }
        }

        // Main content request
        prompt.append("\n\nYêu cầu tạo nội dung:\n");
        prompt.append(request.getContent());

        // Additional specifications
        if (request.getTargetAudience() != null) {
            prompt.append("\n\nĐối tượng mục tiêu: ").append(request.getTargetAudience());
        }

        if (request.getIndustry() != null) {
            prompt.append("\nLĩnh vực: ").append(request.getIndustry());
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            prompt.append("\nTiêu đề: ").append(request.getTitle());
        }

        // Quality requirements
        prompt.append("\n\nYêu cầu chất lượng:");
        prompt.append("\n- Nội dung phải chính xác, hữu ích và có giá trị");
        prompt.append("\n- Cấu trúc rõ ràng với đầu, thân, kết");
        prompt.append("\n- Sử dụng từ ngữ phù hợp với đối tượng mục tiêu");
        prompt.append("\n- Tránh lặp từ và câu cấu trúc đơn điệu");
        prompt.append("\n- Đảm bảo tính nhất quán về giọng điệu và phong cách");

        return prompt.toString();
    }

    /**
     * Validate Gemini request before sending
     */
    private void validateGeminiRequest(Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) request.get("contents");

        if (contents == null || contents.isEmpty()) {
            throw new BusinessException("No contents in Gemini request");
        }

        // Estimate token count for the prompt
        Map<String, Object> firstContent = contents.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) firstContent.get("parts");

        if (parts == null || parts.isEmpty()) {
            throw new BusinessException("No parts in Gemini request content");
        }

        String text = (String) parts.get(0).get("text");
        int estimatedTokens = text.length() / 4; // Rough estimation

        @SuppressWarnings("unchecked")
        Map<String, Object> generationConfig = (Map<String, Object>) request.get("generationConfig");
        Long maxTokens = (Long) generationConfig.get("maxOutputTokens");

        if (estimatedTokens > 6000 - maxTokens) { // Leave room for response
            log.warn("Gemini prompt may be too long: {} estimated tokens", estimatedTokens);
        }

        log.debug("Gemini request validated - estimated prompt tokens: {}, max response tokens: {}",
                estimatedTokens, maxTokens);
    }

    /**
     * Call Gemini API
     */
    private Map<String, Object> callGeminiApi(
            Map<String, Object> geminiRequest, N8nConfig geminiConfig) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiConfig.getXApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(geminiRequest, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                geminiConfig.getAgentUrl(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new BusinessException("Empty response from Gemini API");
        }

        return responseBody;
    }

    /**
     * Process Gemini API response
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processGeminiResponse(
            Map<String, Object> responseBody, long startTime) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Check for API errors first
            if (responseBody.containsKey("error") && responseBody.get("error") != null) {
                Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                String errorMessage = (String) error.get("message");
                log.error("Gemini API error: {}", errorMessage);

                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "Gemini API error: " + errorMessage);
                return result;
            }

            // Process Gemini response format
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "No candidates returned from Gemini");
                return result;
            }

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");

            if (content == null) {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "No content in Gemini candidate");
                return result;
            }

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "No parts in Gemini content");
                return result;
            }

            String generatedText = (String) parts.get(0).get("text");
            if (StringUtil.isBlank(generatedText)) {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "Empty content generated by Gemini");
                return result;
            }

            // Clean and process content
            String cleanedContent = cleanGeneratedContent(generatedText);

            result.put("generatedContent", cleanedContent);
            result.put("wordCount", countWords(cleanedContent));
            result.put("characterCount", cleanedContent.length());
            result.put("sentenceCount", countSentences(cleanedContent));
            result.put("paragraphCount", countParagraphs(cleanedContent));

            // Calculate quality metrics
            result.put("qualityScore", calculateQualityScore(cleanedContent));
            result.put("readabilityScore", calculateReadabilityScore(cleanedContent));

            // Extract usage information if available
            if (responseBody.containsKey("usageMetadata")) {
                Map<String, Object> usage = (Map<String, Object>) responseBody.get("usageMetadata");
                result.put("tokensUsed", usage.get("totalTokenCount"));
                result.put("promptTokens", usage.get("promptTokenCount"));
                result.put("completionTokens", usage.get("candidatesTokenCount"));

                // Calculate cost estimation
                Integer totalTokens = (Integer) usage.get("totalTokenCount");
                if (totalTokens != null) {
                    result.put("estimatedCost", calculateEstimatedCost(totalTokens));
                }
            }

            result.put("processingTimeMs", System.currentTimeMillis() - startTime);
            result.put("status", ContentConstants.STATUS_COMPLETED);

            log.debug("Successfully processed Gemini response - content length: {}, tokens used: {}",
                    result.get("characterCount"), result.get("tokensUsed"));

        } catch (Exception e) {
            log.error("Error processing Gemini response", e);
            result.put("status", ContentConstants.STATUS_FAILED);
            result.put("errorMessage", "Failed to process Gemini response: " + e.getMessage());
        }

        return result;
    }

    // Helper methods (similar to OpenAI implementation)

    private void validateGenerateRequest(ContentGenerateRequest request, User user) {
        if (request == null) {
            throw new BusinessException("Generate request is required");
        }
        if (user == null) {
            throw new BusinessException("User is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("Content is required");
        }
    }

    private void validateGeneratedContent(Map<String, Object> result) {
        String status = (String) result.get("status");
        if (!StringUtil.equalsIgnoreCase(status, ContentConstants.STATUS_COMPLETED)) {
            return; // Already failed, no need to validate
        }

        String content = (String) result.get("generatedContent");
        if (StringUtil.isBlank(content)) {
            result.put("status", ContentConstants.STATUS_FAILED);
            result.put("errorMessage", "Generated content is empty");
            return;
        }

        // Check minimum content length
        if (content.length() < ContentConstants.MIN_CONTENT_LENGTH) {
            result.put("status", ContentConstants.STATUS_FAILED);
            result.put("errorMessage", "Generated content is too short");
            return;
        }

        // Check for common AI artifacts
        if (StringUtil.containsIgnoreCase(content, ContentConstants.AI_ARTIFACT_AS_AN_AI) ||
                StringUtil.containsIgnoreCase(content, ContentConstants.AI_ARTIFACT_I_CANNOT) ||
                StringUtil.containsIgnoreCase(content, ContentConstants.AI_ARTIFACT_IM_SORRY) ||
                StringUtil.containsIgnoreCase(content, ContentConstants.AI_ARTIFACT_LANGUAGE_MODEL)) {

            log.warn("Generated content contains AI artifacts, may need prompt improvement");
        }

        // Check quality score
        Double qualityScore = (Double) result.get("qualityScore");
        if (qualityScore != null && qualityScore < ContentConstants.QUALITY_SCORE_THRESHOLD) {
            log.warn("Generated content has low quality score: {}", qualityScore);
        }

        log.debug("Content validation passed - length: {}, quality score: {}",
                content.length(), qualityScore);
    }

    private String cleanGeneratedContent(String content) {
        if (content == null)
            return "";

        // Remove excessive whitespace
        content = content.trim().replaceAll("\\s+", " ");

        // Fix common formatting issues
        content = content.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n"); // Max 2 consecutive newlines
        content = content.replaceAll(
                "([.!?])([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ])",
                "$1 $2"); // Add space after sentence endings

        // Ensure proper paragraph breaks
        content = content.replaceAll("\\n", "\n\n");

        return content.trim();
    }

    private double calculateQualityScore(String content) {
        if (content == null || content.trim().isEmpty())
            return 0.0;

        double score = 5.0; // Base score

        // Length factor (optimal range: 100-2000 characters)
        int length = content.length();
        if (length >= ContentConstants.OPTIMAL_MIN_LENGTH && length <= ContentConstants.OPTIMAL_MAX_LENGTH) {
            score += 1.0;
        } else if (length < ContentConstants.MIN_SENTENCE_LENGTH) {
            score -= 2.0;
        }

        // Sentence variety
        String[] sentences = content.split("[.!?]+");
        if (sentences.length > 1) {
            score += 0.5;
        }

        // Paragraph structure
        String[] paragraphs = content.split("\\n\\s*\\n");
        if (paragraphs.length > 1) {
            score += 0.5;
        }

        // Avoid repetitive content
        String[] words = content.toLowerCase().split("\\s+");
        long uniqueWords = Arrays.stream(words).distinct().count();
        double uniqueRatio = (double) uniqueWords / words.length;
        if (uniqueRatio > 0.7) {
            score += 1.0;
        } else if (uniqueRatio < 0.5) {
            score -= 1.0;
        }

        return Math.max(0.0, Math.min(10.0, score));
    }

    private double calculateReadabilityScore(String content) {
        if (content == null || content.trim().isEmpty())
            return 0.0;

        int wordCount = countWords(content);
        int sentenceCount = countSentences(content);

        if (sentenceCount == 0)
            return 0.0;

        // Average words per sentence (optimal: 15-20 words)
        double avgWordsPerSentence = (double) wordCount / sentenceCount;

        double score = 5.0;
        if (avgWordsPerSentence >= ContentConstants.OPTIMAL_WORDS_PER_SENTENCE_MIN &&
                avgWordsPerSentence <= ContentConstants.OPTIMAL_WORDS_PER_SENTENCE_MAX) {
            score += 2.0;
        } else if (avgWordsPerSentence > ContentConstants.COMPLEX_SENTENCE_THRESHOLD) {
            score -= 1.0; // Too complex
        } else if (avgWordsPerSentence < ContentConstants.SIMPLE_SENTENCE_THRESHOLD) {
            score -= 1.0; // Too simple
        }

        return Math.max(0.0, Math.min(10.0, score));
    }

    private double calculateEstimatedCost(int totalTokens) {
        // Gemini Pro cost estimation
        return (totalTokens / 1000.0) * COST_PER_TOKEN.doubleValue();
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private int countSentences(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("[.!?]+").length;
    }

    private int countParagraphs(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("\\n\\s*\\n").length;
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
        if (errorRate > 0.2 || avgResponseTime > 12000) { // Gemini might be slightly slower
            return ProviderHealthStatus.HealthLevel.DEGRADED;
        }

        // Healthy otherwise
        return ProviderHealthStatus.HealthLevel.HEALTHY;
    }

    private String buildHealthMessage(ProviderHealthStatus.HealthLevel healthLevel,
            int consecutiveFailures, double errorRate) {
        switch (healthLevel) {
            case HEALTHY:
                return "Gemini provider is operating normally";
            case DEGRADED:
                return String.format("Gemini provider is degraded - error rate: %.1f%%", errorRate * 100);
            case UNHEALTHY:
                return String.format("Gemini provider is unhealthy - error rate: %.1f%%, consecutive failures: %d",
                        errorRate * 100, consecutiveFailures);
            case DOWN:
                return String.format("Gemini provider is down - %d consecutive failures", consecutiveFailures);
            default:
                return "Unknown Gemini health status";
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

        return response;
    }
}