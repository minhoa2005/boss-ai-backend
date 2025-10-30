package ai.content.auto.mapper;

import ai.content.auto.dtos.ConfigsUserDto;
import ai.content.auto.entity.ConfigsUser;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {ConfigsPrimaryMapper.class, UserMapper.class})
public interface ConfigsUserMapper {
  ConfigsUser toEntity(ConfigsUserDto configsUserDto);

  ConfigsUserDto toDto(ConfigsUser configsUser);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ConfigsUser partialUpdate(ConfigsUserDto configsUserDto, @MappingTarget ConfigsUser configsUser);
}
