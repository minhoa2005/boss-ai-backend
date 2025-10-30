package ai.content.auto.service;

import ai.content.auto.dtos.ConfigsPrimaryDto;
import ai.content.auto.dtos.ConfigsUserDto;
import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.entity.User;
import ai.content.auto.mapper.ConfigsPrimaryMapper;
import ai.content.auto.mapper.ConfigsUserMapper;
import ai.content.auto.repository.ConfigsPrimaryRepository;
import ai.content.auto.repository.ConfigsUserRepository;
import ai.content.auto.repository.UserRepository;
import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigService {
  private final ConfigsPrimaryRepository configsPrimaryRepository;
  private final ConfigsUserRepository configsUserRepository;
  private final UserRepository userRepository;
  private final ConfigsUserMapper configsUserMapper;
  private final ConfigsPrimaryMapper configsPrimaryMapper;

  public List<ConfigsPrimaryDto> getConfigsPrimaryByCategory(final String category) {
    return configsPrimaryRepository.findByCategory(category).stream()
        .map(configsPrimaryMapper::toDto)
        .toList();
  }

  public List<ConfigsUserDto> getConfigsUserByUserIdAndCategory(
      final Long userId, final String category) {
    User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("Cannot find user with id: " + userId));
    ConfigsPrimary configsPrimary = configsPrimaryRepository.findByCategory(category).get(0);
    return configsUserRepository.findByUserAndConfigsPrimary(user, configsPrimary).stream()
        .map(configsUserMapper::toDto)
        .toList();
  }
}
