package ai.content.auto.service;

import ai.content.auto.dto.request.CreateVideoTemplateRequest;
import ai.content.auto.dto.response.VideoTemplateResponse;
import ai.content.auto.entity.User;
import ai.content.auto.entity.VideoTemplate;
import ai.content.auto.entity.VideoTemplateUsageLog;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.VideoTemplateRepository;
import ai.content.auto.repository.VideoTemplateUsageLogRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing video templates.
 * Handles CRUD operations, template recommendations, and usage tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoTemplateService {

    private final VideoTemplateRepository videoTemplateRepository;
    private final VideoTemplateUsageLogRepository usageLogRepository;
    private final SecurityUtil securityUtil;

    /**
     * Create a new video template.
     */
    public VideoTemplateResponse createTemplate(CreateVideoTemplateRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            VideoTemplate template = buildTemplateFromRequest(request, currentUser);
            VideoTemplate savedTemplate = saveTemplateInTransaction(template);

            log.info("Video template created: {} by user: {}", savedTemplate.getName(), currentUser.getId());

            return mapToResponse(savedTemplate);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating video template", e);
            throw new BusinessException("Failed to create video template");
        }
    }

    /**
     * Get template by ID.
     */
    public VideoTemplateResponse getTemplateById(Long id) {
        VideoTemplate template = findTemplateByIdInTransaction(id);
        return mapToResponse(template);
    }

    /**
     * Get all public templates.
     */
    public List<VideoTemplateResponse> getPublicTemplates() {
        List<VideoTemplate> templates = findPublicTemplatesInTransaction();
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get templates by category.
     */
    public List<VideoTemplateResponse> getTemplatesByCategory(String category) {
        List<VideoTemplate> templates = findTemplatesByCategoryInTransaction(category);
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user's templates.
     */
    public List<VideoTemplateResponse> getUserTemplates() {
        try {
            User currentUser = securityUtil.getCurrentUser();
            List<VideoTemplate> templates = findUserTemplatesInTransaction(currentUser.getId());
            return templates.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving user templates", e);
            throw new BusinessException("Failed to retrieve user templates");
        }
    }

    /**
     * Get popular templates.
     */
    public List<VideoTemplateResponse> getPopularTemplates(int limit) {
        List<VideoTemplate> templates = findPopularTemplatesInTransaction(limit);
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recommended templates for current user.
     */
    public List<VideoTemplateResponse> getRecommendedTemplates(int limit) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            List<VideoTemplate> templates = findRecommendedTemplatesInTransaction(currentUser.getId(), limit);
            return templates.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving recommended templates", e);
            throw new BusinessException("Failed to retrieve recommended templates");
        }
    }

    /**
     * Search templates by name or description.
     */
    public List<VideoTemplateResponse> searchTemplates(String searchTerm) {
        List<VideoTemplate> templates = searchTemplatesInTransaction(searchTerm);
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update template.
     */
    public VideoTemplateResponse updateTemplate(Long id, CreateVideoTemplateRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            VideoTemplate template = findTemplateByIdInTransaction(id);

            // Check ownership
            if (!template.getCreatedBy().getId().equals(currentUser.getId()) && !template.getIsSystemTemplate()) {
                throw new BusinessException("You don't have permission to update this template");
            }

            updateTemplateFields(template, request);
            VideoTemplate updatedTemplate = saveTemplateInTransaction(template);

            log.info("Video template updated: {} by user: {}", updatedTemplate.getId(), currentUser.getId());

            return mapToResponse(updatedTemplate);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating video template: {}", id, e);
            throw new BusinessException("Failed to update video template");
        }
    }

    /**
     * Delete template.
     */
    public void deleteTemplate(Long id) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            VideoTemplate template = findTemplateByIdInTransaction(id);

            // Check ownership
            if (!template.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new BusinessException("You don't have permission to delete this template");
            }

            deleteTemplateInTransaction(id);

            log.info("Video template deleted: {} by user: {}", id, currentUser.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting video template: {}", id, e);
            throw new BusinessException("Failed to delete video template");
        }
    }

    /**
     * Record template usage.
     */
    public void recordTemplateUsage(Long templateId, Long contentId, Integer videoDuration,
            String status, Long processingTimeMs, Long fileSizeBytes, String errorMessage) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            recordUsageInTransaction(templateId, currentUser.getId(), contentId, videoDuration,
                    status, processingTimeMs, fileSizeBytes, errorMessage);

            // Update template statistics
            updateTemplateStatistics(templateId);

            log.info("Template usage recorded: template={}, user={}, status={}", templateId, currentUser.getId(),
                    status);
        } catch (Exception e) {
            log.error("Error recording template usage: {}", templateId, e);
            // Don't throw exception to avoid breaking video generation flow
        }
    }

    /**
     * Update template statistics based on usage logs.
     */
    private void updateTemplateStatistics(Long templateId) {
        try {
            VideoTemplate template = findTemplateByIdInTransaction(templateId);

            // Calculate success rate
            Long totalUses = (long) usageLogRepository.findByTemplateId(templateId).size();
            Long successfulUses = usageLogRepository.countSuccessfulUses(templateId);

            if (totalUses > 0) {
                BigDecimal successRate = BigDecimal.valueOf(successfulUses)
                        .divide(BigDecimal.valueOf(totalUses), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                template.setSuccessRate(successRate);
                template.setUsageCount(totalUses.intValue());

                saveTemplateInTransaction(template);
            }
        } catch (Exception e) {
            log.error("Error updating template statistics: {}", templateId, e);
        }
    }

    // Private helper methods

    private VideoTemplate buildTemplateFromRequest(CreateVideoTemplateRequest request, User user) {
        return VideoTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .styleName(request.getStyleName())
                .animationStyle(request.getAnimationStyle())
                .transitionStyle(request.getTransitionStyle())
                .logoUrl(request.getLogoUrl())
                .logoPosition(request.getLogoPosition())
                .primaryColor(request.getPrimaryColor())
                .secondaryColor(request.getSecondaryColor())
                .accentColor(request.getAccentColor())
                .fontFamily(request.getFontFamily())
                .fontSize(request.getFontSize())
                .defaultDuration(request.getDefaultDuration())
                .minDuration(request.getMinDuration())
                .maxDuration(request.getMaxDuration())
                .aspectRatio(request.getAspectRatio())
                .resolution(request.getResolution())
                .frameRate(request.getFrameRate())
                .videoFormat(request.getVideoFormat())
                .voiceOverEnabled(request.getVoiceOverEnabled())
                .voiceType(request.getVoiceType())
                .voiceSpeed(request.getVoiceSpeed())
                .backgroundMusicEnabled(request.getBackgroundMusicEnabled())
                .musicGenre(request.getMusicGenre())
                .musicVolume(request.getMusicVolume())
                .advancedConfig(request.getAdvancedConfig())
                .isPublic(request.getIsPublic())
                .createdBy(user)
                .build();
    }

    private void updateTemplateFields(VideoTemplate template, CreateVideoTemplateRequest request) {
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setStyleName(request.getStyleName());
        template.setAnimationStyle(request.getAnimationStyle());
        template.setTransitionStyle(request.getTransitionStyle());
        template.setLogoUrl(request.getLogoUrl());
        template.setLogoPosition(request.getLogoPosition());
        template.setPrimaryColor(request.getPrimaryColor());
        template.setSecondaryColor(request.getSecondaryColor());
        template.setAccentColor(request.getAccentColor());
        template.setFontFamily(request.getFontFamily());
        template.setFontSize(request.getFontSize());
        template.setDefaultDuration(request.getDefaultDuration());
        template.setMinDuration(request.getMinDuration());
        template.setMaxDuration(request.getMaxDuration());
        template.setAspectRatio(request.getAspectRatio());
        template.setResolution(request.getResolution());
        template.setFrameRate(request.getFrameRate());
        template.setVideoFormat(request.getVideoFormat());
        template.setVoiceOverEnabled(request.getVoiceOverEnabled());
        template.setVoiceType(request.getVoiceType());
        template.setVoiceSpeed(request.getVoiceSpeed());
        template.setBackgroundMusicEnabled(request.getBackgroundMusicEnabled());
        template.setMusicGenre(request.getMusicGenre());
        template.setMusicVolume(request.getMusicVolume());
        template.setAdvancedConfig(request.getAdvancedConfig());
        template.setIsPublic(request.getIsPublic());
    }

    private VideoTemplateResponse mapToResponse(VideoTemplate template) {
        return VideoTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .styleName(template.getStyleName())
                .animationStyle(template.getAnimationStyle())
                .transitionStyle(template.getTransitionStyle())
                .logoUrl(template.getLogoUrl())
                .logoPosition(template.getLogoPosition())
                .primaryColor(template.getPrimaryColor())
                .secondaryColor(template.getSecondaryColor())
                .accentColor(template.getAccentColor())
                .fontFamily(template.getFontFamily())
                .fontSize(template.getFontSize())
                .defaultDuration(template.getDefaultDuration())
                .minDuration(template.getMinDuration())
                .maxDuration(template.getMaxDuration())
                .aspectRatio(template.getAspectRatio())
                .resolution(template.getResolution())
                .frameRate(template.getFrameRate())
                .videoFormat(template.getVideoFormat())
                .voiceOverEnabled(template.getVoiceOverEnabled())
                .voiceType(template.getVoiceType())
                .voiceSpeed(template.getVoiceSpeed())
                .backgroundMusicEnabled(template.getBackgroundMusicEnabled())
                .musicGenre(template.getMusicGenre())
                .musicVolume(template.getMusicVolume())
                .advancedConfig(template.getAdvancedConfig())
                .isPublic(template.getIsPublic())
                .isSystemTemplate(template.getIsSystemTemplate())
                .usageCount(template.getUsageCount())
                .averageRating(template.getAverageRating())
                .successRate(template.getSuccessRate())
                .createdById(template.getCreatedBy().getId())
                .createdByUsername(template.getCreatedBy().getUsername())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    // Transactional database operations

    @Transactional
    private VideoTemplate saveTemplateInTransaction(VideoTemplate template) {
        return videoTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    private VideoTemplate findTemplateByIdInTransaction(Long id) {
        return videoTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Video template not found: " + id));
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> findPublicTemplatesInTransaction() {
        return videoTemplateRepository.findByIsPublicTrue();
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> findTemplatesByCategoryInTransaction(String category) {
        return videoTemplateRepository.findByCategoryAndIsPublicTrue(category);
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> findUserTemplatesInTransaction(Long userId) {
        return videoTemplateRepository.findByCreatedById(userId);
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> findPopularTemplatesInTransaction(int limit) {
        return videoTemplateRepository.findPopularTemplates(PageRequest.of(0, limit)).getContent();
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> findRecommendedTemplatesInTransaction(Long userId, int limit) {
        return videoTemplateRepository.findRecommendedTemplatesForUser(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    private List<VideoTemplate> searchTemplatesInTransaction(String searchTerm) {
        return videoTemplateRepository.searchTemplates(searchTerm);
    }

    @Transactional
    private void deleteTemplateInTransaction(Long id) {
        videoTemplateRepository.deleteById(id);
    }

    @Transactional
    private void recordUsageInTransaction(Long templateId, Long userId, Long contentId, Integer videoDuration,
            String status, Long processingTimeMs, Long fileSizeBytes, String errorMessage) {
        VideoTemplate template = videoTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found: " + templateId));

        User user = new User();
        user.setId(userId);

        VideoTemplateUsageLog usageLog = VideoTemplateUsageLog.builder()
                .template(template)
                .user(user)
                .videoDuration(videoDuration)
                .generationStatus(status)
                .processingTimeMs(processingTimeMs)
                .fileSizeBytes(fileSizeBytes)
                .errorMessage(errorMessage)
                .build();

        usageLogRepository.save(usageLog);
    }
}
