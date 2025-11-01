package ai.content.auto.mapper;

import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.entity.VUserConfig;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface VUserConfigMapper {
  VUserConfig toEntity(VUserConfigDto VUserConfigDto);

  VUserConfigDto toDto(VUserConfig VUserConfig);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  VUserConfig partialUpdate(VUserConfigDto VUserConfigDto, @MappingTarget VUserConfig VUserConfig);
}
