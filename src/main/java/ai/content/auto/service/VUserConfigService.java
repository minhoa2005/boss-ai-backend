package ai.content.auto.service;

import ai.content.auto.constants.ContentConstants;
import ai.content.auto.dtos.VUserConfigDto;
import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.mapper.VUserConfigMapper;
import ai.content.auto.repository.ConfigsPrimaryRepository;
import ai.content.auto.repository.ConfigsUserRepository;
import ai.content.auto.repository.VUserConfigRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VUserConfigService {
  private final VUserConfigRepository vUserConfigRepository;
  private final ConfigsUserRepository configsUserRepository;
  private final ConfigsPrimaryRepository configsPrimaryRepository;
  private final VUserConfigMapper vUserConfigMapper;
  private final SecurityUtil securityUtil;

  public List<VUserConfigDto> findAllByCategory(String category) {
    Long userId = securityUtil.getCurrentUserId();
    return switch (category) {
      case ContentConstants.CATEGORY_TARGET_AUDIENCE ->
          vUserConfigRepository.findAll().stream()
              .filter(
                  x ->
                      x.getUserId().equals(userId)
                          && x.getCategory()
                              .equalsIgnoreCase(ContentConstants.CATEGORY_TARGET_AUDIENCE))
              .map(vUserConfigMapper::toDto)
              .toList();
      case ContentConstants.CATEGORY_LANGUAGE ->
          vUserConfigRepository.findAll().stream()
              .filter(
                  x ->
                      x.getUserId().equals(userId)
                          && x.getCategory().equalsIgnoreCase(ContentConstants.CATEGORY_LANGUAGE))
              .map(vUserConfigMapper::toDto)
              .toList();
      case ContentConstants.CATEGORY_INDUSTRY ->
          vUserConfigRepository.findAll().stream()
              .filter(
                  x ->
                      x.getUserId().equals(userId)
                          && x.getCategory().equalsIgnoreCase(ContentConstants.CATEGORY_INDUSTRY))
              .map(vUserConfigMapper::toDto)
              .toList();
      case ContentConstants.CATEGORY_TONE ->
          vUserConfigRepository.findAll().stream()
              .filter(
                  x ->
                      x.getUserId().equals(userId)
                          && x.getCategory().equalsIgnoreCase(ContentConstants.CATEGORY_TONE))
              .map(vUserConfigMapper::toDto)
              .toList();
      case ContentConstants.CATEGORY_CONTENT_TYPE ->
          vUserConfigRepository.findAll().stream()
              .filter(
                  x ->
                      x.getUserId().equals(userId)
                          && x.getCategory()
                              .equalsIgnoreCase(ContentConstants.CATEGORY_CONTENT_TYPE))
              .map(vUserConfigMapper::toDto)
              .toList();
      default -> new ArrayList<>();
    };
  }

  public void UpdateConfig(VUserConfigDto vUserConfigDto) {
    Boolean isSelected = vUserConfigDto.isSelected();
    ConfigsPrimary configsPrimary =
        configsPrimaryRepository.findById(vUserConfigDto.id()).orElse(null);
    if (isSelected) {
      ConfigsUser configsUser = new ConfigsUser();
      configsUser.setConfigsPrimary(configsPrimary);
      configsUser.setUser(securityUtil.getCurrentUser());
      configsUserRepository.save(configsUser);
    } else {
      ConfigsUser configsUser =
          configsUserRepository.findConfigsUserByUserAndConfigsPrimary(
              securityUtil.getCurrentUser(), configsPrimary);
      if (configsUser != null) {
        configsUserRepository.delete(configsUser);
      }
    }
  }
}
