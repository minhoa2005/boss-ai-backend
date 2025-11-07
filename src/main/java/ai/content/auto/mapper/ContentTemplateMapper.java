package ai.content.auto.mapper;

import ai.content.auto.dtos.ContentTemplateDto;
import ai.content.auto.dtos.CreateTemplateRequest;
import ai.content.auto.dtos.UpdateTemplateRequest;
import ai.content.auto.entity.ContentTemplate;
import ai.content.auto.entity.TemplateCategory;
import ai.content.auto.entity.User;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Component
public class ContentTemplateMapper {

    public ContentTemplateDto toDto(ContentTemplate entity) {
        if (entity == null) {
            return null;
        }

        ContentTemplateDto dto = new ContentTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setPromptTemplate(entity.getPromptTemplate());
        dto.setDefaultParams(entity.getDefaultParams());
        dto.setIndustry(entity.getIndustry());
        dto.setContentType(entity.getContentType());
        dto.setTargetAudience(entity.getTargetAudience());
        dto.setTone(entity.getTone());
        dto.setLanguage(entity.getLanguage());
        dto.setUsageCount(entity.getUsageCount());
        dto.setAverageRating(entity.getAverageRating());
        dto.setSuccessRate(entity.getSuccessRate());
        dto.setTotalRatings(entity.getTotalRatings());
        dto.setStatus(entity.getStatus());
        dto.setVisibility(entity.getVisibility());
        dto.setIsSystemTemplate(entity.getIsSystemTemplate());
        dto.setIsFeatured(entity.getIsFeatured());
        dto.setTags(entity.getTags());
        dto.setRequiredFields(entity.getRequiredFields());
        dto.setOptionalFields(entity.getOptionalFields());
        dto.setValidationRules(entity.getValidationRules());
        dto.setAverageGenerationTimeMs(entity.getAverageGenerationTimeMs());
        dto.setAverageTokensUsed(entity.getAverageTokensUsed());
        dto.setAverageCost(entity.getAverageCost());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setVersion(entity.getVersion());

        // Map category information
        if (entity.getTemplateCategory() != null) {
            dto.setCategoryId(entity.getTemplateCategory().getId());
            dto.setCategoryName(entity.getTemplateCategory().getName());
            dto.setCategorySlug(entity.getTemplateCategory().getSlug());
        }

        // Map user information
        if (entity.getCreatedBy() != null) {
            dto.setCreatedById(entity.getCreatedBy().getId());
            dto.setCreatedByUsername(entity.getCreatedBy().getUsername());
        }
        if (entity.getUpdatedBy() != null) {
            dto.setUpdatedById(entity.getUpdatedBy().getId());
            dto.setUpdatedByUsername(entity.getUpdatedBy().getUsername());
        }

        return dto;
    }

    public ContentTemplate toEntity(CreateTemplateRequest request, User createdBy) {
        if (request == null) {
            return null;
        }

        ContentTemplate entity = new ContentTemplate();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setPromptTemplate(request.getPromptTemplate());
        entity.setDefaultParams(request.getDefaultParams());
        entity.setIndustry(request.getIndustry());
        entity.setContentType(request.getContentType());
        entity.setTargetAudience(request.getTargetAudience());
        entity.setTone(request.getTone());
        entity.setLanguage(request.getLanguage() != null ? request.getLanguage() : "vi");
        entity.setVisibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE");
        entity.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        entity.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        entity.setRequiredFields(request.getRequiredFields() != null ? request.getRequiredFields() : new ArrayList<>());
        entity.setOptionalFields(request.getOptionalFields() != null ? request.getOptionalFields() : new ArrayList<>());
        entity.setValidationRules(request.getValidationRules());

        // Set default values
        entity.setUsageCount(0);
        entity.setTotalRatings(0);
        entity.setStatus("ACTIVE");
        entity.setIsSystemTemplate(false);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setVersion(0L);

        return entity;
    }

    public void updateEntityFromRequest(ContentTemplate entity, UpdateTemplateRequest request, User updatedBy) {
        if (request == null || entity == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getPromptTemplate() != null) {
            entity.setPromptTemplate(request.getPromptTemplate());
        }
        if (request.getDefaultParams() != null) {
            entity.setDefaultParams(request.getDefaultParams());
        }
        if (request.getIndustry() != null) {
            entity.setIndustry(request.getIndustry());
        }
        if (request.getContentType() != null) {
            entity.setContentType(request.getContentType());
        }
        if (request.getTargetAudience() != null) {
            entity.setTargetAudience(request.getTargetAudience());
        }
        if (request.getTone() != null) {
            entity.setTone(request.getTone());
        }
        if (request.getLanguage() != null) {
            entity.setLanguage(request.getLanguage());
        }
        if (request.getVisibility() != null) {
            entity.setVisibility(request.getVisibility());
        }
        if (request.getIsFeatured() != null) {
            entity.setIsFeatured(request.getIsFeatured());
        }
        if (request.getTags() != null) {
            entity.setTags(request.getTags());
        }
        if (request.getRequiredFields() != null) {
            entity.setRequiredFields(request.getRequiredFields());
        }
        if (request.getOptionalFields() != null) {
            entity.setOptionalFields(request.getOptionalFields());
        }
        if (request.getValidationRules() != null) {
            entity.setValidationRules(request.getValidationRules());
        }

        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(OffsetDateTime.now());
    }
}