package ai.content.auto.repository;

import ai.content.auto.entity.OpenaiResponseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenaiResponseLogRepository extends JpaRepository<OpenaiResponseLog, Long> {
}