package ai.content.auto.service;

import ai.content.auto.dtos.*;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.TemplateUsageLog;
import ai.content.auto.entity.User;
import ai.content.auto.entity.UserPreset;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.ContentTemplateMapper;
import ai.content.auto.mapper.TemplateUsageLogMapper;
import ai.content.auto.mapper.UserPresetMapper;
import ai.content.auto.repository.ContentTemplateRepository;
import ai.content.auto.repository.TemplateUsageLogRepository;
import ai.content.auto.repository.UserPresetRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateManagementService {

    private final ContentTemplateRepository templateRepository;
    private final UserPresetRepository presetRepository;
    private final TemplateUsageLogRepository usageLogRepository;
    private final UserRepository userRepository;
    private final ContentTemplateMapper templateMapper;
    private final UserPresetMapper presetMapper;
    private final TemplateUsageLogMapper usageLogMapper;
    private final SecurityUtil securityUtil;
    private final TemplateCategorizationService categorizationService;
    private final TemplatePopularityService popularityService;

    // ================================
    // TEMPLATE MANAGEMENT METHODS
    // ================================

    /**
     * Get templates by category and industry, ordered by usage count
     */
    public List<ContentTemplateDto> getTemplatesByCategory(String category, String industry) {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching templates by category: {} and industry: {} for user: {}", category, industry, userId);

            return getTemplatesByCategoryInTransaction(category, industry, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching templates by category: {} and industry: {} for user: {}",
                    category, industry, getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch templates");
        }
    }

    /**
     * Get templates by tag
     */
    public List<ContentTemplateDto> getTemplatesByTag(String tag) {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching templates by tag: {} for user: {}", tag, userId);

            return getTemplatesByTagInTransaction(tag, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching templates by tag: {} for user: {}", tag, getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch templates by tag");
        }
    }

    @Transactional(readOnly = true)
    private List<ContentTemplateDto> getTemplatesByTagInTransaction(String tag, Long userId) {
        List<ContentTemplate> templates = templateRepository.findByTag(tag, userId);
        return templates.stream()
                .map(templateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    private List<ContentTemplateDto> getTemplatesByCategoryInTransaction(String category, String industry,
            Long userId) {
        List<ContentTemplate> templates = templateRepository.findByCategoryAndIndustryOrderByUsageCountDesc(
                category, industry, userId);
        return templates.stream()
                .map(templateMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get recommended templates based on user preferences
     */
    public List<ContentTemplateDto> getRecommendedTemplates() {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching recommended templates for user: {}", userId);

            // Get user's content preferences (simplified - could be enhanced with ML)
            UserContentPreferences preferences = getUserPreferences(userId);

            return getRecommendedTemplatesInTransaction(userId, preferences);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching recommended templates for user: {}", getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch recommended templates");
        }
    }

    @Transactional(readOnly = true)
    private List<ContentTemplateDto> getRecommendedTemplatesInTransaction(Long userId,
            UserContentPreferences preferences) {
        List<ContentTemplate> templates = templateRepository.findRecommendedTemplates(
                userId,
                preferences.getPreferredIndustries(),
                preferences.getPreferredContentTypes(),
                preferences.getPreferredTones());

        return templates.stream()
                .map(templateMapper::toDto)
                .limit(10) // Limit to top 10 recommendations
                .collect(Collectors.toList());
    }

    /**
     * Create a new template
     */
    public ContentTemplateDto createTemplate(CreateTemplateRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Creating template: {} by user: {}", request.getName(), currentUser.getId());

            // Validate template name uniqueness for user
            validateTemplateNameUniqueness(request.getName(), currentUser.getId());

            return createTemplateInTransaction(request, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating template: {} for user: {}",
                    request.getName(), getCurrentUserId(), e);
            throw new BusinessException("Failed to create template");
        }
    }

    @Transactional
    private ContentTemplateDto createTemplateInTransaction(CreateTemplateRequest request, User currentUser) {
        ContentTemplate template = templateMapper.toEntity(request, currentUser);

        // Process tags if provided
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            // Create or get existing tags and update their usage count
            List<TemplateTagDto> tags = categorizationService.getOrCreateTags(request.getTags());
            // The tags are already processed and saved by the categorization service
        }

        ContentTemplate savedTemplate = templateRepository.save(template);

        log.info("Template created successfully: {} with ID: {}", savedTemplate.getName(), savedTemplate.getId());
        return templateMapper.toDto(savedTemplate);
    }

    /**
     * Update an existing template
     */
    public ContentTemplateDto updateTemplate(Long templateId, UpdateTemplateRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Updating template: {} by user: {}", templateId, currentUser.getId());

            return updateTemplateInTransaction(templateId, request, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating template: {} for user: {}", templateId, getCurrentUserId(), e);
            throw new BusinessException("Failed to update template");
        }
    }

    @Transactional
    private ContentTemplateDto updateTemplateInTransaction(Long templateId, UpdateTemplateRequest request,
            User currentUser) {
        ContentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found"));

        // Check if user has permission to update this template
        if (!template.getCreatedBy().getId().equals(currentUser.getId()) && !template.getIsSystemTemplate()) {
            throw new BusinessException("You don't have permission to update this template");
        }

        // Validate name uniqueness if name is being changed
        if (request.getName() != null && !request.getName().equals(template.getName())) {
            validateTemplateNameUniqueness(request.getName(), currentUser.getId());
        }

        templateMapper.updateEntityFromRequest(template, request, currentUser);
        ContentTemplate savedTemplate = templateRepository.save(template);

        log.info("Template updated successfully: {}", savedTemplate.getId());
        return templateMapper.toDto(savedTemplate);
    }

    /**
     * Get template by ID
     */
    public ContentTemplateDto getTemplateById(Long templateId) {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching template: {} for user: {}", templateId, userId);

            return getTemplateByIdInTransaction(templateId, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching template: {} for user: {}", templateId, getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch template");
        }
    }

    @Transactional(readOnly = true)
    private ContentTemplateDto getTemplateByIdInTransaction(Long templateId, Long userId) {
        ContentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found"));

        // Check if user has access to this template
        if (!template.getVisibility().equals("PUBLIC") &&
                !template.getCreatedBy().getId().equals(userId)) {
            throw new BusinessException("You don't have access to this template");
        }

        return templateMapper.toDto(template);
    }

    /**
     * Delete a template
     */
    public void deleteTemplate(Long templateId) {
        try {
            User currentUser = getCurrentUser();
            log.info("Deleting template: {} by user: {}", templateId, currentUser.getId());

            deleteTemplateInTransaction(templateId, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting template: {} for user: {}", templateId, getCurrentUserId(), e);
            throw new BusinessException("Failed to delete template");
        }
    }

    @Transactional
    private void deleteTemplateInTransaction(Long templateId, User currentUser) {
        ContentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found"));

        // Check if user has permission to delete this template
        if (!template.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BusinessException("You don't have permission to delete this template");
        }

        // System templates cannot be deleted
        if (template.getIsSystemTemplate()) {
            throw new BusinessException("System templates cannot be deleted");
        }

        // Soft delete by setting status to DELETED
        template.setStatus("DELETED");
        template.setUpdatedBy(currentUser);
        template.setUpdatedAt(OffsetDateTime.now());
        templateRepository.save(template);

        log.info("Template deleted successfully: {}", templateId);
    }

    /**
     * Get featured templates
     */
    public List<ContentTemplateDto> getFeaturedTemplates() {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching featured templates for user: {}", userId);

            return getFeaturedTemplatesInTransaction(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching featured templates for user: {}", getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch featured templates");
        }
    }

    @Transactional(readOnly = true)
    private List<ContentTemplateDto> getFeaturedTemplatesInTransaction(Long userId) {
        List<ContentTemplate> templates = templateRepository.findFeaturedTemplates(userId);
        return templates.stream()
                .map(templateMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get popular templates with pagination
     */
    public Page<ContentTemplateDto> getPopularTemplates(int page, int size) {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching popular templates for user: {} (page: {}, size: {})", userId, page, size);

            return getPopularTemplatesInTransaction(userId, page, size);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching popular templates for user: {}", getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch popular templates");
        }
    }

    @Transactional(readOnly = true)
    private Page<ContentTemplateDto> getPopularTemplatesInTransaction(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentTemplate> templates = templateRepository.findPopularTemplates(userId, pageable);
        return templates.map(templateMapper::toDto);
    }

    // ================================
    // USER PRESET MANAGEMENT METHODS
    // ================================

    /**
     * Create a new user preset
     */
    public UserPresetDto createPreset(CreatePresetRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Creating preset: {} by user: {}", request.getName(), currentUser.getId());

            // Validate preset name uniqueness for user
            validatePresetNameUniqueness(request.getName(), currentUser.getId());

            return createPresetInTransaction(request, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating preset: {} for user: {}",
                    request.getName(), getCurrentUserId(), e);
            throw new BusinessException("Failed to create preset");
        }
    }

    @Transactional
    private UserPresetDto createPresetInTransaction(CreatePresetRequest request, User currentUser) {
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            presetRepository.unsetDefaultPresets(currentUser.getId());
        }

        UserPreset preset = presetMapper.toEntity(request, currentUser);
        UserPreset savedPreset = presetRepository.save(preset);

        log.info("Preset created successfully: {} with ID: {}", savedPreset.getName(), savedPreset.getId());
        return presetMapper.toDto(savedPreset);
    }

    /**
     * Get user presets
     */
    public List<UserPresetDto> getUserPresets() {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching presets for user: {}", userId);

            return getUserPresetsInTransaction(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching presets for user: {}", getCurrentUserId(), e);
            throw new BusinessException("Failed to fetch presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getUserPresetsInTransaction(Long userId) {
        List<UserPreset> presets = presetRepository.findByUserId(userId);
        return presets.stream()
                .map(presetMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update a user preset
     */
    public UserPresetDto updatePreset(Long presetId, UpdatePresetRequest request) {
        try {
            User currentUser = getCurrentUser();
            log.info("Updating preset: {} by user: {}", presetId, currentUser.getId());

            return updatePresetInTransaction(presetId, request, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating preset: {} for user: {}", presetId, getCurrentUserId(), e);
            throw new BusinessException("Failed to update preset");
        }
    }

    @Transactional
    private UserPresetDto updatePresetInTransaction(Long presetId, UpdatePresetRequest request, User currentUser) {
        UserPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new BusinessException("Preset not found"));

        // Check if user owns this preset
        if (!preset.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException("You don't have permission to update this preset");
        }

        // Validate name uniqueness if name is being changed
        if (request.getName() != null && !request.getName().equals(preset.getName())) {
            validatePresetNameUniqueness(request.getName(), currentUser.getId());
        }

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            presetRepository.unsetDefaultPresets(currentUser.getId());
        }

        presetMapper.updateEntityFromRequest(preset, request);
        UserPreset savedPreset = presetRepository.save(preset);

        log.info("Preset updated successfully: {}", savedPreset.getId());
        return presetMapper.toDto(savedPreset);
    }

    /**
     * Delete a user preset
     */
    public void deletePreset(Long presetId) {
        try {
            User currentUser = getCurrentUser();
            log.info("Deleting preset: {} by user: {}", presetId, currentUser.getId());

            deletePresetInTransaction(presetId, currentUser);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting preset: {} for user: {}", presetId, getCurrentUserId(), e);
            throw new BusinessException("Failed to delete preset");
        }
    }

    @Transactional
    private void deletePresetInTransaction(Long presetId, User currentUser) {
        UserPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new BusinessException("Preset not found"));

        // Check if user owns this preset
        if (!preset.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException("You don't have permission to delete this preset");
        }

        presetRepository.delete(preset);
        log.info("Preset deleted successfully: {}", presetId);
    }

    // ================================
    // TEMPLATE APPLICATION METHODS
    // ================================

    /**
     * Apply template to generate content request
     */
    public ContentGenerateRequest applyTemplate(Long templateId, Map<String, Object> customParams) {
        try {
            Long userId = getCurrentUserId();
            log.info("Applying template: {} with custom params for user: {}", templateId, userId);

            return applyTemplateInTransaction(templateId, customParams, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error applying template: {} for user: {}", templateId, getCurrentUserId(), e);
            throw new BusinessException("Failed to apply template");
        }
    }

    @Transactional
    private ContentGenerateRequest applyTemplateInTransaction(Long templateId, Map<String, Object> customParams,
            Long userId) {
        ContentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found"));

        // Check if user has access to this template
        if (!template.getVisibility().equals("PUBLIC") &&
                !template.getCreatedBy().getId().equals(userId)) {
            throw new BusinessException("You don't have access to this template");
        }

        // Record template usage
        recordTemplateUsage(template, userId);

        // Merge template params with custom params
        Map<String, Object> mergedParams = template.getDefaultParams();
        if (customParams != null) {
            mergedParams.putAll(customParams);
        }

        // Build content generation request
        ContentGenerateRequest request = new ContentGenerateRequest();
        request.setContent(template.getPromptTemplate());
        request.setIndustry(template.getIndustry());
        request.setContentType(template.getContentType());
        request.setTargetAudience(template.getTargetAudience());
        request.setTone(template.getTone());
        request.setLanguage(template.getLanguage());
        // Set other parameters from mergedParams as needed

        log.info("Template applied successfully: {}", templateId);
        return request;
    }

    // ================================
    // USAGE TRACKING METHODS
    // ================================

    /**
     * Record template usage for analytics
     */
    @Transactional
    private void recordTemplateUsage(ContentTemplate template, Long userId) {
        try {
            // Increment usage count
            template.setUsageCount(template.getUsageCount() + 1);
            templateRepository.save(template);

            // Create usage log entry (simplified - could be enhanced with more details)
            TemplateUsageLog usageLog = new TemplateUsageLog();
            usageLog.setTemplate(template);
            usageLog.setUser(userRepository.findById(userId).orElse(null));
            usageLog.setUsageType("GENERATION");
            usageLog.setUsageSource("WEB");
            usageLog.setUsedAt(OffsetDateTime.now());
            usageLog.setCreatedAt(OffsetDateTime.now());

            usageLogRepository.save(usageLog);

            log.debug("Template usage recorded for template: {} by user: {}", template.getId(), userId);

            // Update template metrics asynchronously (every 10 uses)
            if (template.getUsageCount() % 10 == 0) {
                try {
                    popularityService.updateTemplateMetrics(template.getId());
                } catch (Exception e) {
                    log.warn("Failed to update template metrics after usage: {}", template.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to record template usage for template: {} by user: {}", template.getId(), userId, e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    /**
     * Get template analytics and performance data
     */
    public TemplatePerformanceSummary getTemplateAnalytics(Long templateId) {
        try {
            Long userId = getCurrentUserId();
            log.info("Fetching template analytics for template: {} by user: {}", templateId, userId);

            // Verify user has access to this template
            getTemplateByIdInTransaction(templateId, userId);

            return popularityService.getTemplatePerformance(templateId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching template analytics for template: {}", templateId, e);
            throw new BusinessException("Failed to fetch template analytics");
        }
    }

    // ================================
    // HELPER METHODS
    // ================================

    private User getCurrentUser() {
        return securityUtil.getCurrentUser();
    }

    private Long getCurrentUserId() {
        return securityUtil.getCurrentUser().getId();
    }

    private void validateTemplateNameUniqueness(String name, Long userId) {
        if (templateRepository.findByNameAndCreatedBy_Id(name, userId).isPresent()) {
            throw new BusinessException("Template with this name already exists");
        }
    }

    private void validatePresetNameUniqueness(String name, Long userId) {
        if (presetRepository.findByNameAndUser_Id(name, userId).isPresent()) {
            throw new BusinessException("Preset with this name already exists");
        }
    }

    /**
     * Get user content preferences (simplified implementation)
     * In a real implementation, this could analyze user's content generation
     * history
     */
    private UserContentPreferences getUserPreferences(Long userId) {
        // Simplified implementation - could be enhanced with ML-based recommendations
        UserContentPreferences preferences = new UserContentPreferences();
        preferences.setPreferredIndustries(List.of("Technology", "Marketing", "Business"));
        preferences.setPreferredContentTypes(List.of("Blog Post", "Social Media", "Email"));
        preferences.setPreferredTones(List.of("Professional", "Friendly", "Informative"));
        return preferences;
    }

    // Inner class for user preferences (could be moved to separate file)
    private static class UserContentPreferences {
        private List<String> preferredIndustries;
        private List<String> preferredContentTypes;
        private List<String> preferredTones;

        public List<String> getPreferredIndustries() {
            return preferredIndustries;
        }

        public void setPreferredIndustries(List<String> preferredIndustries) {
            this.preferredIndustries = preferredIndustries;
        }

        public List<String> getPreferredContentTypes() {
            return preferredContentTypes;
        }

        public void setPreferredContentTypes(List<String> preferredContentTypes) {
            this.preferredContentTypes = preferredContentTypes;
        }

        public List<String> getPreferredTones() {
            return preferredTones;
        }

        public void setPreferredTones(List<String> preferredTones) {
            this.preferredTones = preferredTones;
        }
    }
}