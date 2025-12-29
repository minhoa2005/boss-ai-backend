package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.*;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.exception.InternalServerException;
import ai.content.auto.mapper.ContentGenerationMapper;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.service.ai.AIProviderManager;
import ai.content.auto.util.SecurityUtil;
import ai.content.auto.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentGenerationRepository contentGenerationRepository;
    private final ContentGenerationMapper contentGenerationMapper;
    private final AIProviderManager aiProviderManager;
    private final N8nService n8nService;
    private final SecurityUtil securityUtil;
    private final ConfigurationValidationService configurationValidationService;
    private final ContentNormalizationService contentNormalizationService;
    private final ContentVersioningService contentVersioningService;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    public ContentGenerateResponse generateContent(ContentGenerateRequest request) {
        String tempJobId = "sync_" + System.currentTimeMillis(); // Temporary job ID for WebSocket updates

        try {
            // 1. Normalize input to match database values
            ContentGenerateRequest normalizedRequest = contentNormalizationService.normalizeGenerateRequest(request);

            // 2. Validate normalized input
            validateGenerateRequest(normalizedRequest);

            // 3. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Generating content for user: {}", currentUser.getId());

            // Send initial progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Starting content generation...",
                    "progress", 10));

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Calling AI provider...",
                    "progress", 30));

            // 4. Call AI provider manager with normalized request (transparent provider
            // selection)
            ContentGenerateResponse response = aiProviderManager.generateContent(normalizedRequest, currentUser);

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Processing AI response...",
                    "progress", 60));

            // 5. Set title if not provided
            if (normalizedRequest.getTitle() != null) {
                response.setTitle(normalizedRequest.getTitle());
            } else if (response.getTitle() == null) {
                response.setTitle(generateTitle(response.getGeneratedContent()));
            }

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Saving generated content...",
                    "progress", 80));

            // 6. Automatically save generated content to database
            ContentGeneration savedContent = saveGeneratedContentInTransaction(normalizedRequest, response,
                    currentUser);

            // 7. Set the saved content ID in response for reference
            response.setContentId(savedContent.getId());

            // 8. Create version from generated content
            createVersionFromGeneratedContent(savedContent, response, currentUser.getId());

            // Send completion notification via WebSocket
            webSocketService.sendJobCompletionNotification(currentUser.getId(), tempJobId, true, Map.of(
                    "contentId", savedContent.getId(),
                    "title", response.getTitle(),
                    "generatedContent", response.getGeneratedContent(),
                    "wordCount", response.getWordCount()));

            log.info("Content generated and saved successfully with ID: {} for user: {}", savedContent.getId(),
                    currentUser.getId());
            return response;

        } catch (BusinessException e) {
            log.error("Business error generating content for user: {}", securityUtil.getCurrentUserId(), e);

            // Send error notification via WebSocket
            webSocketService.sendJobErrorNotification(securityUtil.getCurrentUserId(), tempJobId,
                    "Content generation failed: " + e.getMessage(), Map.of(
                            "error", e.getMessage()));

            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating content for user: {}", securityUtil.getCurrentUserId(), e);

            // Send error notification via WebSocket
            webSocketService.sendJobErrorNotification(securityUtil.getCurrentUserId(), tempJobId,
                    "Unexpected error: " + e.getMessage(), Map.of(
                            "error", e.getMessage()));

            throw new InternalServerException("Failed to generate content");
        }
    }

    /**
     * Generate content for asynchronous processing with explicit user context
     * This method is used by queue processing where security context is not
     * available
     */
    public ContentGenerateResponse generateContentForUser(ContentGenerateRequest request, Long userId) {
        String tempJobId = "temp_" + System.currentTimeMillis(); // Temporary job ID for WebSocket updates

        try {
            // 1. Normalize input to match database values
            ContentGenerateRequest normalizedRequest = contentNormalizationService.normalizeGenerateRequest(request);

            // 2. Validate normalized input
            validateGenerateRequest(normalizedRequest);

            // 3. Get user by ID (for async processing without security context)
            User user = getUserById(userId);
            log.info("Generating content for user: {} (async processing)", user.getId());

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Calling AI provider...",
                    "progress", 50));

            // 4. Call AI provider manager with normalized request
            ContentGenerateResponse response = aiProviderManager.generateContent(normalizedRequest, user);

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Saving generated content...",
                    "progress", 70));

            // 5. Set title if not provided
            if (normalizedRequest.getTitle() != null) {
                response.setTitle(normalizedRequest.getTitle());
            } else if (response.getTitle() == null) {
                response.setTitle(generateTitle(response.getGeneratedContent()));
            }

            // 6. Automatically save generated content to database
            ContentGeneration savedContent = saveGeneratedContentInTransaction(normalizedRequest, response, user);

            // 7. Set the saved content ID in response for reference
            response.setContentId(savedContent.getId());

            // Send progress update via WebSocket
            webSocketService.sendJobStatusUpdate(tempJobId, "PROCESSING", Map.of(
                    "message", "Creating content version...",
                    "progress", 90));

            // 8. Create version from generated content
            createVersionFromGeneratedContent(savedContent, response, userId);

            log.info("Content generated and saved successfully with ID: {} for user: {} (async processing)",
                    savedContent.getId(), user.getId());
            return response;

        } catch (BusinessException e) {
            log.error("Business error generating content for user: {} (async processing)", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating content for user: {} (async processing)", userId, e);
            throw new InternalServerException("Failed to generate content");
        }
    }

    /**
     * Generate content asynchronously using the queue system
     * This method queues the request and returns immediately with job information
     */
    public ContentGenerateResponse generateContentAsync(ContentGenerateRequest request) {
        try {
            // 1. Normalize input to match database values
            ContentGenerateRequest normalizedRequest = contentNormalizationService.normalizeGenerateRequest(request);

            // 2. Validate normalized input
            validateGenerateRequest(normalizedRequest);

            // 3. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Queuing async content generation for user: {}", currentUser.getId());

            // 4. For now, fall back to synchronous processing with WebSocket updates
            // TODO: Integrate with QueueManagementService for true async processing
            return generateContent(request);

        } catch (BusinessException e) {
            log.error("Business error queuing async content generation for user: {}", securityUtil.getCurrentUserId(),
                    e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error queuing async content generation for user: {}", securityUtil.getCurrentUserId(),
                    e);
            throw new InternalServerException("Failed to queue content generation");
        }
    }

    public ContentGenerationDto saveContent(ContentSaveRequest request) {
        try {
            // 1. Normalize input to match database values
            ContentSaveRequest normalizedRequest = contentNormalizationService.normalizeSaveRequest(request);

            // 2. Validate normalized input
            validateSaveRequest(normalizedRequest);

            // 3. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Saving content for user: {}", currentUser.getId());

            // 4. Save content in transaction with normalized request
            ContentGeneration saved = saveContentInTransaction(normalizedRequest, currentUser);

            // 5. Create version from saved content
            createVersionFromSavedContent(saved, normalizedRequest);

            log.info("Content saved successfully with ID: {} for user: {}", saved.getId(), currentUser.getId());
            return contentGenerationMapper.toDto(saved);

        } catch (BusinessException e) {
            log.error("Business error saving content for user: {}", securityUtil.getCurrentUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error saving content for user: {}", securityUtil.getCurrentUserId(), e);
            throw new InternalServerException("Failed to save content");
        }
    }

    public Map<String, Object> triggerWorkflow(ContentWorkflowRequest request) {
        try {
            // 1. Normalize input to match database values
            ContentWorkflowRequest normalizedRequest = contentNormalizationService.normalizeWorkflowRequest(request);

            // 2. Validate normalized input
            validateWorkflowRequest(normalizedRequest);

            // 3. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Triggering workflow for user: {}", currentUser.getId());

            // 4. Save content and trigger workflow in transaction with normalized request
            ContentGeneration saved = saveWorkflowContentInTransaction(normalizedRequest, currentUser);

            // 5. Trigger N8N workflow (no transaction) with normalized request
            Map<String, Object> workflowResult = n8nService.triggerWorkflow(normalizedRequest, currentUser.getId());

            // 5. Update status in separate transaction
            updateWorkflowStatusInTransaction(saved.getId(), workflowResult);

            log.info("Workflow triggered successfully for content ID: {} and user: {}", saved.getId(),
                    currentUser.getId());
            return workflowResult;

        } catch (BusinessException e) {
            log.error("Business error triggering workflow for user: {}", securityUtil.getCurrentUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error triggering workflow for user: {}", securityUtil.getCurrentUserId(), e);
            throw new InternalServerException("Failed to trigger workflow");
        }
    }

    @Transactional(readOnly = true)
    public List<ContentGenerationDto> getUserContents() {
        try {
            // 1. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Retrieving contents for user: {}", currentUser.getId());

            // 2. Query database
            List<ContentGeneration> contents = contentGenerationRepository.findByUserOrderByCreatedAtDesc(currentUser);

            // 3. Map to DTOs
            return contents.stream()
                    .map(contentGenerationMapper::toDto)
                    .toList();

        } catch (BusinessException e) {
            log.error("Business error retrieving contents for user: {}", securityUtil.getCurrentUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving contents for user: {}", securityUtil.getCurrentUserId(), e);
            throw new InternalServerException("Failed to retrieve contents");
        }
    }

    @Transactional(readOnly = true)
    public ContentGenerationDto getContentById(Long id) {
        try {
            // 1. Validate input
            if (id == null) {
                throw new BusinessException("Content ID is required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Retrieving content ID: {} for user: {}", id, userId);

            // 3. Query database
            ContentGeneration content = contentGenerationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Content not found"));

            // 4. Check ownership
            if (!content.getUser().getId().equals(userId)) {
                throw new BusinessException("Access denied to this content");
            }

            return contentGenerationMapper.toDto(content);

        } catch (BusinessException e) {
            log.error("Business error retrieving content ID: {} for user: {}", id, securityUtil.getCurrentUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving content ID: {} for user: {}", id, securityUtil.getCurrentUserId(),
                    e);
            throw new InternalServerException("Failed to retrieve content");
        }
    }

    // Private transactional methods for database operations

    @Transactional
    protected ContentGeneration saveContentInTransaction(ContentSaveRequest request, User user) {
        ContentGeneration contentGeneration = buildContentGeneration(request, user);
        contentGeneration.setStatus(ContentConstants.STATUS_SAVED);
        return contentGenerationRepository.save(contentGeneration);
    }

    @Transactional
    protected ContentGeneration saveGeneratedContentInTransaction(ContentGenerateRequest request,
            ContentGenerateResponse response, User user) {
        ContentGeneration contentGeneration = buildContentGenerationFromGenerate(request, response, user);
        contentGeneration.setStatus(ContentConstants.STATUS_GENERATED);
        contentGeneration.setCompletedAt(Instant.now());
        return contentGenerationRepository.save(contentGeneration);
    }

    @Transactional
    protected ContentGeneration saveWorkflowContentInTransaction(ContentWorkflowRequest request, User user) {
        ContentGeneration contentGeneration = buildContentGenerationFromWorkflow(request, user);
        contentGeneration.setStatus(ContentConstants.STATUS_WORKFLOW_TRIGGERED);
        contentGeneration.setStartedAt(Instant.now());
        return contentGenerationRepository.save(contentGeneration);
    }

    @Transactional
    protected void updateWorkflowStatusInTransaction(Long contentId, Map<String, Object> workflowResult) {
        try {
            ContentGeneration contentGeneration = contentGenerationRepository.findById(contentId).orElse(null);
            if (contentGeneration != null) {
                String status = (String) workflowResult.get("status");
                if (StringUtil.equalsIgnoreCase(status, "SUCCESS")) {
                    contentGeneration.setStatus(ContentConstants.STATUS_WORKFLOW_COMPLETED);
                    contentGeneration.setCompletedAt(Instant.now());
                } else {
                    contentGeneration.setStatus(ContentConstants.STATUS_WORKFLOW_FAILED);
                    contentGeneration.setFailedAt(Instant.now());
                    contentGeneration.setErrorMessage((String) workflowResult.get("message"));
                }
                contentGenerationRepository.save(contentGeneration);
            }
        } catch (Exception e) {
            log.error("Error updating workflow status for content ID: {}", contentId, e);
            // Don't throw - avoid breaking main workflow
        }
    }

    // Private validation methods

    private void validateGenerateRequest(ContentGenerateRequest request) {
        if (request == null) {
            throw new BusinessException("Generate request is required");
        }
        if (StringUtil.isBlank(request.getContent())) {
            throw new BusinessException("Content is required");
        }
        if (StringUtil.isBlank(request.getContentType())) {
            throw new BusinessException("Content type is required");
        }

        // Validate content type against database configurations
        // if
        // (!configurationValidationService.isValidContentType(request.getContentType()))
        // {
        // throw new BusinessException("Invalid content type: " +
        // request.getContentType());
        // }

        // Validate tone if provided
        // if (StringUtil.isNotBlank(request.getTone()) &&
        // !configurationValidationService.isValidTone(request.getTone())) {
        // throw new BusinessException("Invalid tone: " + request.getTone());
        // }

        // Validate language if provided
        if (StringUtil.isNotBlank(request.getLanguage()) &&
                !configurationValidationService.isValidLanguage(request.getLanguage())) {
            throw new BusinessException("Invalid language: " + request.getLanguage());
        }

        // Validate industry if provided
        if (StringUtil.isNotBlank(request.getIndustry()) &&
                !configurationValidationService.isValidIndustry(request.getIndustry())) {
            throw new BusinessException("Invalid industry: " + request.getIndustry());
        }

        // Validate target audience if provided
        // if (StringUtil.isNotBlank(request.getTargetAudience()) &&
        // !configurationValidationService.isValidTargetAudience(request.getTargetAudience()))
        // {
        // throw new BusinessException("Invalid target audience: " +
        // request.getTargetAudience());
        // }
    }

    private void validateSaveRequest(ContentSaveRequest request) {
        if (request == null) {
            throw new BusinessException("Save request is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }

        // Validate content type if provided
        // if (StringUtil.isNotBlank(request.getContentType()) &&
        // !configurationValidationService.isValidContentType(request.getContentType()))
        // {
        // throw new BusinessException("Invalid content type: " +
        // request.getContentType());
        // }

        // Validate tone if provided
        // if (StringUtil.isNotBlank(request.getTone()) &&
        // !configurationValidationService.isValidTone(request.getTone())) {
        // throw new BusinessException("Invalid tone: " + request.getTone());
        // }

        // Validate language if provided
        if (StringUtil.isNotBlank(request.getLanguage()) &&
                !configurationValidationService.isValidLanguage(request.getLanguage())) {
            throw new BusinessException("Invalid language: " + request.getLanguage());
        }

        // Validate industry if provided
        if (StringUtil.isNotBlank(request.getIndustry()) &&
                !configurationValidationService.isValidIndustry(request.getIndustry())) {
            throw new BusinessException("Invalid industry: " + request.getIndustry());
        }

        // Validate target audience if provided
        // if (StringUtil.isNotBlank(request.getTargetAudience()) &&
        // !configurationValidationService.isValidTargetAudience(request.getTargetAudience()))
        // {
        // throw new BusinessException("Invalid target audience: " +
        // request.getTargetAudience());
        // }
    }

    private void validateWorkflowRequest(ContentWorkflowRequest request) {
        if (request == null) {
            throw new BusinessException("Workflow request is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }

        // Validate content type if provided
        // if (StringUtil.isNotBlank(request.getContentType()) &&
        // !configurationValidationService.isValidContentType(request.getContentType()))
        // {
        // throw new BusinessException("Invalid content type: " +
        // request.getContentType());
        // }

        // Validate tone if provided
        // if (StringUtil.isNotBlank(request.getTone()) &&
        // !configurationValidationService.isValidTone(request.getTone())) {
        // throw new BusinessException("Invalid tone: " + request.getTone());
        // }

        // Validate language if provided
        if (StringUtil.isNotBlank(request.getLanguage()) &&
                !configurationValidationService.isValidLanguage(request.getLanguage())) {
            throw new BusinessException("Invalid language: " + request.getLanguage());
        }

        // Validate industry if provided
        if (StringUtil.isNotBlank(request.getIndustry()) &&
                !configurationValidationService.isValidIndustry(request.getIndustry())) {
            throw new BusinessException("Invalid industry: " + request.getIndustry());
        }

        // Validate target audience if provided
        // if (StringUtil.isNotBlank(request.getTargetAudience()) &&
        // !configurationValidationService.isValidTargetAudience(request.getTargetAudience()))
        // {
        // throw new BusinessException("Invalid target audience: " +
        // request.getTargetAudience());
        // }
    }

    // Private helper methods

    private ContentGeneration buildContentGeneration(ContentSaveRequest request, User user) {
        ContentGeneration contentGeneration = new ContentGeneration();
        contentGeneration.setUser(user);
        contentGeneration.setContentType(request.getContentType());
        contentGeneration.setGeneratedContent(request.getGeneratedContent());
        contentGeneration.setTitle(request.getTitle());
        contentGeneration.setIndustry(request.getIndustry());
        contentGeneration.setTargetAudience(request.getTargetAudience());
        contentGeneration.setTone(request.getTone());
        contentGeneration.setLanguage(request.getLanguage());
        contentGeneration.setPrompt(request.getPrompt());
        contentGeneration.setAiProvider(ContentConstants.AI_PROVIDER_OPENAI);
        contentGeneration.setAiModel(ContentConstants.DEFAULT_AI_MODEL);
        contentGeneration.setRetryCount(0);
        contentGeneration.setMaxRetries(ContentConstants.DEFAULT_MAX_RETRIES);
        contentGeneration.setIsBillable(true);
        contentGeneration.setCreatedAt(Instant.now());
        contentGeneration.setUpdatedAt(Instant.now());
        contentGeneration.setCurrentVersion(1);

        // Calculate word and character count
        if (request.getGeneratedContent() != null) {
            contentGeneration.setWordCount(countWords(request.getGeneratedContent()));
            contentGeneration.setCharacterCount(request.getGeneratedContent().length());
        }

        return contentGeneration;
    }

    private ContentGeneration buildContentGenerationFromWorkflow(ContentWorkflowRequest request, User user) {
        ContentGeneration contentGeneration = new ContentGeneration();
        contentGeneration.setUser(user);
        contentGeneration.setContentType(request.getContentType());
        contentGeneration.setGeneratedContent(request.getGeneratedContent());
        contentGeneration.setTitle(request.getTitle());
        contentGeneration.setIndustry(request.getIndustry());
        contentGeneration.setTargetAudience(request.getTargetAudience());
        contentGeneration.setTone(request.getTone());
        contentGeneration.setLanguage(request.getLanguage());
        contentGeneration.setPrompt(request.getPrompt());
        contentGeneration.setAiProvider(ContentConstants.AI_PROVIDER_OPENAI);
        contentGeneration.setAiModel(ContentConstants.DEFAULT_AI_MODEL);
        contentGeneration.setRetryCount(0);
        contentGeneration.setMaxRetries(ContentConstants.DEFAULT_MAX_RETRIES);
        contentGeneration.setIsBillable(true);
        contentGeneration.setCreatedAt(Instant.now());
        contentGeneration.setUpdatedAt(Instant.now());
        contentGeneration.setCurrentVersion(1);

        // Calculate word and character count
        if (request.getGeneratedContent() != null) {
            contentGeneration.setWordCount(countWords(request.getGeneratedContent()));
            contentGeneration.setCharacterCount(request.getGeneratedContent().length());
        }

        return contentGeneration;
    }

    private ContentGeneration buildContentGenerationFromGenerate(ContentGenerateRequest request,
            ContentGenerateResponse response, User user) {
        ContentGeneration contentGeneration = new ContentGeneration();
        contentGeneration.setUser(user);
        contentGeneration.setContentType(request.getContentType());
        contentGeneration.setGeneratedContent(response.getGeneratedContent());
        contentGeneration.setTitle(response.getTitle());
        contentGeneration.setIndustry(request.getIndustry());
        contentGeneration.setTargetAudience(request.getTargetAudience());
        contentGeneration.setTone(request.getTone());
        contentGeneration.setLanguage(request.getLanguage());
        contentGeneration.setPrompt(request.getContent()); // Original content as prompt

        // AI provider information from response
        contentGeneration.setAiProvider(
                response.getAiProvider() != null ? response.getAiProvider() : ContentConstants.AI_PROVIDER_OPENAI);
        contentGeneration
                .setAiModel(response.getAiModel() != null ? response.getAiModel() : ContentConstants.DEFAULT_AI_MODEL);

        // Generation metrics from response
        contentGeneration.setWordCount(response.getWordCount());
        contentGeneration.setCharacterCount(response.getCharacterCount());
        contentGeneration.setTokensUsed(response.getTokensUsed());
        contentGeneration.setGenerationCost(response.getGenerationCost());
        contentGeneration.setProcessingTimeMs(response.getProcessingTimeMs());

        // Quality scores from response
        contentGeneration.setQualityScore(response.getQualityScore());
        contentGeneration.setReadabilityScore(response.getReadabilityScore());
        contentGeneration.setSentimentScore(response.getSentimentScore());

        // OpenAI response ID from response
        contentGeneration.setOpenaiResponseId(response.getOpenaiResponseId());

        // Default values
        contentGeneration.setRetryCount(0);
        contentGeneration.setMaxRetries(ContentConstants.DEFAULT_MAX_RETRIES);
        contentGeneration.setIsBillable(true);
        contentGeneration.setCreatedAt(Instant.now());
        contentGeneration.setUpdatedAt(Instant.now());
        contentGeneration.setCurrentVersion(1);
        contentGeneration.setStartedAt(Instant.now());

        return contentGeneration;
    }

    private String generateTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Generated Content";
        }

        // Extract first sentence or first 50 characters as title
        String[] sentences = content.split("[.!?]");
        if (sentences.length > 0 && sentences[0].length() <= 100) {
            return sentences[0].trim();
        }

        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Get user by ID for async processing
     * Used when security context is not available (e.g., scheduled tasks)
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
    }

    /**
     * Create a version from saved content.
     * 
     * @param saved   Saved content generation
     * @param request Original save request
     */
    private void createVersionFromSavedContent(ContentGeneration saved, ContentSaveRequest request) {
        try {
            // Build ContentGenerateResponse from saved content
            ContentGenerateResponse response = new ContentGenerateResponse();
            response.setGeneratedContent(saved.getGeneratedContent());
            response.setTitle(saved.getTitle());
            response.setWordCount(saved.getWordCount());
            response.setCharacterCount(saved.getCharacterCount());
            response.setAiProvider(saved.getAiProvider());
            response.setAiModel(saved.getAiModel());
            response.setIndustry(saved.getIndustry());
            response.setTargetAudience(saved.getTargetAudience());
            response.setTone(saved.getTone());
            response.setLanguage(saved.getLanguage());
            response.setStatus("SAVED");

            // Create version
            contentVersioningService.createVersion(saved.getId(), response);

            log.info("Version created for saved content: {}", saved.getId());
        } catch (Exception e) {
            log.error("Error creating version for saved content: {}", saved.getId(), e);
            // Don't throw - avoid breaking content save operation
        }
    }

    /**
     * Create a version from generated content.
     * 
     * @param saved    Saved content generation
     * @param response Original generate response
     * @param userId   User ID who owns the content
     */
    private void createVersionFromGeneratedContent(ContentGeneration saved, ContentGenerateResponse response,
            Long userId) {
        try {
            // Create version from the response with explicit user ID
            // This avoids security context issues in background jobs
            contentVersioningService.createVersionForUser(saved.getId(), response, userId);

            log.info("Version created for generated content: {} by user: {}", saved.getId(), userId);
        } catch (Exception e) {
            log.error("Error creating version for generated content: {} by user: {}", saved.getId(), userId, e);
            // Don't throw - avoid breaking content generation operation
        }
    }
}