package ai.content.auto.repository;

import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ai.content.auto.entity.Hook;

@Repository
public interface HookRepository extends JpaRepository<Hook, Long> {
  @Query(value = """
      SELECT hook,
        (
          (CASE WHEN industry = :industry THEN 50 ELSE 0 END) +
          (CASE WHEN target_audience = :targetAudience THEN 30 ELSE 0 END) +
          (CASE WHEN tone = :tone THEN 10 ELSE 0 END) +
          (CASE WHEN content_type = :contentType THEN 5 ELSE 0 END)
        ) AS relevance_score
      FROM hooks
      WHERE industry = :industry
         OR target_audience = :targetAudience
         OR tone = :tone
         OR content_type = :contentType
      ORDER BY relevance_score DESC, hook_id ASC
      LIMIT 3
      """, nativeQuery = true)
  List<Map<String, Object>> findRelevantHooks(
      @Param("industry") String industry,
      @Param("targetAudience") String targetAudience,
      @Param("tone") String tone,
      @Param("contentType") String contentType);
}
