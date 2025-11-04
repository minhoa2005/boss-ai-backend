package ai.content.auto.mapper;

import ai.content.auto.dtos.GenerationJobDto;
import ai.content.auto.entity.GenerationJob;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper for GenerationJob entity and DTO
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GenerationJobMapper {

    /**
     * Convert entity to DTO
     */
    GenerationJobDto toDto(GenerationJob entity);

    /**
     * Convert DTO to entity
     */
    GenerationJob toEntity(GenerationJobDto dto);
}