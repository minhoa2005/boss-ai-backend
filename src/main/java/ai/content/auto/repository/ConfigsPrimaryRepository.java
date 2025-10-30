package ai.content.auto.repository;

import ai.content.auto.entity.ConfigsPrimary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigsPrimaryRepository extends JpaRepository<ConfigsPrimary, Long> {
    List<ConfigsPrimary> findByCategory(String category);
}
