package ai.content.auto.mapper;

import ai.content.auto.dtos.ConfigsPrimaryDto;
import ai.content.auto.entity.ConfigsPrimary;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface ConfigsPrimaryMapper {
  ConfigsPrimary toEntity(ConfigsPrimaryDto configsPrimaryDto);

  ConfigsPrimaryDto toDto(ConfigsPrimary configsPrimary);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ConfigsPrimary partialUpdate(
      ConfigsPrimaryDto configsPrimaryDto, @MappingTarget ConfigsPrimary configsPrimary);
}
