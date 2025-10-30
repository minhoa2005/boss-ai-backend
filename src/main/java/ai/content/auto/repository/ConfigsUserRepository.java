package ai.content.auto.repository;

import ai.content.auto.entity.ConfigsPrimary;
import ai.content.auto.entity.ConfigsUser;
import ai.content.auto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigsUserRepository extends JpaRepository<ConfigsUser, Long> {
    List<ConfigsUser> findByUserAndConfigsPrimary(User user, ConfigsPrimary configsPrimary);
}
