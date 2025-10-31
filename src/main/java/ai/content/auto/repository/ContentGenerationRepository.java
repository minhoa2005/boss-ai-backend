package ai.content.auto.repository;

import ai.content.auto.entity.ContentGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentGenerationRepository extends JpaRepository<ContentGeneration, Long> {
}