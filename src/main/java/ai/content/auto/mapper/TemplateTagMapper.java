package ai.content.auto.mapper;

import ai.content.auto.dtos.CreateTemplateTagRequest;
import ai.content.auto.dtos.TemplateTagDto;
import ai.content.auto.entity.TemplateTag;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TemplateTagMapper {

    public TemplateTagDto toDto(TemplateTag entity) {
        if (entity == null) {
            return null;
        }

        TemplateTagDto dto = new TemplateTagDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setDescription(entity.getDescription());
        dto.setUsageCount(entity.getUsageCount());
        dto.setStatus(entity.getStatus());
        dto.setColor(entity.getColor());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public TemplateTag toEntity(CreateTemplateTagRequest request) {
        if (request == null) {
            return null;
        }

        TemplateTag entity = new TemplateTag();
        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        entity.setDescription(request.getDescription());
        entity.setColor(request.getColor());
        entity.setUsageCount(0);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        return entity;
    }

    public void updateEntityFromRequest(TemplateTag entity, CreateTemplateTagRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        entity.setDescription(request.getDescription());
        entity.setColor(request.getColor());
        entity.setUpdatedAt(OffsetDateTime.now());
    }

    public List<TemplateTagDto> toDtoList(List<TemplateTag> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}