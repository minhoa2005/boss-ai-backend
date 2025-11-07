package ai.content.auto.service;

import ai.content.auto.dtos.CreatePresetRequest;
import ai.content.auto.dtos.UpdatePresetRequest;
import ai.content.auto.dtos.UserPresetDto;
import ai.content.auto.entity.User;
import ai.content.auto.entity.UserPreset;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.repository.UserPresetRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing user presets
 * Handles CRUD operations, sharing, import/export, and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresetService {

    private final UserPresetRepository presetRepository;
    private final SecurityUtil securityUtil;

    /**
     * Create a new preset for the current user
     */
    public UserPresetDto createPreset(CreatePresetRequest request) {
        try {
            log.info("Creating preset: {}", request.getName());

            User currentUser = securityUtil.getCurrentUser();

            // Validate preset name uniqueness for user
            validatePresetNameUnique(request.getName(), currentUser.getId());

            // Validate workspace if sharing
            validateWorkspaceSharing(request);

            return createPresetInTransaction(request, currentUser);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating preset: {}", request.getName(), e);
            throw new BusinessException("Failed to create preset");
        }
    }

    @Transactional
    private UserPresetDto createPresetInTransaction(CreatePresetRequest request, User currentUser) {
        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            presetRepository.unsetDefaultPresets(currentUser.getId());
        }

        // Create preset entity
        UserPreset preset = new UserPreset();
        preset.setUser(currentUser);
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setConfiguration(request.getConfiguration());
        preset.setCategory(request.getCategory());
        preset.setContentType(request.getContentType());
        preset.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        preset.setIsFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false);
        preset.setIsShared(request.getIsShared() != null ? request.getIsShared() : false);
        preset.setSharedWithWorkspace(
                request.getSharedWithWorkspace() != null ? request.getSharedWithWorkspace() : false);
        preset.setWorkspaceId(request.getWorkspaceId());
        preset.setTags(request.getTags());
        preset.setUsageCount(0);
        preset.setTotalUses(0);
        preset.setSuccessRate(BigDecimal.ZERO);
        preset.setCreatedAt(OffsetDateTime.now());
        preset.setUpdatedAt(OffsetDateTime.now());
        preset.setVersion(0L);

        UserPreset savedPreset = presetRepository.save(preset);

        log.info("Preset created successfully: {} (ID: {})", savedPreset.getName(), savedPreset.getId());

        return mapToDto(savedPreset);
    }

    /**
     * Update an existing preset
     */
    public UserPresetDto updatePreset(Long presetId, UpdatePresetRequest request) {
        try {
            log.info("Updating preset: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate ownership
            UserPreset preset = findPresetByIdAndValidateOwnership(presetId, currentUser.getId());

            // Validate name uniqueness if changing name
            if (request.getName() != null && !request.getName().equals(preset.getName())) {
                validatePresetNameUnique(request.getName(), currentUser.getId());
            }

            return updatePresetInTransaction(preset, request, currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating preset: {}", presetId, e);
            throw new BusinessException("Failed to update preset");
        }
    }

    @Transactional
    private UserPresetDto updatePresetInTransaction(UserPreset preset, UpdatePresetRequest request, Long userId) {
        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(preset.getIsDefault())) {
            presetRepository.unsetDefaultPresets(userId);
        }

        // Update fields
        if (request.getName() != null) {
            preset.setName(request.getName());
        }
        if (request.getDescription() != null) {
            preset.setDescription(request.getDescription());
        }
        if (request.getConfiguration() != null) {
            preset.setConfiguration(request.getConfiguration());
        }
        if (request.getCategory() != null) {
            preset.setCategory(request.getCategory());
        }
        if (request.getContentType() != null) {
            preset.setContentType(request.getContentType());
        }
        if (request.getIsDefault() != null) {
            preset.setIsDefault(request.getIsDefault());
        }
        if (request.getIsFavorite() != null) {
            preset.setIsFavorite(request.getIsFavorite());
        }
        if (request.getIsShared() != null) {
            preset.setIsShared(request.getIsShared());
        }
        if (request.getSharedWithWorkspace() != null) {
            preset.setSharedWithWorkspace(request.getSharedWithWorkspace());
        }
        if (request.getWorkspaceId() != null) {
            preset.setWorkspaceId(request.getWorkspaceId());
        }
        if (request.getTags() != null) {
            preset.setTags(request.getTags());
        }

        preset.setUpdatedAt(OffsetDateTime.now());

        UserPreset updatedPreset = presetRepository.save(preset);

        log.info("Preset updated successfully: {} (ID: {})", updatedPreset.getName(), updatedPreset.getId());

        return mapToDto(updatedPreset);
    }

    /**
     * Delete a preset
     */
    public void deletePreset(Long presetId) {
        try {
            log.info("Deleting preset: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate ownership
            UserPreset preset = findPresetByIdAndValidateOwnership(presetId, currentUser.getId());

            deletePresetInTransaction(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting preset: {}", presetId, e);
            throw new BusinessException("Failed to delete preset");
        }
    }

    @Transactional
    private void deletePresetInTransaction(UserPreset preset) {
        presetRepository.delete(preset);
        log.info("Preset deleted successfully: {} (ID: {})", preset.getName(), preset.getId());
    }

    /**
     * Get preset by ID
     */
    public UserPresetDto getPresetById(Long presetId) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            UserPreset preset = findPresetByIdAndValidateAccess(presetId, currentUser.getId());

            return mapToDto(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting preset: {}", presetId, e);
            throw new BusinessException("Failed to get preset");
        }
    }

    /**
     * Get all presets for current user
     */
    public List<UserPresetDto> getUserPresets() {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getUserPresetsInTransaction(currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user presets", e);
            throw new BusinessException("Failed to get user presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getUserPresetsInTransaction(Long userId) {
        List<UserPreset> presets = presetRepository.findByUserId(userId);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get presets with pagination
     */
    public Page<UserPresetDto> getUserPresets(int page, int size) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getUserPresetsWithPaginationInTransaction(currentUser.getId(), page, size);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user presets with pagination", e);
            throw new BusinessException("Failed to get user presets");
        }
    }

    @Transactional(readOnly = true)
    private Page<UserPresetDto> getUserPresetsWithPaginationInTransaction(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserPreset> presets = presetRepository.findByUserId(userId, pageable);
        return presets.map(this::mapToDto);
    }

    /**
     * Get presets by category
     */
    public List<UserPresetDto> getPresetsByCategory(String category) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getPresetsByCategoryInTransaction(currentUser.getId(), category);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting presets by category: {}", category, e);
            throw new BusinessException("Failed to get presets by category");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getPresetsByCategoryInTransaction(Long userId, String category) {
        List<UserPreset> presets = presetRepository.findByUserIdAndCategory(userId, category);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get presets by content type
     */
    public List<UserPresetDto> getPresetsByContentType(String contentType) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getPresetsByContentTypeInTransaction(currentUser.getId(), contentType);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting presets by content type: {}", contentType, e);
            throw new BusinessException("Failed to get presets by content type");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getPresetsByContentTypeInTransaction(Long userId, String contentType) {
        List<UserPreset> presets = presetRepository.findByUserIdAndContentType(userId, contentType);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get favorite presets
     */
    public List<UserPresetDto> getFavoritePresets() {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getFavoritePresetsInTransaction(currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting favorite presets", e);
            throw new BusinessException("Failed to get favorite presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getFavoritePresetsInTransaction(Long userId) {
        List<UserPreset> presets = presetRepository.findFavoritesByUserId(userId);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get default preset for user
     */
    public UserPresetDto getDefaultPreset() {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getDefaultPresetInTransaction(currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting default preset", e);
            throw new BusinessException("Failed to get default preset");
        }
    }

    @Transactional(readOnly = true)
    private UserPresetDto getDefaultPresetInTransaction(Long userId) {
        return presetRepository.findDefaultByUserId(userId)
                .map(this::mapToDto)
                .orElse(null);
    }

    /**
     * Search presets by name or description
     */
    public List<UserPresetDto> searchPresets(String searchTerm) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return searchPresetsInTransaction(currentUser.getId(), searchTerm);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching presets: {}", searchTerm, e);
            throw new BusinessException("Failed to search presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> searchPresetsInTransaction(Long userId, String searchTerm) {
        List<UserPreset> presets = presetRepository.searchPresets(userId, searchTerm);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get most used presets
     */
    public List<UserPresetDto> getMostUsedPresets(int limit) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getMostUsedPresetsInTransaction(currentUser.getId(), limit);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting most used presets", e);
            throw new BusinessException("Failed to get most used presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getMostUsedPresetsInTransaction(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<UserPreset> presets = presetRepository.findMostUsedByUserId(userId, pageable);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get recently used presets
     */
    public List<UserPresetDto> getRecentlyUsedPresets(int limit) {
        try {
            User currentUser = securityUtil.getCurrentUser();

            return getRecentlyUsedPresetsInTransaction(currentUser.getId(), limit);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting recently used presets", e);
            throw new BusinessException("Failed to get recently used presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getRecentlyUsedPresetsInTransaction(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<UserPreset> presets = presetRepository.findRecentlyUsedByUserId(userId, pageable);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Record preset usage
     */
    public void recordPresetUsage(Long presetId) {
        try {
            log.info("Recording preset usage: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate access
            findPresetByIdAndValidateAccess(presetId, currentUser.getId());

            recordPresetUsageInTransaction(presetId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error recording preset usage: {}", presetId, e);
            // Don't throw exception - usage tracking shouldn't break the flow
        }
    }

    @Transactional
    private void recordPresetUsageInTransaction(Long presetId) {
        presetRepository.incrementUsageCount(presetId);
        log.debug("Preset usage recorded: {}", presetId);
    }

    // Helper methods

    /**
     * Validate preset name uniqueness for user
     */
    private void validatePresetNameUnique(String name, Long userId) {
        presetRepository.findByNameAndUser_Id(name, userId)
                .ifPresent(preset -> {
                    throw new BusinessException("Preset with name '" + name + "' already exists");
                });
    }

    /**
     * Validate workspace sharing
     */
    private void validateWorkspaceSharing(CreatePresetRequest request) {
        if (Boolean.TRUE.equals(request.getSharedWithWorkspace()) && request.getWorkspaceId() == null) {
            throw new BusinessException("Workspace ID is required when sharing with workspace");
        }
    }

    /**
     * Find preset by ID and validate ownership
     */
    @Transactional(readOnly = true)
    private UserPreset findPresetByIdAndValidateOwnership(Long presetId, Long userId) {
        UserPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new BusinessException("Preset not found: " + presetId));

        if (!preset.getUser().getId().equals(userId)) {
            throw new BusinessException("You don't have permission to modify this preset");
        }

        return preset;
    }

    /**
     * Find preset by ID and validate access (ownership or shared)
     */
    @Transactional(readOnly = true)
    private UserPreset findPresetByIdAndValidateAccess(Long presetId, Long userId) {
        UserPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new BusinessException("Preset not found: " + presetId));

        // Check if user owns the preset or has access through workspace
        boolean hasAccess = preset.getUser().getId().equals(userId) ||
                (Boolean.TRUE.equals(preset.getSharedWithWorkspace()) && preset.getWorkspaceId() != null);

        if (!hasAccess) {
            throw new BusinessException("You don't have permission to access this preset");
        }

        return preset;
    }

    /**
     * Map entity to DTO
     */
    private UserPresetDto mapToDto(UserPreset preset) {
        UserPresetDto dto = new UserPresetDto();
        dto.setId(preset.getId());
        dto.setUserId(preset.getUser().getId());
        dto.setUsername(preset.getUser().getUsername());
        dto.setName(preset.getName());
        dto.setDescription(preset.getDescription());
        dto.setConfiguration(preset.getConfiguration());
        dto.setCategory(preset.getCategory());
        dto.setContentType(preset.getContentType());
        dto.setIsDefault(preset.getIsDefault());
        dto.setIsFavorite(preset.getIsFavorite());
        dto.setUsageCount(preset.getUsageCount());
        dto.setLastUsedAt(preset.getLastUsedAt());
        dto.setIsShared(preset.getIsShared());
        dto.setSharedWithWorkspace(preset.getSharedWithWorkspace());
        dto.setWorkspaceId(preset.getWorkspaceId());
        dto.setTags(preset.getTags());
        dto.setAverageGenerationTimeMs(preset.getAverageGenerationTimeMs());
        dto.setAverageQualityScore(preset.getAverageQualityScore());
        dto.setSuccessRate(preset.getSuccessRate());
        dto.setTotalUses(preset.getTotalUses());
        dto.setCreatedAt(preset.getCreatedAt());
        dto.setUpdatedAt(preset.getUpdatedAt());
        dto.setVersion(preset.getVersion());
        return dto;
    }

    /**
     * Get shared presets in workspace
     */
    public List<UserPresetDto> getSharedPresetsInWorkspace(Long workspaceId) {
        try {
            log.info("Getting shared presets in workspace: {}", workspaceId);

            // Validate user has access to workspace
            validateWorkspaceAccess(workspaceId);

            return getSharedPresetsInWorkspaceInTransaction(workspaceId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting shared presets in workspace: {}", workspaceId, e);
            throw new BusinessException("Failed to get shared presets");
        }
    }

    @Transactional(readOnly = true)
    private List<UserPresetDto> getSharedPresetsInWorkspaceInTransaction(Long workspaceId) {
        List<UserPreset> presets = presetRepository.findSharedInWorkspace(workspaceId);
        return presets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Share preset with workspace
     */
    public UserPresetDto sharePresetWithWorkspace(Long presetId, Long workspaceId) {
        try {
            log.info("Sharing preset {} with workspace {}", presetId, workspaceId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate ownership
            UserPreset preset = findPresetByIdAndValidateOwnership(presetId, currentUser.getId());

            // Validate workspace access
            validateWorkspaceAccess(workspaceId);

            return sharePresetWithWorkspaceInTransaction(preset, workspaceId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sharing preset {} with workspace {}", presetId, workspaceId, e);
            throw new BusinessException("Failed to share preset with workspace");
        }
    }

    @Transactional
    private UserPresetDto sharePresetWithWorkspaceInTransaction(UserPreset preset, Long workspaceId) {
        preset.setIsShared(true);
        preset.setSharedWithWorkspace(true);
        preset.setWorkspaceId(workspaceId);
        preset.setUpdatedAt(OffsetDateTime.now());

        UserPreset updatedPreset = presetRepository.save(preset);

        log.info("Preset {} shared with workspace {} successfully", preset.getId(), workspaceId);

        return mapToDto(updatedPreset);
    }

    /**
     * Unshare preset from workspace
     */
    public UserPresetDto unsharePresetFromWorkspace(Long presetId) {
        try {
            log.info("Unsharing preset {} from workspace", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate ownership
            UserPreset preset = findPresetByIdAndValidateOwnership(presetId, currentUser.getId());

            return unsharePresetFromWorkspaceInTransaction(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error unsharing preset {} from workspace", presetId, e);
            throw new BusinessException("Failed to unshare preset from workspace");
        }
    }

    @Transactional
    private UserPresetDto unsharePresetFromWorkspaceInTransaction(UserPreset preset) {
        preset.setIsShared(false);
        preset.setSharedWithWorkspace(false);
        preset.setWorkspaceId(null);
        preset.setUpdatedAt(OffsetDateTime.now());

        UserPreset updatedPreset = presetRepository.save(preset);

        log.info("Preset {} unshared from workspace successfully", preset.getId());

        return mapToDto(updatedPreset);
    }

    /**
     * Validate workspace access
     * TODO: Implement actual workspace membership validation when workspace service
     * is available
     */
    private void validateWorkspaceAccess(Long workspaceId) {
        if (workspaceId == null) {
            throw new BusinessException("Workspace ID is required");
        }
        // TODO: Check if current user is member of workspace
        // For now, we'll allow any workspace ID
        log.debug("Workspace access validation for workspace: {}", workspaceId);
    }

    /**
     * Export preset configuration
     */
    public Map<String, Object> exportPreset(Long presetId) {
        try {
            log.info("Exporting preset: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate access
            UserPreset preset = findPresetByIdAndValidateAccess(presetId, currentUser.getId());

            return exportPresetData(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exporting preset: {}", presetId, e);
            throw new BusinessException("Failed to export preset");
        }
    }

    /**
     * Export multiple presets
     */
    public List<Map<String, Object>> exportPresets(List<Long> presetIds) {
        try {
            log.info("Exporting {} presets", presetIds.size());

            User currentUser = securityUtil.getCurrentUser();

            return exportPresetsInTransaction(presetIds, currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exporting presets", e);
            throw new BusinessException("Failed to export presets");
        }
    }

    @Transactional(readOnly = true)
    private List<Map<String, Object>> exportPresetsInTransaction(List<Long> presetIds, Long userId) {
        return presetIds.stream()
                .map(presetId -> {
                    try {
                        UserPreset preset = findPresetByIdAndValidateAccess(presetId, userId);
                        return exportPresetData(preset);
                    } catch (Exception e) {
                        log.warn("Failed to export preset {}: {}", presetId, e.getMessage());
                        return null;
                    }
                })
                .filter(data -> data != null)
                .collect(Collectors.toList());
    }

    /**
     * Import preset from configuration
     */
    public UserPresetDto importPreset(Map<String, Object> presetData) {
        try {
            log.info("Importing preset");

            User currentUser = securityUtil.getCurrentUser();

            // Validate preset data
            validatePresetImportData(presetData);

            return importPresetInTransaction(presetData, currentUser);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error importing preset", e);
            throw new BusinessException("Failed to import preset");
        }
    }

    @Transactional
    private UserPresetDto importPresetInTransaction(Map<String, Object> presetData, User currentUser) {
        // Extract data from import
        String name = (String) presetData.get("name");
        String description = (String) presetData.get("description");
        @SuppressWarnings("unchecked")
        Map<String, Object> configuration = (Map<String, Object>) presetData.get("configuration");
        String category = (String) presetData.get("category");
        String contentType = (String) presetData.get("contentType");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) presetData.get("tags");

        // Check if preset with same name exists
        String importedName = name;
        int counter = 1;
        while (presetRepository.findByNameAndUser_Id(importedName, currentUser.getId()).isPresent()) {
            importedName = name + " (" + counter + ")";
            counter++;
        }

        // Create new preset
        UserPreset preset = new UserPreset();
        preset.setUser(currentUser);
        preset.setName(importedName);
        preset.setDescription(description);
        preset.setConfiguration(configuration);
        preset.setCategory(category);
        preset.setContentType(contentType);
        preset.setTags(tags);
        preset.setIsDefault(false);
        preset.setIsFavorite(false);
        preset.setIsShared(false);
        preset.setSharedWithWorkspace(false);
        preset.setUsageCount(0);
        preset.setTotalUses(0);
        preset.setSuccessRate(BigDecimal.ZERO);
        preset.setCreatedAt(OffsetDateTime.now());
        preset.setUpdatedAt(OffsetDateTime.now());
        preset.setVersion(0L);

        UserPreset savedPreset = presetRepository.save(preset);

        log.info("Preset imported successfully: {} (ID: {})", savedPreset.getName(), savedPreset.getId());

        return mapToDto(savedPreset);
    }

    /**
     * Import multiple presets
     */
    public List<UserPresetDto> importPresets(List<Map<String, Object>> presetsData) {
        try {
            log.info("Importing {} presets", presetsData.size());

            User currentUser = securityUtil.getCurrentUser();

            return importPresetsInTransaction(presetsData, currentUser);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error importing presets", e);
            throw new BusinessException("Failed to import presets");
        }
    }

    @Transactional
    private List<UserPresetDto> importPresetsInTransaction(List<Map<String, Object>> presetsData, User currentUser) {
        return presetsData.stream()
                .map(presetData -> {
                    try {
                        validatePresetImportData(presetData);
                        return importPresetInTransaction(presetData, currentUser);
                    } catch (Exception e) {
                        log.warn("Failed to import preset: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Export preset data to map
     */
    private Map<String, Object> exportPresetData(UserPreset preset) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("name", preset.getName());
        data.put("description", preset.getDescription());
        data.put("configuration", preset.getConfiguration());
        data.put("category", preset.getCategory());
        data.put("contentType", preset.getContentType());
        data.put("tags", preset.getTags());
        data.put("exportedAt", OffsetDateTime.now().toString());
        data.put("version", "1.0");
        return data;
    }

    /**
     * Validate preset import data
     */
    private void validatePresetImportData(Map<String, Object> presetData) {
        if (presetData == null || presetData.isEmpty()) {
            throw new BusinessException("Preset data is required");
        }

        if (!presetData.containsKey("name") || presetData.get("name") == null) {
            throw new BusinessException("Preset name is required");
        }

        if (!presetData.containsKey("configuration") || presetData.get("configuration") == null) {
            throw new BusinessException("Preset configuration is required");
        }

        // Validate configuration is a map
        if (!(presetData.get("configuration") instanceof Map)) {
            throw new BusinessException("Preset configuration must be a valid object");
        }
    }

    /**
     * Get preset usage analytics
     */
    public Map<String, Object> getPresetAnalytics(Long presetId) {
        try {
            log.info("Getting analytics for preset: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate access
            UserPreset preset = findPresetByIdAndValidateAccess(presetId, currentUser.getId());

            return getPresetAnalyticsData(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting preset analytics: {}", presetId, e);
            throw new BusinessException("Failed to get preset analytics");
        }
    }

    /**
     * Get user's overall preset analytics
     */
    public Map<String, Object> getUserPresetAnalytics() {
        try {
            log.info("Getting user preset analytics");

            User currentUser = securityUtil.getCurrentUser();

            return getUserPresetAnalyticsInTransaction(currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user preset analytics", e);
            throw new BusinessException("Failed to get user preset analytics");
        }
    }

    @Transactional(readOnly = true)
    private Map<String, Object> getUserPresetAnalyticsInTransaction(Long userId) {
        List<UserPreset> presets = presetRepository.findByUserId(userId);

        Map<String, Object> analytics = new java.util.HashMap<>();

        // Total presets
        analytics.put("totalPresets", presets.size());

        // Total usage count
        int totalUsage = presets.stream()
                .mapToInt(UserPreset::getUsageCount)
                .sum();
        analytics.put("totalUsage", totalUsage);

        // Average usage per preset
        double avgUsage = presets.isEmpty() ? 0.0 : (double) totalUsage / presets.size();
        analytics.put("averageUsagePerPreset", Math.round(avgUsage * 100.0) / 100.0);

        // Most used preset
        UserPreset mostUsed = presets.stream()
                .max((p1, p2) -> Integer.compare(p1.getUsageCount(), p2.getUsageCount()))
                .orElse(null);
        if (mostUsed != null) {
            Map<String, Object> mostUsedData = new java.util.HashMap<>();
            mostUsedData.put("id", mostUsed.getId());
            mostUsedData.put("name", mostUsed.getName());
            mostUsedData.put("usageCount", mostUsed.getUsageCount());
            analytics.put("mostUsedPreset", mostUsedData);
        }

        // Presets by category
        Map<String, Long> byCategory = presets.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(UserPreset::getCategory, Collectors.counting()));
        analytics.put("presetsByCategory", byCategory);

        // Presets by content type
        Map<String, Long> byContentType = presets.stream()
                .filter(p -> p.getContentType() != null)
                .collect(Collectors.groupingBy(UserPreset::getContentType, Collectors.counting()));
        analytics.put("presetsByContentType", byContentType);

        // Average success rate
        double avgSuccessRate = presets.stream()
                .filter(p -> p.getSuccessRate() != null)
                .mapToDouble(p -> p.getSuccessRate().doubleValue())
                .average()
                .orElse(0.0);
        analytics.put("averageSuccessRate", Math.round(avgSuccessRate * 100.0) / 100.0);

        // Favorite presets count
        long favoriteCount = presets.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsFavorite()))
                .count();
        analytics.put("favoritePresetsCount", favoriteCount);

        // Shared presets count
        long sharedCount = presets.stream()
                .filter(p -> Boolean.TRUE.equals(p.getSharedWithWorkspace()))
                .count();
        analytics.put("sharedPresetsCount", sharedCount);

        return analytics;
    }

    /**
     * Get optimization suggestions for preset
     */
    public List<String> getPresetOptimizationSuggestions(Long presetId) {
        try {
            log.info("Getting optimization suggestions for preset: {}", presetId);

            User currentUser = securityUtil.getCurrentUser();

            // Validate access
            UserPreset preset = findPresetByIdAndValidateAccess(presetId, currentUser.getId());

            return generateOptimizationSuggestions(preset);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting optimization suggestions: {}", presetId, e);
            throw new BusinessException("Failed to get optimization suggestions");
        }
    }

    /**
     * Get optimization suggestions for all user presets
     */
    public Map<Long, List<String>> getAllPresetOptimizationSuggestions() {
        try {
            log.info("Getting optimization suggestions for all presets");

            User currentUser = securityUtil.getCurrentUser();

            return getAllPresetOptimizationSuggestionsInTransaction(currentUser.getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting all optimization suggestions", e);
            throw new BusinessException("Failed to get optimization suggestions");
        }
    }

    @Transactional(readOnly = true)
    private Map<Long, List<String>> getAllPresetOptimizationSuggestionsInTransaction(Long userId) {
        List<UserPreset> presets = presetRepository.findByUserId(userId);

        return presets.stream()
                .collect(Collectors.toMap(
                        UserPreset::getId,
                        this::generateOptimizationSuggestions));
    }

    /**
     * Generate optimization suggestions for a preset
     */
    private List<String> generateOptimizationSuggestions(UserPreset preset) {
        List<String> suggestions = new java.util.ArrayList<>();

        // Low usage suggestion
        if (preset.getUsageCount() < 5 && preset.getCreatedAt().isBefore(OffsetDateTime.now().minusDays(30))) {
            suggestions.add(
                    "This preset hasn't been used much. Consider reviewing if it's still needed or update it to be more useful.");
        }

        // Low success rate suggestion
        if (preset.getSuccessRate() != null && preset.getSuccessRate().compareTo(BigDecimal.valueOf(70)) < 0
                && preset.getTotalUses() > 10) {
            suggestions
                    .add("Success rate is below 70%. Review the configuration to improve content generation quality.");
        }

        // No category suggestion
        if (preset.getCategory() == null || preset.getCategory().isEmpty()) {
            suggestions.add("Add a category to better organize your presets and make them easier to find.");
        }

        // No tags suggestion
        if (preset.getTags() == null || preset.getTags().isEmpty()) {
            suggestions.add("Add tags to improve preset discoverability and organization.");
        }

        // No description suggestion
        if (preset.getDescription() == null || preset.getDescription().isEmpty()) {
            suggestions.add("Add a description to help remember what this preset is for and when to use it.");
        }

        // High usage, not favorite suggestion
        if (preset.getUsageCount() > 20 && !Boolean.TRUE.equals(preset.getIsFavorite())) {
            suggestions.add("This preset is frequently used. Consider marking it as a favorite for quick access.");
        }

        // Workspace sharing suggestion
        if (preset.getUsageCount() > 15 && !Boolean.TRUE.equals(preset.getSharedWithWorkspace())) {
            suggestions.add("This preset is popular. Consider sharing it with your workspace to help your team.");
        }

        // Configuration complexity suggestion
        if (preset.getConfiguration() != null && preset.getConfiguration().size() > 15) {
            suggestions.add(
                    "This preset has many configuration options. Consider simplifying or splitting into multiple presets.");
        }

        // Outdated preset suggestion
        if (preset.getLastUsedAt() != null
                && preset.getLastUsedAt().isBefore(OffsetDateTime.now().minusDays(90))) {
            suggestions.add("This preset hasn't been used in over 90 days. Consider archiving or updating it.");
        }

        // Performance suggestion
        if (preset.getAverageGenerationTimeMs() != null && preset.getAverageGenerationTimeMs() > 30000) {
            suggestions.add(
                    "Average generation time is over 30 seconds. Consider optimizing the configuration for better performance.");
        }

        return suggestions;
    }

    /**
     * Get preset analytics data
     */
    private Map<String, Object> getPresetAnalyticsData(UserPreset preset) {
        Map<String, Object> analytics = new java.util.HashMap<>();

        analytics.put("id", preset.getId());
        analytics.put("name", preset.getName());
        analytics.put("usageCount", preset.getUsageCount());
        analytics.put("totalUses", preset.getTotalUses());
        analytics.put("successRate", preset.getSuccessRate());
        analytics.put("averageGenerationTimeMs", preset.getAverageGenerationTimeMs());
        analytics.put("averageQualityScore", preset.getAverageQualityScore());
        analytics.put("lastUsedAt", preset.getLastUsedAt());
        analytics.put("createdAt", preset.getCreatedAt());

        // Calculate days since creation
        if (preset.getCreatedAt() != null) {
            long daysSinceCreation = java.time.Duration.between(preset.getCreatedAt(), OffsetDateTime.now())
                    .toDays();
            analytics.put("daysSinceCreation", daysSinceCreation);

            // Calculate usage frequency (uses per day)
            double usageFrequency = daysSinceCreation > 0 ? (double) preset.getUsageCount() / daysSinceCreation : 0.0;
            analytics.put("usageFrequencyPerDay", Math.round(usageFrequency * 100.0) / 100.0);
        }

        // Calculate days since last use
        if (preset.getLastUsedAt() != null) {
            long daysSinceLastUse = java.time.Duration.between(preset.getLastUsedAt(), OffsetDateTime.now()).toDays();
            analytics.put("daysSinceLastUse", daysSinceLastUse);
        }

        // Add optimization suggestions
        analytics.put("optimizationSuggestions", generateOptimizationSuggestions(preset));

        return analytics;
    }
}
