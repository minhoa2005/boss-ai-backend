package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.ConfigsPrimaryDto;
import ai.content.auto.dtos.ConfigsUserDto;
import ai.content.auto.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * Get industry configurations with role-based access
     * - ADMIN: Returns all available industry configurations
     * - USER: Returns only user's selected industry configurations
     */
    @GetMapping("/industry")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getIndustryConfigs() {
        log.info("Fetching industry configurations with role-based access");

        List<ConfigsPrimaryDto> industries = configService.getConfigsByCategory("industry");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("Industry configurations retrieved successfully")
                .setData(industries);

        return ResponseEntity.ok(response);
    }

    /**
     * Get content type configurations with role-based access
     * - ADMIN: Returns all available content type configurations
     * - USER: Returns only user's selected content type configurations
     */
    @GetMapping("/content-type")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getContentTypeConfigs() {
        log.info("Fetching content type configurations with role-based access");

        List<ConfigsPrimaryDto> contentTypes = configService.getConfigsByCategory("content_type");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("Content type configurations retrieved successfully")
                .setData(contentTypes);

        return ResponseEntity.ok(response);
    }

    /**
     * Get language configurations with role-based access
     * - ADMIN: Returns all available language configurations
     * - USER: Returns only user's selected language configurations
     */
    @GetMapping("/language")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getLanguageConfigs() {
        log.info("Fetching language configurations with role-based access");

        List<ConfigsPrimaryDto> languages = configService.getConfigsByCategory("language");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("Language configurations retrieved successfully")
                .setData(languages);

        return ResponseEntity.ok(response);
    }

    /**
     * Get tone configurations with role-based access
     * - ADMIN: Returns all available tone configurations
     * - USER: Returns only user's selected tone configurations
     */
    @GetMapping("/tone")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getToneConfigs() {
        log.info("Fetching tone configurations with role-based access");

        List<ConfigsPrimaryDto> tones = configService.getConfigsByCategory("tone");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("Tone configurations retrieved successfully")
                .setData(tones);

        return ResponseEntity.ok(response);
    }

    /**
     * Get target audience configurations with role-based access
     * - ADMIN: Returns all available target audience configurations
     * - USER: Returns only user's selected target audience configurations
     */
    @GetMapping("/target-audience")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getTargetAudienceConfigs() {
        log.info("Fetching target audience configurations with role-based access");

        List<ConfigsPrimaryDto> targetAudiences = configService.getConfigsByCategory("target_audience");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("Target audience configurations retrieved successfully")
                .setData(targetAudiences);

        return ResponseEntity.ok(response);
    }

    // ========== ALL AVAILABLE CONFIGURATIONS (for selection/admin purposes)
    // ==========

    /**
     * Get all available industry configurations (admin created)
     * Returns all ConfigsPrimary data regardless of user selection - for selection
     * UI
     */
    @GetMapping("/available/industry")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getAllAvailableIndustryConfigs() {
        log.info("Fetching all available industry configurations");

        List<ConfigsPrimaryDto> industries = configService.getAllConfigsByCategory("industry");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("All available industry configurations retrieved successfully")
                .setData(industries);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available content type configurations (admin created)
     * Returns all ConfigsPrimary data regardless of user selection - for selection
     * UI
     */
    @GetMapping("/available/content-type")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getAllAvailableContentTypeConfigs() {
        log.info("Fetching all available content type configurations");

        List<ConfigsPrimaryDto> contentTypes = configService.getAllConfigsByCategory("content_type");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("All available content type configurations retrieved successfully")
                .setData(contentTypes);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available language configurations (admin created)
     * Returns all ConfigsPrimary data regardless of user selection - for selection
     * UI
     */
    @GetMapping("/available/language")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getAllAvailableLanguageConfigs() {
        log.info("Fetching all available language configurations");

        List<ConfigsPrimaryDto> languages = configService.getAllConfigsByCategory("language");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("All available language configurations retrieved successfully")
                .setData(languages);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available tone configurations (admin created)
     * Returns all ConfigsPrimary data regardless of user selection - for selection
     * UI
     */
    @GetMapping("/available/tone")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getAllAvailableToneConfigs() {
        log.info("Fetching all available tone configurations");

        List<ConfigsPrimaryDto> tones = configService.getAllConfigsByCategory("tone");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("All available tone configurations retrieved successfully")
                .setData(tones);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available target audience configurations (admin created)
     * Returns all ConfigsPrimary data regardless of user selection - for selection
     * UI
     */
    @GetMapping("/available/target-audience")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsPrimaryDto>>> getAllAvailableTargetAudienceConfigs() {
        log.info("Fetching all available target audience configurations");

        List<ConfigsPrimaryDto> targetAudiences = configService.getAllConfigsByCategory("target_audience");

        BaseResponse<List<ConfigsPrimaryDto>> response = new BaseResponse<List<ConfigsPrimaryDto>>()
                .setErrorMessage("All available target audience configurations retrieved successfully")
                .setData(targetAudiences);

        return ResponseEntity.ok(response);
    }

    // ========== USER SELECTION DETAILS (ConfigsUser data) ==========

    /**
     * Get industry selection details with role-based access
     * - ADMIN: Returns all users' industry selection details
     * - USER: Returns current user's industry selection details
     * Returns ConfigsUserDto objects that include selection metadata
     */
    @GetMapping("/user/industry")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsUserDto>>> getUserIndustrySelectionDetails() {
        log.info("Fetching industry selection details with role-based access");

        List<ConfigsUserDto> userIndustries = configService.getCurrentUserConfigsByCategory("industry");

        BaseResponse<List<ConfigsUserDto>> response = new BaseResponse<List<ConfigsUserDto>>()
                .setErrorMessage("Industry selection details retrieved successfully")
                .setData(userIndustries);

        return ResponseEntity.ok(response);
    }

    /**
     * Get content type selection details with role-based access
     * - ADMIN: Returns all users' content type selection details
     * - USER: Returns current user's content type selection details
     * Returns ConfigsUserDto objects that include selection metadata
     */
    @GetMapping("/user/content-type")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsUserDto>>> getUserContentTypeSelectionDetails() {
        log.info("Fetching content type selection details with role-based access");

        List<ConfigsUserDto> userContentTypes = configService.getCurrentUserConfigsByCategory("content_type");

        BaseResponse<List<ConfigsUserDto>> response = new BaseResponse<List<ConfigsUserDto>>()
                .setErrorMessage("Content type selection details retrieved successfully")
                .setData(userContentTypes);

        return ResponseEntity.ok(response);
    }

    /**
     * Get language selection details with role-based access
     * - ADMIN: Returns all users' language selection details
     * - USER: Returns current user's language selection details
     * Returns ConfigsUserDto objects that include selection metadata
     */
    @GetMapping("/user/language")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsUserDto>>> getUserLanguageSelectionDetails() {
        log.info("Fetching language selection details with role-based access");

        List<ConfigsUserDto> userLanguages = configService.getCurrentUserConfigsByCategory("language");

        BaseResponse<List<ConfigsUserDto>> response = new BaseResponse<List<ConfigsUserDto>>()
                .setErrorMessage("Language selection details retrieved successfully")
                .setData(userLanguages);

        return ResponseEntity.ok(response);
    }

    /**
     * Get tone selection details with role-based access
     * - ADMIN: Returns all users' tone selection details
     * - USER: Returns current user's tone selection details
     * Returns ConfigsUserDto objects that include selection metadata
     */
    @GetMapping("/user/tone")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsUserDto>>> getUserToneSelectionDetails() {
        log.info("Fetching tone selection details with role-based access");

        List<ConfigsUserDto> userTones = configService.getCurrentUserConfigsByCategory("tone");

        BaseResponse<List<ConfigsUserDto>> response = new BaseResponse<List<ConfigsUserDto>>()
                .setErrorMessage("Tone selection details retrieved successfully")
                .setData(userTones);

        return ResponseEntity.ok(response);
    }

    /**
     * Get target audience selection details with role-based access
     * - ADMIN: Returns all users' target audience selection details
     * - USER: Returns current user's target audience selection details
     * Returns ConfigsUserDto objects that include selection metadata
     */
    @GetMapping("/user/target-audience")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<ConfigsUserDto>>> getUserTargetAudienceSelectionDetails() {
        log.info("Fetching target audience selection details with role-based access");

        List<ConfigsUserDto> userTargetAudiences = configService.getCurrentUserConfigsByCategory("target_audience");

        BaseResponse<List<ConfigsUserDto>> response = new BaseResponse<List<ConfigsUserDto>>()
                .setErrorMessage("Target audience selection details retrieved successfully")
                .setData(userTargetAudiences);

        return ResponseEntity.ok(response);
    }
}
