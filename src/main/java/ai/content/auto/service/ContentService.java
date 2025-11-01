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
    private final OpenAiService openAiService;
    private final N8nService n8nService;
    private final SecurityUtil securityUtil;
    private final ConfigurationValidationService configurationValidationService;
    private final ContentNormalizationService contentNormalizationService;

    public ContentGenerateResponse generateContent(ContentGenerateRequest request) {
        try {
            // 1. Normalize input to match database values
            ContentGenerateRequest normalizedRequest = contentNormalizationService.normalizeGenerateRequest(request);

            // 2. Validate normalized input
            validateGenerateRequest(normalizedRequest);

            // 3. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Generating content for user: {}", currentUser.getId());

            // 4. Call OpenAI service with normalized request
            Map<String, Object> openaiResult = openAiService.generateContent(normalizedRequest, currentUser);

            // 5. Build response
            ContentGenerateResponse response = buildGenerateResponse(openaiResult, normalizedRequest);

            log.info("Content generated successfully for user: {}", currentUser.getId());
            return response;

        } catch (BusinessException e) {
            log.error("Business error generating content for user: {}", securityUtil.getCurrentUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error generating content for user: {}", securityUtil.getCurrentUserId(), e);
            throw new InternalServerException("Failed to generate content");
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
        if (!configurationValidationService.isValidContentType(request.getContentType())) {
            throw new BusinessException("Invalid content type: " + request.getContentType());
        }

        // Validate tone if provided
        if (StringUtil.isNotBlank(request.getTone()) &&
                !configurationValidationService.isValidTone(request.getTone())) {
            throw new BusinessException("Invalid tone: " + request.getTone());
        }

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
        if (StringUtil.isNotBlank(request.getTargetAudience()) &&
                !configurationValidationService.isValidTargetAudience(request.getTargetAudience())) {
            throw new BusinessException("Invalid target audience: " + request.getTargetAudience());
        }
    }

    private void validateSaveRequest(ContentSaveRequest request) {
        if (request == null) {
            throw new BusinessException("Save request is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }

        // Validate content type if provided
        if (StringUtil.isNotBlank(request.getContentType()) &&
                !configurationValidationService.isValidContentType(request.getContentType())) {
            throw new BusinessException("Invalid content type: " + request.getContentType());
        }

        // Validate tone if provided
        if (StringUtil.isNotBlank(request.getTone()) &&
                !configurationValidationService.isValidTone(request.getTone())) {
            throw new BusinessException("Invalid tone: " + request.getTone());
        }

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
        if (StringUtil.isNotBlank(request.getTargetAudience()) &&
                !configurationValidationService.isValidTargetAudience(request.getTargetAudience())) {
            throw new BusinessException("Invalid target audience: " + request.getTargetAudience());
        }
    }

    private void validateWorkflowRequest(ContentWorkflowRequest request) {
        if (request == null) {
            throw new BusinessException("Workflow request is required");
        }
        if (StringUtil.isBlank(request.getGeneratedContent())) {
            throw new BusinessException("Generated content is required");
        }

        // Validate content type if provided
        if (StringUtil.isNotBlank(request.getContentType()) &&
                !configurationValidationService.isValidContentType(request.getContentType())) {
            throw new BusinessException("Invalid content type: " + request.getContentType());
        }

        // Validate tone if provided
        if (StringUtil.isNotBlank(request.getTone()) &&
                !configurationValidationService.isValidTone(request.getTone())) {
            throw new BusinessException("Invalid tone: " + request.getTone());
        }

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
        if (StringUtil.isNotBlank(request.getTargetAudience()) &&
                !configurationValidationService.isValidTargetAudience(request.getTargetAudience())) {
            throw new BusinessException("Invalid target audience: " + request.getTargetAudience());
        }
    }

    // Private helper methods

    private ContentGenerateResponse buildGenerateResponse(Map<String, Object> openaiResult,
            ContentGenerateRequest request) {
        ContentGenerateResponse response = new ContentGenerateResponse();
        response.setGeneratedContent((String) openaiResult.get("generatedContent"));
        response.setWordCount((Integer) openaiResult.get("wordCount"));
        response.setCharacterCount((Integer) openaiResult.get("characterCount"));
        response.setTokensUsed((Integer) openaiResult.get("tokensUsed"));
        response.setProcessingTimeMs((Long) openaiResult.get("processingTimeMs"));
        response.setStatus((String) openaiResult.get("status"));
        response.setErrorMessage((String) openaiResult.get("errorMessage"));

        // Set title if not provided
        if (request.getTitle() != null) {
            response.setTitle(request.getTitle());
        } else {
            response.setTitle(generateTitle(response.getGeneratedContent()));
        }

        return response;
    }

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
}