package ai.content.auto.mapper;

import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.entity.ContentVersion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for ContentVersion entity and DTO conversions.
 * Handles mapping between ContentVersion entity and ContentVersionDto.
 * 
 * Requirements: 1.1, 1.2
 */
@Component
public class ContentVersionMapper {

    /**
     * Convert ContentVersion entity to DTO.
     * 
     * @param entity ContentVersion entity
     * @return ContentVersionDto
     */
    public ContentVersionDto toDto(ContentVersion entity) {
        if (entity == null) {
            return null;
        }

        ContentVersionDto dto = ContentVersionDto.builder()
                .id(entity.getId())
                .contentId(entity.getContentId())
                .versionNumber(entity.getVersionNumber())
                .content(entity.getContent())
                .title(entity.getTitle())
                .generationParams(entity.getGenerationParams())
                .aiProvider(entity.getAiProvider())
                .aiModel(entity.getAiModel())
                .tokensUsed(entity.getTokensUsed())
                .generationCost(entity.getGenerationCost())
                .processingTimeMs(entity.getProcessingTimeMs())
                .readabilityScore(entity.getReadabilityScore())
                .seoScore(entity.getSeoScore())
                .qualityScore(entity.getQualityScore())
                .sentimentScore(entity.getSentimentScore())
                .wordCount(entity.getWordCount())
                .characterCount(entity.getCharacterCount())
                .industry(entity.getIndustry())
                .targetAudience(entity.getTargetAudience())
                .tone(entity.getTone())
                .language(entity.getLanguage())
                .parentVersionId(entity.getParentVersionId())
                .branchName(entity.getBranchName())
                .isExperimental(entity.getIsExperimental())
                .versionTag(entity.getVersionTag())
                .annotation(entity.getAnnotation())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Set calculated fields
        dto.setOverallScore(entity.calculateOverallScore());

        // Set username if user is loaded
        if (entity.getCreatedByUser() != null) {
            dto.setCreatedByUsername(entity.getCreatedByUser().getUsername());
        }

        return dto;
    }

    /**
     * Convert ContentVersion entity to DTO with additional metadata.
     * 
     * @param entity          ContentVersion entity
     * @param isLatestVersion Whether this is the latest version
     * @param totalVersions   Total number of versions for this content
     * @return ContentVersionDto with metadata
     */
    public ContentVersionDto toDtoWithMetadata(ContentVersion entity, Boolean isLatestVersion, Integer totalVersions) {
        ContentVersionDto dto = toDto(entity);
        if (dto != null) {
            dto.setIsLatestVersion(isLatestVersion);
            dto.setTotalVersions(totalVersions);
        }
        return dto;
    }

    /**
     * Convert list of ContentVersion entities to DTOs.
     * 
     * @param entities List of ContentVersion entities
     * @return List of ContentVersionDto
     */
    public List<ContentVersionDto> toDtoList(List<ContentVersion> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert ContentVersionDto to entity (for updates).
     * Note: This is typically used for partial updates, not full entity creation.
     * 
     * @param dto ContentVersionDto
     * @return ContentVersion entity
     */
    public ContentVersion toEntity(ContentVersionDto dto) {
        if (dto == null) {
            return null;
        }

        return ContentVersion.builder()
                .id(dto.getId())
                .contentId(dto.getContentId())
                .versionNumber(dto.getVersionNumber())
                .content(dto.getContent())
                .title(dto.getTitle())
                .generationParams(dto.getGenerationParams())
                .aiProvider(dto.getAiProvider())
                .aiModel(dto.getAiModel())
                .tokensUsed(dto.getTokensUsed())
                .generationCost(dto.getGenerationCost())
                .processingTimeMs(dto.getProcessingTimeMs())
                .readabilityScore(dto.getReadabilityScore())
                .seoScore(dto.getSeoScore())
                .qualityScore(dto.getQualityScore())
                .sentimentScore(dto.getSentimentScore())
                .wordCount(dto.getWordCount())
                .characterCount(dto.getCharacterCount())
                .industry(dto.getIndustry())
                .targetAudience(dto.getTargetAudience())
                .tone(dto.getTone())
                .language(dto.getLanguage())
                .parentVersionId(dto.getParentVersionId())
                .branchName(dto.getBranchName())
                .isExperimental(dto.getIsExperimental())
                .versionTag(dto.getVersionTag())
                .annotation(dto.getAnnotation())
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    /**
     * Create a lightweight DTO with minimal data for list views.
     * 
     * @param entity ContentVersion entity
     * @return Lightweight ContentVersionDto
     */
    public ContentVersionDto toLightweightDto(ContentVersion entity) {
        if (entity == null) {
            return null;
        }

        return ContentVersionDto.builder()
                .id(entity.getId())
                .contentId(entity.getContentId())
                .versionNumber(entity.getVersionNumber())
                .title(entity.getTitle())
                .aiProvider(entity.getAiProvider())
                .aiModel(entity.getAiModel())
                .qualityScore(entity.getQualityScore())
                .readabilityScore(entity.getReadabilityScore())
                .seoScore(entity.getSeoScore())
                .wordCount(entity.getWordCount())
                .characterCount(entity.getCharacterCount())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .overallScore(entity.calculateOverallScore())
                .build();
    }
}