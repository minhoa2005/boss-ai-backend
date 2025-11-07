package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.CreatePresetRequest;
import ai.content.auto.dtos.UpdatePresetRequest;
import ai.content.auto.dtos.UserPresetDto;
import ai.content.auto.service.UserPresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for user preset management
 * Handles CRUD operations, sharing, import/export, and analytics
 */
@RestController
@RequestMapping("/api/v1/presets")
@RequiredArgsConstructor
@Slf4j
public class UserPresetController {

    private final UserPresetService presetService;

    /**
     * Create a new preset
     */
    @PostMapping
    public ResponseEntity<BaseResponse<UserPresetDto>> createPreset(
            @Valid @RequestBody CreatePresetRequest request) {

        log.info("Creating preset: {}", request.getName());

        UserPresetDto preset = presetService.createPreset(request);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset created successfully")
                .setData(preset);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing preset
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserPresetDto>> updatePreset(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePresetRequest request) {

        log.info("Updating preset: {}", id);

        UserPresetDto preset = presetService.updatePreset(id, request);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset updated successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a preset
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deletePreset(@PathVariable Long id) {

        log.info("Deleting preset: {}", id);

        presetService.deletePreset(id);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Preset deleted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get preset by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserPresetDto>> getPresetById(@PathVariable Long id) {

        log.info("Getting preset: {}", id);

        UserPresetDto preset = presetService.getPresetById(id);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset retrieved successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all presets for current user
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getUserPresets() {

        log.info("Getting user presets");

        List<UserPresetDto> presets = presetService.getUserPresets();

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get presets with pagination
     */
    @GetMapping("/paginated")
    public ResponseEntity<BaseResponse<Page<UserPresetDto>>> getUserPresetsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Getting user presets with pagination (page: {}, size: {})", page, size);

        Page<UserPresetDto> presets = presetService.getUserPresets(page, size);

        BaseResponse<Page<UserPresetDto>> response = new BaseResponse<Page<UserPresetDto>>()
                .setErrorMessage("Presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get presets by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getPresetsByCategory(
            @PathVariable String category) {

        log.info("Getting presets by category: {}", category);

        List<UserPresetDto> presets = presetService.getPresetsByCategory(category);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get presets by content type
     */
    @GetMapping("/content-type/{contentType}")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getPresetsByContentType(
            @PathVariable String contentType) {

        log.info("Getting presets by content type: {}", contentType);

        List<UserPresetDto> presets = presetService.getPresetsByContentType(contentType);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get favorite presets
     */
    @GetMapping("/favorites")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getFavoritePresets() {

        log.info("Getting favorite presets");

        List<UserPresetDto> presets = presetService.getFavoritePresets();

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Favorite presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get default preset
     */
    @GetMapping("/default")
    public ResponseEntity<BaseResponse<UserPresetDto>> getDefaultPreset() {

        log.info("Getting default preset");

        UserPresetDto preset = presetService.getDefaultPreset();

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Default preset retrieved successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Search presets
     */
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> searchPresets(
            @RequestParam String query) {

        log.info("Searching presets: {}", query);

        List<UserPresetDto> presets = presetService.searchPresets(query);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get most used presets
     */
    @GetMapping("/most-used")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getMostUsedPresets(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting most used presets (limit: {})", limit);

        List<UserPresetDto> presets = presetService.getMostUsedPresets(limit);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Most used presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Get recently used presets
     */
    @GetMapping("/recently-used")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getRecentlyUsedPresets(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Getting recently used presets (limit: {})", limit);

        List<UserPresetDto> presets = presetService.getRecentlyUsedPresets(limit);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Recently used presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Record preset usage
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<BaseResponse<Void>> recordPresetUsage(@PathVariable Long id) {

        log.info("Recording preset usage: {}", id);

        presetService.recordPresetUsage(id);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorMessage("Preset usage recorded successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get shared presets in workspace
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> getSharedPresetsInWorkspace(
            @PathVariable Long workspaceId) {

        log.info("Getting shared presets in workspace: {}", workspaceId);

        List<UserPresetDto> presets = presetService.getSharedPresetsInWorkspace(workspaceId);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Shared presets retrieved successfully")
                .setData(presets);

        return ResponseEntity.ok(response);
    }

    /**
     * Share preset with workspace
     */
    @PostMapping("/{id}/share/{workspaceId}")
    public ResponseEntity<BaseResponse<UserPresetDto>> sharePresetWithWorkspace(
            @PathVariable Long id,
            @PathVariable Long workspaceId) {

        log.info("Sharing preset {} with workspace {}", id, workspaceId);

        UserPresetDto preset = presetService.sharePresetWithWorkspace(id, workspaceId);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset shared with workspace successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Unshare preset from workspace
     */
    @PostMapping("/{id}/unshare")
    public ResponseEntity<BaseResponse<UserPresetDto>> unsharePresetFromWorkspace(
            @PathVariable Long id) {

        log.info("Unsharing preset {} from workspace", id);

        UserPresetDto preset = presetService.unsharePresetFromWorkspace(id);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset unshared from workspace successfully")
                .setData(preset);

        return ResponseEntity.ok(response);
    }

    /**
     * Export preset
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<BaseResponse<Map<String, Object>>> exportPreset(@PathVariable Long id) {

        log.info("Exporting preset: {}", id);

        Map<String, Object> presetData = presetService.exportPreset(id);

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("Preset exported successfully")
                .setData(presetData);

        return ResponseEntity.ok(response);
    }

    /**
     * Export multiple presets
     */
    @PostMapping("/export")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> exportPresets(
            @RequestBody List<Long> presetIds) {

        log.info("Exporting {} presets", presetIds.size());

        List<Map<String, Object>> presetsData = presetService.exportPresets(presetIds);

        BaseResponse<List<Map<String, Object>>> response = new BaseResponse<List<Map<String, Object>>>()
                .setErrorMessage("Presets exported successfully")
                .setData(presetsData);

        return ResponseEntity.ok(response);
    }

    /**
     * Import preset
     */
    @PostMapping("/import")
    public ResponseEntity<BaseResponse<UserPresetDto>> importPreset(
            @RequestBody Map<String, Object> presetData) {

        log.info("Importing preset");

        UserPresetDto preset = presetService.importPreset(presetData);

        BaseResponse<UserPresetDto> response = new BaseResponse<UserPresetDto>()
                .setErrorMessage("Preset imported successfully")
                .setData(preset);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Import multiple presets
     */
    @PostMapping("/import/batch")
    public ResponseEntity<BaseResponse<List<UserPresetDto>>> importPresets(
            @RequestBody List<Map<String, Object>> presetsData) {

        log.info("Importing {} presets", presetsData.size());

        List<UserPresetDto> presets = presetService.importPresets(presetsData);

        BaseResponse<List<UserPresetDto>> response = new BaseResponse<List<UserPresetDto>>()
                .setErrorMessage("Presets imported successfully")
                .setData(presets);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get preset analytics
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getPresetAnalytics(
            @PathVariable Long id) {

        log.info("Getting analytics for preset: {}", id);

        Map<String, Object> analytics = presetService.getPresetAnalytics(id);

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("Preset analytics retrieved successfully")
                .setData(analytics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user preset analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getUserPresetAnalytics() {

        log.info("Getting user preset analytics");

        Map<String, Object> analytics = presetService.getUserPresetAnalytics();

        BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                .setErrorMessage("User preset analytics retrieved successfully")
                .setData(analytics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get optimization suggestions for preset
     */
    @GetMapping("/{id}/suggestions")
    public ResponseEntity<BaseResponse<List<String>>> getPresetOptimizationSuggestions(
            @PathVariable Long id) {

        log.info("Getting optimization suggestions for preset: {}", id);

        List<String> suggestions = presetService.getPresetOptimizationSuggestions(id);

        BaseResponse<List<String>> response = new BaseResponse<List<String>>()
                .setErrorMessage("Optimization suggestions retrieved successfully")
                .setData(suggestions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get optimization suggestions for all presets
     */
    @GetMapping("/suggestions")
    public ResponseEntity<BaseResponse<Map<Long, List<String>>>> getAllPresetOptimizationSuggestions() {

        log.info("Getting optimization suggestions for all presets");

        Map<Long, List<String>> suggestions = presetService.getAllPresetOptimizationSuggestions();

        BaseResponse<Map<Long, List<String>>> response = new BaseResponse<Map<Long, List<String>>>()
                .setErrorMessage("Optimization suggestions retrieved successfully")
                .setData(suggestions);

        return ResponseEntity.ok(response);
    }
}
