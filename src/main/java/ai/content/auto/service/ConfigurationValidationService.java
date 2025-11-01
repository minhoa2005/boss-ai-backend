package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.ConfigsPrimaryDto;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to validate and synchronize configuration data between
 * ContentConstants and database configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidationService {

    private final ConfigService configService;

    // ===== VALIDATION METHODS =====

    /**
     * Validate if a content type exists in database configurations
     */
    public boolean isValidContentType(String contentType) {
        if (StringUtil.isBlank(contentType)) {
            return false;
        }

        try {
            List<ConfigsPrimaryDto> dbContentTypes = configService.getAllConfigsByCategory("content_type");
            return dbContentTypes.stream()
                    .anyMatch(config -> StringUtil.equalsIgnoreCase(config.value(), contentType));
        } catch (Exception e) {
            log.warn("Error validating content type against database: {}", contentType, e);
            // Fallback to constants validation
            return isValidContentTypeInConstants(contentType);
        }
    }

    /**
     * Validate if a tone exists in database configurations
     */
    public boolean isValidTone(String tone) {
        if (StringUtil.isBlank(tone)) {
            return false;
        }

        try {
            List<ConfigsPrimaryDto> dbTones = configService.getAllConfigsByCategory("tone");
            return dbTones.stream()
                    .anyMatch(config -> StringUtil.equalsIgnoreCase(config.value(), tone));
        } catch (Exception e) {
            log.warn("Error validating tone against database: {}", tone, e);
            // Fallback to constants validation
            return isValidToneInConstants(tone);
        }
    }

    /**
     * Validate if a language exists in database configurations
     */
    public boolean isValidLanguage(String language) {
        if (StringUtil.isBlank(language)) {
            return false;
        }

        try {
            List<ConfigsPrimaryDto> dbLanguages = configService.getAllConfigsByCategory("language");
            return dbLanguages.stream()
                    .anyMatch(config -> StringUtil.equalsIgnoreCase(config.value(), language));
        } catch (Exception e) {
            log.warn("Error validating language against database: {}", language, e);
            // Fallback to constants validation
            return isValidLanguageInConstants(language);
        }
    }

    /**
     * Validate if an industry exists in database configurations
     */
    public boolean isValidIndustry(String industry) {
        if (StringUtil.isBlank(industry)) {
            return false;
        }

        try {
            List<ConfigsPrimaryDto> dbIndustries = configService.getAllConfigsByCategory("industry");
            return dbIndustries.stream()
                    .anyMatch(config -> StringUtil.equalsIgnoreCase(config.value(), industry));
        } catch (Exception e) {
            log.warn("Error validating industry against database: {}", industry, e);
            // No constants fallback for industry as it's purely database-driven
            return false;
        }
    }

    /**
     * Validate if a target audience exists in database configurations
     */
    public boolean isValidTargetAudience(String targetAudience) {
        if (StringUtil.isBlank(targetAudience)) {
            return false;
        }

        try {
            List<ConfigsPrimaryDto> dbTargetAudiences = configService.getAllConfigsByCategory("target_audience");
            return dbTargetAudiences.stream()
                    .anyMatch(config -> StringUtil.equalsIgnoreCase(config.value(), targetAudience));
        } catch (Exception e) {
            log.warn("Error validating target audience against database: {}", targetAudience, e);
            // No constants fallback for target audience as it's purely database-driven
            return false;
        }
    }

    // ===== NORMALIZATION METHODS =====

    /**
     * Normalize content type to match database value
     */
    public String normalizeContentType(String contentType) {
        if (StringUtil.isBlank(contentType)) {
            return null;
        }

        try {
            List<ConfigsPrimaryDto> dbContentTypes = configService.getAllConfigsByCategory("content_type");
            return dbContentTypes.stream()
                    .filter(config -> StringUtil.equalsIgnoreCase(config.value(), contentType))
                    .map(ConfigsPrimaryDto::value)
                    .findFirst()
                    .orElse(contentType); // Return original if not found
        } catch (Exception e) {
            log.warn("Error normalizing content type: {}", contentType, e);
            return contentType;
        }
    }

    /**
     * Normalize tone to match database value
     */
    public String normalizeTone(String tone) {
        if (StringUtil.isBlank(tone)) {
            return null;
        }

        try {
            List<ConfigsPrimaryDto> dbTones = configService.getAllConfigsByCategory("tone");
            return dbTones.stream()
                    .filter(config -> StringUtil.equalsIgnoreCase(config.value(), tone))
                    .map(ConfigsPrimaryDto::value)
                    .findFirst()
                    .orElse(tone); // Return original if not found
        } catch (Exception e) {
            log.warn("Error normalizing tone: {}", tone, e);
            return tone;
        }
    }

    /**
     * Normalize language to match database value
     */
    public String normalizeLanguage(String language) {
        if (StringUtil.isBlank(language)) {
            return null;
        }

        try {
            List<ConfigsPrimaryDto> dbLanguages = configService.getAllConfigsByCategory("language");
            return dbLanguages.stream()
                    .filter(config -> StringUtil.equalsIgnoreCase(config.value(), language))
                    .map(ConfigsPrimaryDto::value)
                    .findFirst()
                    .orElse(language); // Return original if not found
        } catch (Exception e) {
            log.warn("Error normalizing language: {}", language, e);
            return language;
        }
    }

    // ===== SYNCHRONIZATION VALIDATION =====

    /**
     * Validate that ContentConstants are synchronized with database
     * This should be called at application startup
     */
    public void validateConstantsSynchronization() {
        log.info("Starting configuration synchronization validation...");

        try {
            validateContentTypeSynchronization();
            validateToneSynchronization();
            validateLanguageSynchronization();

            log.info("Configuration synchronization validation completed successfully");
        } catch (Exception e) {
            log.error("Configuration synchronization validation failed", e);
            throw new BusinessException("Configuration synchronization validation failed: " + e.getMessage());
        }
    }

    private void validateContentTypeSynchronization() {
        List<ConfigsPrimaryDto> dbContentTypes = configService.getAllConfigsByCategory("content_type");
        Set<String> dbValues = dbContentTypes.stream()
                .map(ConfigsPrimaryDto::value)
                .collect(Collectors.toSet());

        // Check if constants exist in database
        checkConstantInDatabase("CONTENT_TYPE_BLOG", ContentConstants.CONTENT_TYPE_BLOG, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_ARTICLE", ContentConstants.CONTENT_TYPE_ARTICLE, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_SOCIAL", ContentConstants.CONTENT_TYPE_SOCIAL, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_FACEBOOK", ContentConstants.CONTENT_TYPE_FACEBOOK, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_INSTAGRAM", ContentConstants.CONTENT_TYPE_INSTAGRAM, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_EMAIL", ContentConstants.CONTENT_TYPE_EMAIL, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_NEWSLETTER", ContentConstants.CONTENT_TYPE_NEWSLETTER, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_PRODUCT", ContentConstants.CONTENT_TYPE_PRODUCT, dbValues);
        checkConstantInDatabase("CONTENT_TYPE_AD", ContentConstants.CONTENT_TYPE_AD, dbValues);

        log.debug("Content type synchronization validation passed");
    }

    private void validateToneSynchronization() {
        List<ConfigsPrimaryDto> dbTones = configService.getAllConfigsByCategory("tone");
        Set<String> dbValues = dbTones.stream()
                .map(ConfigsPrimaryDto::value)
                .collect(Collectors.toSet());

        // Check if constants exist in database
        checkConstantInDatabase("TONE_PROFESSIONAL", ContentConstants.TONE_PROFESSIONAL, dbValues);
        checkConstantInDatabase("TONE_FRIENDLY", ContentConstants.TONE_FRIENDLY, dbValues);
        checkConstantInDatabase("TONE_ENTHUSIASTIC", ContentConstants.TONE_ENTHUSIASTIC, dbValues);
        checkConstantInDatabase("TONE_HUMOROUS", ContentConstants.TONE_HUMOROUS, dbValues);
        checkConstantInDatabase("TONE_AUTHORITATIVE", ContentConstants.TONE_AUTHORITATIVE, dbValues);
        checkConstantInDatabase("TONE_CASUAL", ContentConstants.TONE_CASUAL, dbValues);

        log.debug("Tone synchronization validation passed");
    }

    private void validateLanguageSynchronization() {
        List<ConfigsPrimaryDto> dbLanguages = configService.getAllConfigsByCategory("language");
        Set<String> dbValues = dbLanguages.stream()
                .map(ConfigsPrimaryDto::value)
                .collect(Collectors.toSet());

        // Check if constants exist in database
        checkConstantInDatabase("LANGUAGE_VIETNAMESE", ContentConstants.LANGUAGE_VIETNAMESE, dbValues);
        checkConstantInDatabase("LANGUAGE_ENGLISH", ContentConstants.LANGUAGE_ENGLISH, dbValues);

        log.debug("Language synchronization validation passed");
    }

    private void checkConstantInDatabase(String constantName, String constantValue, Set<String> dbValues) {
        boolean found = dbValues.stream()
                .anyMatch(dbValue -> StringUtil.equalsIgnoreCase(dbValue, constantValue));

        if (!found) {
            log.warn("Constant {} with value '{}' not found in database configurations", constantName, constantValue);
            // Don't throw exception, just log warning for now
        } else {
            log.debug("Constant {} with value '{}' found in database", constantName, constantValue);
        }
    }

    // ===== FALLBACK VALIDATION (Constants-based) =====

    private boolean isValidContentTypeInConstants(String contentType) {
        return StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_BLOG) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_ARTICLE) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_SOCIAL) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_FACEBOOK) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_INSTAGRAM) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_EMAIL) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_NEWSLETTER) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_PRODUCT) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_DESCRIPTION) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_AD) ||
                StringUtil.equalsIgnoreCase(contentType, ContentConstants.CONTENT_TYPE_ADVERTISEMENT);
    }

    private boolean isValidToneInConstants(String tone) {
        return StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_PROFESSIONAL) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FORMAL) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FRIENDLY) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_CASUAL) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_ENTHUSIASTIC) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_EXCITING) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_HUMOROUS) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_FUNNY) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_AUTHORITATIVE) ||
                StringUtil.equalsIgnoreCase(tone, ContentConstants.TONE_EXPERT);
    }

    private boolean isValidLanguageInConstants(String language) {
        return StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_VIETNAMESE) ||
                StringUtil.equalsIgnoreCase(language, ContentConstants.LANGUAGE_ENGLISH);
    }
}