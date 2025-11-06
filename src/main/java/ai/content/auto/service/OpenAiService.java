package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.entity.N8nConfig;
import ai.content.auto.entity.OpenaiResponseLog;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.InternalServerException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.repository.N8nConfigRepository;
import ai.content.auto.repository.OpenaiResponseLogRepository;
import ai.content.auto.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private final OpenaiResponseLogRepository openaiResponseLogRepository;
    private final N8nConfigRepository n8nConfigRepository;
    private final RestTemplate restTemplate;

    // @Value("${spring.ai.openai.api-key:}")
    // private String openaiApiKey;

    // private static final String OPENAI_API_URL =
    // "https://api.openai.com/v1/chat/completions";
    // private static final String DEFAULT_MODEL = "gpt-4o-mini";

    public Map<String, Object> generateContent(ContentGenerateRequest request, User user) {
        N8nConfig n8nConfig = n8nConfigRepository
                .findN8nConfigByAgentName(ContentConstants.OPENAI_AGENT_NAME)
                .orElseThrow(() -> new NotFoundException("Cannot find openai config"));
        try {
            // 1. Validate input
            validateGenerateRequest(request, user);

            // 2. Check API key
            if (n8nConfig.getXApiKey() == null || n8nConfig.getXApiKey().trim().isEmpty()) {
                throw new BusinessException("OpenAI API key is not configured");
            }

            long startTime = System.currentTimeMillis();
            log.info("Generating content via OpenAI for user: {} with content type: {}",
                    user.getId(), request.getContentType());

            // 3. Calculate optimal max tokens based on content type
            Long maxTokens = calculateOptimalMaxTokens(request.getContentType());

            // 4. Build and validate OpenAI request
            Map<String, Object> openaiRequest = buildOpenAiRequest(request, n8nConfig, maxTokens);
            validateOpenAiRequest(openaiRequest);

            // 5. Call OpenAI API with retry logic
            Map<String, Object> responseBody = callOpenAiApiWithRetry(openaiRequest, n8nConfig,
                    ContentConstants.DEFAULT_MAX_RETRIES);

            // 6. Save log in transaction
            saveResponseLogInTransaction(user, openaiRequest, responseBody, n8nConfig.getModel());

            // 7. Process and validate response
            Map<String, Object> result = processOpenAiResponse(responseBody, startTime);
            validateGeneratedContent(result);

            log.info("Content generated successfully for user: {} - {} words, {} tokens",
                    user.getId(), result.get("wordCount"), result.get("tokensUsed"));
            return result;

        } catch (BusinessException e) {
            log.error("Business error generating content for user: {}", user.getId(), e);
            saveErrorLogInTransaction(user, e.getMessage(), n8nConfig.getModel());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating content for user: {}", user.getId(), e);
            saveErrorLogInTransaction(user, e.getMessage(), n8nConfig.getModel());
            throw new InternalServerException("Failed to generate content: " + e.getMessage());
        }
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
     * Validate OpenAI request before sending
     */
    private void validateOpenAiRequest(Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("input");

        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("No messages in OpenAI request");
        }

        // Calculate approximate token count (rough estimation: 1 token ≈ 4 characters)
        int totalChars = messages.stream()
                .mapToInt(msg -> msg.get("content").length())
                .sum();
        int estimatedTokens = totalChars / 4;

        // Check if prompt is too long (leave room for response)
        Long maxTokens = (Long) request.get("max_output_tokens");
        if (estimatedTokens > ContentConstants.MAX_PROMPT_TOKENS - maxTokens) {
            log.warn("Prompt may be too long: {} estimated tokens", estimatedTokens);
        }

        log.debug("OpenAI request validated - estimated prompt tokens: {}, max response tokens: {}",
                estimatedTokens, maxTokens);
    }

    /**
     * Call OpenAI API with retry logic
     */
    private Map<String, Object> callOpenAiApiWithRetry(Map<String, Object> openaiRequest,
            N8nConfig n8nConfig, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("OpenAI API call attempt {} of {}", attempt, maxRetries);
                return callOpenAiApi(openaiRequest, n8nConfig);

            } catch (Exception e) {
                lastException = e;
                log.warn("OpenAI API call attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(ContentConstants.RETRY_DELAY_BASE_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException("Request interrupted during retry");
                    }
                }
            }
        }

        throw new BusinessException("OpenAI API failed after " + maxRetries + " attempts: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    /**
     * Validate generated content quality
     */
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

    @Transactional
    protected void saveResponseLogInTransaction(
            User user, Map<String, Object> request, Map<String, Object> response, String model) {
        try {
            String created_at = String.valueOf(response.get("created_at"));
            OpenaiResponseLog responseLog = new OpenaiResponseLog();
            responseLog.setUser(user);
            responseLog.setContentInput(request);
            responseLog.setOpenaiResult(response);
            responseLog.setCreateAt(Instant.ofEpochMilli(Long.parseLong(created_at)));
            responseLog.setResponseTime(Instant.now());
            responseLog.setModel(response.get("model").toString());

            // Extract and save OpenAI response ID
            String openaiResponseId = extractOpenAiResponseId(response);
            if (openaiResponseId != null) {
                responseLog.setOpenaiResponseId(openaiResponseId);
                log.debug("Saved OpenAI response ID: {} for user: {}", openaiResponseId, user.getId());
            }

            openaiResponseLogRepository.save(responseLog);
        } catch (Exception e) {
            log.error("Error saving OpenAI response log for user: {}", user.getId(), e);
            // Don't throw - avoid breaking main flow
        }
    }

    @Transactional
    protected void saveErrorLogInTransaction(User user, String errorMessage, String model) {
        try {
            OpenaiResponseLog errorLog = new OpenaiResponseLog();
            errorLog.setUser(user);
            errorLog.setCreateAt(Instant.now());
            errorLog.setModel(model);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", errorMessage);
            errorLog.setOpenaiResult(errorResult);

            openaiResponseLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Error saving OpenAI error log for user: {}", user.getId(), e);
            // Don't throw - avoid breaking main flow
        }
    }

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

    private Map<String, Object> callOpenAiApi(
            Map<String, Object> openaiRequest, N8nConfig n8nConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(n8nConfig.getXApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(openaiRequest, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                n8nConfig.getAgentUrl(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        if (responseBody == null) {
            throw new BusinessException("Empty response from OpenAI API");
        }

        return responseBody;
    }

    private Map<String, Object> buildOpenAiRequest(
            ContentGenerateRequest request, N8nConfig n8nConfig, Long maxTokens) {
        Map<String, Object> openaiRequest = new HashMap<>();
        openaiRequest.put("model", n8nConfig.getModel());
        openaiRequest.put("max_output_tokens", maxTokens);
        openaiRequest.put("temperature", n8nConfig.getTemperature());
        // openaiRequest.put("top_p", ContentConstants.TOP_P_DEFAULT); // Nucleus
        // sampling for better quality
        // openaiRequest.put("frequency_penalty",
        // ContentConstants.FREQUENCY_PENALTY_DEFAULT); // Reduce repetition
        // openaiRequest.put("presence_penalty",
        // ContentConstants.PRESENCE_PENALTY_DEFAULT); // Encourage diverse content

        // Build structured messages with system and user prompts
        List<Map<String, String>> messages = buildMessages(request);
        openaiRequest.put("input", messages);

        return openaiRequest;
    }

    /**
     * Build structured messages for OpenAI API following best practices
     */
    private List<Map<String, String>> buildMessages(ContentGenerateRequest request) {
        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPrompt(request);

        return List.of(
                Map.of("role", ContentConstants.OPENAI_ROLE_SYSTEM, "content", systemPrompt),
                Map.of("role", ContentConstants.OPENAI_ROLE_USER, "content", userPrompt));
    }

    /**
     * Build system prompt that defines the AI's role and capabilities
     */
    private String buildSystemPrompt(ContentGenerateRequest request) {
        StringBuilder systemPrompt = new StringBuilder();

        // Define role based on content type
        String contentType = StringUtil.defaultIfBlank(
                StringUtil.toLowerCase(request.getContentType()),
                ContentConstants.CONTENT_TYPE_GENERAL);

        if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_BLOG) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_ARTICLE)) {
            systemPrompt.append(
                    "Bạn là một chuyên gia viết blog và bài viết chuyên nghiệp với hơn 10 năm kinh nghiệm. ");
            systemPrompt.append("Bạn có khả năng tạo ra nội dung hấp dẫn, có cấu trúc rõ ràng và tối ưu SEO. ");
        } else if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_SOCIAL) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_FACEBOOK) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_INSTAGRAM)) {
            systemPrompt.append(
                    "Bạn là một chuyên gia marketing trên mạng xã hội với khả năng tạo ra nội dung viral và tương tác cao. ");
            systemPrompt.append("Bạn hiểu rõ về xu hướng, hashtag và cách thu hút sự chú ý của người dùng. ");
        } else if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_EMAIL) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_NEWSLETTER)) {
            systemPrompt.append(
                    "Bạn là một chuyên gia email marketing với khả năng viết email có tỷ lệ mở và click cao. ");
            systemPrompt.append("Bạn biết cách tạo subject line hấp dẫn và call-to-action hiệu quả. ");
        } else if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_PRODUCT) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_DESCRIPTION)) {
            systemPrompt.append(
                    "Bạn là một chuyên gia copywriting sản phẩm với khả năng tạo ra mô tả sản phẩm thuyết phục và bán hàng. ");
            systemPrompt.append("Bạn hiểu tâm lý khách hàng và cách highlight lợi ích sản phẩm. ");
        } else if (StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_AD) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_ADVERTISEMENT)) {
            systemPrompt.append(
                    "Bạn là một chuyên gia quảng cáo với khả năng tạo ra nội dung quảng cáo hiệu quả và thuyết phục. ");
            systemPrompt.append("Bạn biết cách tạo ra hook mạnh mẽ và call-to-action rõ ràng. ");
        } else {
            systemPrompt.append(
                    "Bạn là một chuyên gia viết nội dung đa năng với khả năng tạo ra nội dung chất lượng cao cho nhiều mục đích khác nhau. ");
        }

        // Add language and tone instructions
        String language = StringUtil.defaultIfBlank(request.getLanguage(), ContentConstants.LANGUAGE_VIETNAMESE);
        if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_VIETNAMESE)) {
            systemPrompt.append("Bạn viết bằng tiếng Việt tự nhiên, dễ hiểu và phù hợp với văn hóa Việt Nam. ");
        } else if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_ENGLISH)) {
            systemPrompt.append("You write in natural, clear English that resonates with the target audience. ");
        }

        // Add tone instructions
        if (StringUtil.isNotBlank(request.getTone())) {
            String tone = StringUtil.toLowerCase(request.getTone());
            if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_PROFESSIONAL) ||
                    StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FORMAL)) {
                systemPrompt.append("Giọng điệu của bạn chuyên nghiệp, trang trọng và đáng tin cậy. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FRIENDLY) ||
                    StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_CASUAL)) {
                systemPrompt.append("Giọng điệu của bạn thân thiện, gần gũi và dễ tiếp cận. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_ENTHUSIASTIC) ||
                    StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_EXCITING)) {
                systemPrompt.append("Giọng điệu của bạn nhiệt huyết, hứng khởi và tràn đầy năng lượng. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_HUMOROUS) ||
                    StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FUNNY)) {
                systemPrompt.append("Giọng điệu của bạn hài hước, vui vẻ nhưng vẫn phù hợp với chủ đề. ");
            } else if (StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_AUTHORITATIVE) ||
                    StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_EXPERT)) {
                systemPrompt.append("Giọng điệu của bạn có thẩm quyền, chuyên gia và thuyết phục. ");
            }
        }

        // Add industry-specific knowledge
        if (request.getIndustry() != null) {
            systemPrompt.append("Bạn có hiểu biết sâu sắc về lĩnh vực ").append(request.getIndustry())
                    .append(" và có thể sử dụng thuật ngữ chuyên ngành một cách chính xác. ");
        }

        // Add quality standards
        systemPrompt.append("\nYêu cầu chất lượng:\n");
        systemPrompt.append("- Nội dung phải chính xác, hữu ích và có giá trị\n");
        systemPrompt.append("- Cấu trúc rõ ràng với đầu, thân, kết\n");
        systemPrompt.append("- Sử dụng từ ngữ phù hợp với đối tượng mục tiêu\n");
        systemPrompt.append("- Tránh lặp từ và câu cấu trúc đơn điệu\n");
        systemPrompt.append("- Đảm bảo tính nhất quán về giọng điệu và phong cách");

        return systemPrompt.toString();
    }

    /**
     * Build user prompt with specific instructions and context
     */
    private String buildUserPrompt(ContentGenerateRequest request) {
        StringBuilder userPrompt = new StringBuilder();

        // Main content request
        userPrompt.append("Hãy tạo nội dung dựa trên yêu cầu sau:\n\n");
        userPrompt.append("📝 **Nội dung gốc/Ý tưởng:**\n");
        userPrompt.append(request.getContent()).append("\n\n");

        // Content specifications
        userPrompt.append("📋 **Thông số kỹ thuật:**\n");

        if (request.getContentType() != null) {
            userPrompt.append("• Loại nội dung: ").append(getContentTypeDescription(request.getContentType()))
                    .append("\n");
        }

        if (request.getTargetAudience() != null) {
            userPrompt.append("• Đối tượng mục tiêu: ").append(request.getTargetAudience()).append("\n");
        }

        if (request.getIndustry() != null) {
            userPrompt.append("• Lĩnh vực: ").append(request.getIndustry()).append("\n");
        }

        if (request.getTone() != null) {
            userPrompt.append("• Giọng điệu: ").append(getToneDescription(request.getTone())).append("\n");
        }

        if (request.getLanguage() != null) {
            userPrompt.append("• Ngôn ngữ: ").append(getLanguageDescription(request.getLanguage())).append("\n");
        }

        // Add specific instructions based on content type
        userPrompt.append("\n🎯 **Yêu cầu cụ thể:**\n");
        userPrompt.append(getContentTypeInstructions(request.getContentType()));

        // Final instructions
        userPrompt.append("\n✨ **Lưu ý quan trọng:**\n");
        userPrompt.append("- Tạo nội dung hoàn chỉnh, sẵn sàng sử dụng\n");
        userPrompt.append("- Đảm bảo nội dung phù hợp với đối tượng và mục đích\n");
        userPrompt.append("- Sử dụng formatting phù hợp (tiêu đề, đoạn văn, bullet points)\n");
        userPrompt.append("- Tránh nội dung nhạy cảm hoặc không phù hợp\n");

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            userPrompt.append("- Sử dụng tiêu đề: \"").append(request.getTitle()).append("\"\n");
        }

        return userPrompt.toString();
    }

    /**
     * Get detailed description for content type
     */
    private String getContentTypeDescription(String contentType) {
        if (StringUtil.isBlank(contentType)) {
            return contentType;
        }

        String normalizedType = StringUtil.toLowerCase(contentType);

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_BLOG)) {
            return ContentConstants.DESC_BLOG;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_ARTICLE)) {
            return ContentConstants.DESC_ARTICLE;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_SOCIAL)) {
            return ContentConstants.DESC_SOCIAL;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_FACEBOOK)) {
            return ContentConstants.DESC_FACEBOOK;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_INSTAGRAM)) {
            return ContentConstants.DESC_INSTAGRAM;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_EMAIL)) {
            return ContentConstants.DESC_EMAIL;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_NEWSLETTER)) {
            return ContentConstants.DESC_NEWSLETTER;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_PRODUCT)) {
            return ContentConstants.DESC_PRODUCT;
        }
        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_AD)) {
            return ContentConstants.DESC_AD;
        }

        return contentType;
    }

    /**
     * Get detailed description for tone
     */
    private String getToneDescription(String tone) {
        if (StringUtil.isBlank(tone)) {
            return tone;
        }

        String normalizedTone = StringUtil.toLowerCase(tone);

        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_PROFESSIONAL)) {
            return ContentConstants.DESC_TONE_PROFESSIONAL;
        }
        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_FRIENDLY)) {
            return ContentConstants.DESC_TONE_FRIENDLY;
        }
        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_ENTHUSIASTIC)) {
            return ContentConstants.DESC_TONE_ENTHUSIASTIC;
        }
        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_HUMOROUS)) {
            return ContentConstants.DESC_TONE_HUMOROUS;
        }
        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_AUTHORITATIVE)) {
            return ContentConstants.DESC_TONE_AUTHORITATIVE;
        }
        if (StringUtil.equalsIgnoreCase(normalizedTone, ContentConstants.TONE_CASUAL)) {
            return ContentConstants.DESC_TONE_CASUAL;
        }

        return tone;
    }

    /**
     * Get language description
     */
    private String getLanguageDescription(String language) {
        if (StringUtil.isBlank(language)) {
            return language;
        }

        if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_VIETNAMESE)) {
            return ContentConstants.DESC_LANGUAGE_VI;
        }
        if (StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_ENGLISH)) {
            return ContentConstants.DESC_LANGUAGE_EN;
        }

        return language;
    }

    /**
     * Get specific instructions based on content type
     */
    private String getContentTypeInstructions(String contentType) {
        if (contentType == null) {
            return "- Tạo nội dung chất lượng cao phù hợp với mục đích sử dụng";
        }

        String normalizedType = StringUtil.toLowerCase(contentType);

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_BLOG) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_ARTICLE)) {
            return """
                    - Bắt đầu với hook hấp dẫn để thu hút người đọc
                    - Sử dụng tiêu đề phụ (H2, H3) để chia nhỏ nội dung
                    - Bao gồm introduction, body với các điểm chính, và conclusion
                    - Thêm call-to-action phù hợp ở cuối bài
                    - Sử dụng bullet points và số liệu khi cần thiết
                    """;
        }

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_SOCIAL) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_FACEBOOK) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_INSTAGRAM)) {
            return """
                    - Bắt đầu với câu mở đầu thu hút sự chú ý
                    - Sử dụng emoji phù hợp để tăng tính tương tác
                    - Bao gồm call-to-action rõ ràng (like, share, comment)
                    - Thêm hashtag liên quan (3-5 hashtag cho Facebook, 10-15 cho Instagram)
                    - Tạo nội dung khuyến khích tương tác và chia sẻ
                    """;
        }

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_EMAIL) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_NEWSLETTER)) {
            return """
                    - Tạo subject line hấp dẫn (không quá 50 ký tự)
                    - Bắt đầu với lời chào cá nhân hóa
                    - Cấu trúc rõ ràng với các điểm chính
                    - Bao gồm call-to-action mạnh mẽ và rõ ràng
                    - Kết thúc với lời cảm ơn và thông tin liên hệ
                    """;
        }

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_PRODUCT) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_DESCRIPTION)) {
            return """
                    - Tập trung vào lợi ích thay vì tính năng
                    - Sử dụng từ ngữ thuyết phục và tạo cảm xúc
                    - Bao gồm thông tin kỹ thuật quan trọng
                    - Thêm social proof nếu có thể
                    - Kết thúc với call-to-action mua hàng rõ ràng
                    """;
        }

        if (StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_AD) ||
                StringUtil.equalsIgnoreCase(normalizedType, ContentConstants.CONTENT_TYPE_ADVERTISEMENT)) {
            return """
                    - Tạo headline mạnh mẽ và thu hút ngay từ đầu
                    - Tập trung vào pain point và solution
                    - Sử dụng từ ngữ tạo cấp bách (limited time, exclusive)
                    - Bao gồm offer cụ thể và hấp dẫn
                    - Call-to-action rõ ràng và dễ thực hiện
                    """;
        }

        return "- Tạo nội dung chất lượng cao phù hợp với mục đích sử dụng";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processOpenAiResponse(
            Map<String, Object> responseBody, long startTime) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check for API errors first
            if (responseBody.containsKey("error") && responseBody.get("error") != null) {
                Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                String errorMessage = (String) error.get("message");
                log.error("OpenAI API error: {}", errorMessage);

                result.put("status", "FAILED");
                result.put("errorMessage", "OpenAI API error: " + errorMessage);
                return result;
            }

            // Check response status - allow both "completed" and "incomplete" with content
            String status = (String) responseBody.get("status");
            boolean isCompleted = "completed".equals(status);
            boolean isIncompleteWithContent = "incomplete".equals(status);

            if (!isCompleted && !isIncompleteWithContent) {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "OpenAI response status is not valid: " + status);
                return result;
            }

            // Log incomplete status for monitoring
            if (isIncompleteWithContent) {
                Object incompleteDetails = responseBody.get("incomplete_details");
                log.info("Processing incomplete OpenAI response - status: {}, details: {}",
                        status, incompleteDetails);
            }

            // Process new OpenAI response format with 'output' array
            Object outputObj = responseBody.get("output");
            if (outputObj instanceof List<?> outputList && !outputList.isEmpty()) {
                List<Map<String, Object>> outputs = (List<Map<String, Object>>) outputList;
                Map<String, Object> firstOutput = outputs.get(0);

                // Check output status - allow both "completed" and "incomplete" with content
                String outputStatus = (String) firstOutput.get("status");
                boolean outputCompleted = "completed".equals(outputStatus);
                boolean outputIncomplete = "incomplete".equals(outputStatus);

                if (!outputCompleted && !outputIncomplete) {
                    result.put("status", ContentConstants.STATUS_FAILED);
                    result.put("errorMessage", "Invalid output status: " + outputStatus);
                    return result;
                }

                if (outputIncomplete) {
                    log.info("Processing incomplete output - status: {}", outputStatus);
                }

                // Extract content from the content array
                Object contentObj = firstOutput.get("content");
                if (contentObj instanceof List<?> contentList && !contentList.isEmpty()) {
                    List<Map<String, Object>> contents = (List<Map<String, Object>>) contentList;

                    // Find the text content
                    String generatedText = null;
                    for (Map<String, Object> contentItem : contents) {
                        String type = (String) contentItem.get("type");
                        if ("output_text".equals(type)) {
                            generatedText = (String) contentItem.get("text");
                            break;
                        }
                    }

                    if (StringUtil.isBlank(generatedText)) {
                        result.put("status", ContentConstants.STATUS_FAILED);
                        result.put("errorMessage", "Empty content generated by OpenAI");
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
                } else {
                    result.put("status", ContentConstants.STATUS_FAILED);
                    result.put("errorMessage", "No content array found in OpenAI output");
                    return result;
                }
            } else {
                result.put("status", ContentConstants.STATUS_FAILED);
                result.put("errorMessage", "No output array returned from OpenAI");
                return result;
            }

            // Extract usage information
            Object usageObj = responseBody.get("usage");
            if (usageObj instanceof Map<?, ?>) {
                Map<String, Object> usage = (Map<String, Object>) usageObj;
                result.put("tokensUsed", usage.get("total_tokens"));
                result.put("promptTokens", usage.get("input_tokens"));
                result.put("completionTokens", usage.get("output_tokens"));

                // Calculate cost estimation (approximate)
                Integer totalTokens = (Integer) usage.get("total_tokens");
                if (totalTokens != null) {
                    result.put("estimatedCost", calculateEstimatedCost(totalTokens));
                }
            }

            result.put("processingTimeMs", System.currentTimeMillis() - startTime);

            // Extract and include OpenAI response ID
            String openaiResponseId = extractOpenAiResponseId(responseBody);
            if (openaiResponseId != null) {
                result.put("openaiResponseId", openaiResponseId);
            }

            // Set final status based on response completeness
            if (isCompleted) {
                result.put("status", ContentConstants.STATUS_COMPLETED);
            } else if (isIncompleteWithContent) {
                result.put("status", ContentConstants.STATUS_COMPLETED);
                result.put("isIncomplete", true);

                // Add incomplete details if available
                Object incompleteDetails = responseBody.get("incomplete_details");
                if (incompleteDetails != null) {
                    result.put("incompleteDetails", incompleteDetails);
                }

                log.info("Successfully processed incomplete OpenAI response with valid content");
            }

            log.debug("Successfully processed OpenAI response - content length: {}, tokens used: {}",
                    result.get("characterCount"), result.get("tokensUsed"));

        } catch (Exception e) {
            log.error("Error processing OpenAI response", e);
            result.put("status", ContentConstants.STATUS_FAILED);
            result.put("errorMessage", "Failed to process OpenAI response: " + e.getMessage());
        }

        return result;
    }

    /**
     * Clean and format the generated content
     */
    private String cleanGeneratedContent(String content) {
        if (content == null)
            return "";

        // Remove excessive whitespace
        content = content.trim().replaceAll("\\s+", " ");

        // Fix common formatting issues
        content = content.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n"); // Max 2 consecutive newlines
        content = content.replaceAll(
                "([.!?])([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ])", "$1 $2"); // Add
                                                                                                               // space
                                                                                                               // after
                                                                                                               // sentence
                                                                                                               // endings

        // Ensure proper paragraph breaks
        content = content.replaceAll("\\n", "\n\n");

        return content.trim();
    }

    /**
     * Calculate basic quality score based on content characteristics
     */
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

        // Sentence variety (different sentence lengths)
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
        long uniqueWords = java.util.Arrays.stream(words).distinct().count();
        double uniqueRatio = (double) uniqueWords / words.length;
        if (uniqueRatio > 0.7) {
            score += 1.0;
        } else if (uniqueRatio < 0.5) {
            score -= 1.0;
        }

        return Math.max(0.0, Math.min(10.0, score));
    }

    /**
     * Calculate basic readability score
     */
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

    /**
     * Calculate estimated cost based on token usage
     */
    private double calculateEstimatedCost(int totalTokens) {
        // Approximate cost for GPT-4o-mini: $0.00015 per 1K tokens input, $0.0006 per
        // 1K tokens output
        // Using average rate from constants
        return (totalTokens / 1000.0) * ContentConstants.COST_PER_1K_TOKENS;
    }

    /**
     * Count sentences in text
     */
    private int countSentences(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("[.!?]+").length;
    }

    /**
     * Count paragraphs in text
     */
    private int countParagraphs(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("\\n\\s*\\n").length;
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Extract OpenAI response ID from the response
     */
    private String extractOpenAiResponseId(Map<String, Object> response) {
        try {
            // Try to extract 'id' field from the response
            Object idObj = response.get("id");
            if (idObj != null) {
                return idObj.toString();
            }

            // Alternative: try to extract from system_fingerprint or other fields
            Object systemFingerprint = response.get("system_fingerprint");
            if (systemFingerprint != null) {
                return systemFingerprint.toString();
            }

            log.debug("No OpenAI response ID found in response");
            return null;
        } catch (Exception e) {
            log.warn("Error extracting OpenAI response ID: {}", e.getMessage());
            return null;
        }
    }
}
