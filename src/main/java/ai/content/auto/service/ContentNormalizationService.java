package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateRequest;
import ai.content.auto.dtos.ContentSaveRequest;
import ai.content.auto.dtos.ContentWorkflowRequest;
import ai.content.auto.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to normalize content request data to match database configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentNormalizationService {

    private final ConfigurationValidationService configurationValidationService;

    /**
     * Normalize ContentGenerateRequest to match database values
     */
    public ContentGenerateRequest normalizeGenerateRequest(ContentGenerateRequest request) {
        if (request == null) {
            return null;
        }

        log.debug("Normalizing ContentGenerateRequest");

        // Create a new request with normalized values
        ContentGenerateRequest normalized = new ContentGenerateRequest();
        normalized.setContent(request.getContent());
        normalized.setTitle(request.getTitle());

        // Normalize configuration values
        normalized.setContentType(normalizeValue(request.getContentType(), "content_type"));
        normalized.setTone(normalizeValue(request.getTone(), "tone"));
        normalized.setLanguage(normalizeValue(request.getLanguage(), "language"));
        normalized.setIndustry(normalizeValue(request.getIndustry(), "industry"));
        normalized.setTargetAudience(normalizeValue(request.getTargetAudience(), "target_audience"));

        logNormalizationChanges("ContentGenerateRequest", request, normalized);

        return normalized;
    }

    /**
     * Normalize ContentSaveRequest to match database values
     */
    public ContentSaveRequest normalizeSaveRequest(ContentSaveRequest request) {
        if (request == null) {
            return null;
        }

        log.debug("Normalizing ContentSaveRequest");

        // Create a new request with normalized values
        ContentSaveRequest normalized = new ContentSaveRequest();
        normalized.setGeneratedContent(request.getGeneratedContent());
        normalized.setTitle(request.getTitle());
        normalized.setPrompt(request.getPrompt());

        // Normalize configuration values
        normalized.setContentType(normalizeValue(request.getContentType(), "content_type"));
        normalized.setTone(normalizeValue(request.getTone(), "tone"));
        normalized.setLanguage(normalizeValue(request.getLanguage(), "language"));
        normalized.setIndustry(normalizeValue(request.getIndustry(), "industry"));
        normalized.setTargetAudience(normalizeValue(request.getTargetAudience(), "target_audience"));

        logNormalizationChanges("ContentSaveRequest", request, normalized);

        return normalized;
    }

    /**
     * Normalize ContentWorkflowRequest to match database values
     */
    public ContentWorkflowRequest normalizeWorkflowRequest(ContentWorkflowRequest request) {
        if (request == null) {
            return null;
        }

        log.debug("Normalizing ContentWorkflowRequest");

        // Create a new request with normalized values
        ContentWorkflowRequest normalized = new ContentWorkflowRequest();
        normalized.setGeneratedContent(request.getGeneratedContent());
        normalized.setTitle(request.getTitle());
        normalized.setPrompt(request.getPrompt());

        // Normalize configuration values
        normalized.setContentType(normalizeValue(request.getContentType(), "content_type"));
        normalized.setTone(normalizeValue(request.getTone(), "tone"));
        normalized.setLanguage(normalizeValue(request.getLanguage(), "language"));
        normalized.setIndustry(normalizeValue(request.getIndustry(), "industry"));
        normalized.setTargetAudience(normalizeValue(request.getTargetAudience(), "target_audience"));

        logNormalizationChanges("ContentWorkflowRequest", request, normalized);

        return normalized;
    }

    /**
     * Normalize a single value based on its category
     */
    private String normalizeValue(String value, String category) {
        if (StringUtil.isBlank(value)) {
            return value;
        }

        return switch (category) {
            case "content_type" -> configurationValidationService.normalizeContentType(value);
            case "tone" -> configurationValidationService.normalizeTone(value);
            case "language" -> configurationValidationService.normalizeLanguage(value);
            case "industry" -> value; // Industry normalization would need database lookup
            case "target_audience" -> value; // Target audience normalization would need database lookup
            default -> value;
        };
    }

    /**
     * Log normalization changes for debugging
     */
    private void logNormalizationChanges(String requestType, Object original, Object normalized) {
        // Only log if there are actual changes
        boolean hasChanges = false;

        if (original instanceof ContentGenerateRequest orig && normalized instanceof ContentGenerateRequest norm) {
            hasChanges = checkForChanges(orig.getContentType(), norm.getContentType(), "contentType") ||
                    checkForChanges(orig.getTone(), norm.getTone(), "tone") ||
                    checkForChanges(orig.getLanguage(), norm.getLanguage(), "language") ||
                    checkForChanges(orig.getIndustry(), norm.getIndustry(), "industry") ||
                    checkForChanges(orig.getTargetAudience(), norm.getTargetAudience(), "targetAudience");
        } else if (original instanceof ContentSaveRequest orig && normalized instanceof ContentSaveRequest norm) {
            hasChanges = checkForChanges(orig.getContentType(), norm.getContentType(), "contentType") ||
                    checkForChanges(orig.getTone(), norm.getTone(), "tone") ||
                    checkForChanges(orig.getLanguage(), norm.getLanguage(), "language") ||
                    checkForChanges(orig.getIndustry(), norm.getIndustry(), "industry") ||
                    checkForChanges(orig.getTargetAudience(), norm.getTargetAudience(), "targetAudience");
        } else if (original instanceof ContentWorkflowRequest orig
                && normalized instanceof ContentWorkflowRequest norm) {
            hasChanges = checkForChanges(orig.getContentType(), norm.getContentType(), "contentType") ||
                    checkForChanges(orig.getTone(), norm.getTone(), "tone") ||
                    checkForChanges(orig.getLanguage(), norm.getLanguage(), "language") ||
                    checkForChanges(orig.getIndustry(), norm.getIndustry(), "industry") ||
                    checkForChanges(orig.getTargetAudience(), norm.getTargetAudience(), "targetAudience");
        }

        if (hasChanges) {
            log.info("Normalized {} with configuration value changes", requestType);
        } else {
            log.debug("No normalization changes needed for {}", requestType);
        }
    }

    /**
     * Check if a field value was changed during normalization
     */
    private boolean checkForChanges(String original, String normalized, String fieldName) {
        if (!StringUtil.equalsIgnoreCase(original, normalized)) {
            log.debug("Normalized {}: '{}' -> '{}'", fieldName, original, normalized);
            return true;
        }
        return false;
    }
}