package ai.content.auto.mapper;

import ai.content.auto.dtos.CreateTemplateCategoryRequest;
import ai.content.auto.dtos.TemplateCategoryDto;
import ai.content.auto.entity.TemplateCategory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TemplateCategoryMapper {

    public TemplateCategoryDto toDto(TemplateCategory entity) {
        if (entity == null) {
            return null;
        }

        TemplateCategoryDto dto = new TemplateCategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSlug(entity.getSlug());
        dto.setSortOrder(entity.getSortOrder());
        dto.setTemplateCount(entity.getTemplateCount());
        dto.setStatus(entity.getStatus());
        dto.setIcon(entity.getIcon());
        dto.setColor(entity.getColor());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Set parent information
        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
            dto.setParentName(entity.getParent().getName());
        }

        // Set children (without recursive mapping to avoid infinite loops)
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(entity.getChildren().stream()
                    .map(this::toDtoWithoutChildren)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public TemplateCategoryDto toDtoWithoutChildren(TemplateCategory entity) {
        if (entity == null) {
            return null;
        }

        TemplateCategoryDto dto = new TemplateCategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSlug(entity.getSlug());
        dto.setSortOrder(entity.getSortOrder());
        dto.setTemplateCount(entity.getTemplateCount());
        dto.setStatus(entity.getStatus());
        dto.setIcon(entity.getIcon());
        dto.setColor(entity.getColor());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
            dto.setParentName(entity.getParent().getName());
        }

        return dto;
    }

    public TemplateCategory toEntity(CreateTemplateCategoryRequest request, TemplateCategory parent) {
        if (request == null) {
            return null;
        }

        TemplateCategory entity = new TemplateCategory();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSlug(request.getSlug());
        entity.setParent(parent);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setIcon(request.getIcon());
        entity.setColor(request.getColor());
        entity.setStatus("ACTIVE");
        entity.setTemplateCount(0);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        return entity;
    }

    public void updateEntityFromRequest(TemplateCategory entity, CreateTemplateCategoryRequest request,
            TemplateCategory parent) {
        if (entity == null || request == null) {
            return;
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSlug(request.getSlug());
        entity.setParent(parent);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : entity.getSortOrder());
        entity.setIcon(request.getIcon());
        entity.setColor(request.getColor());
        entity.setUpdatedAt(OffsetDateTime.now());
    }

    public List<TemplateCategoryDto> toDtoList(List<TemplateCategory> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}