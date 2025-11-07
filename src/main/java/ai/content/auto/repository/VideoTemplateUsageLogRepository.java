package ai.content.auto.repository;

import ai.content.auto.entity.VideoTemplateUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for VideoTemplateUsageLog entity operations.
 */
@Repository
public interface VideoTemplateUsageLogRepository extends JpaRepository<VideoTemplateUsageLog, Long> {

    /**
     * Find usage logs for a specific template.
     */
    List<VideoTemplateUsageLog> findByTemplateId(Long templateId);

    /**
     * Find usage logs for a specific user.
     */
    List<VideoTemplateUsageLog> findByUserId(Long userId);

    /**
     * Find successful usage logs for a template.
     */
    List<VideoTemplateUsageLog> findByTemplateIdAndGenerationStatus(Long templateId, String status);

    /**
     * Count successful uses of a template.
     */
    @Query("SELECT COUNT(vtul) FROM VideoTemplateUsageLog vtul WHERE vtul.template.id = :templateId AND vtul.generationStatus = 'SUCCESS'")
    Long countSuccessfulUses(@Param("templateId") Long templateId);

    /**
     * Calculate average processing time for a template.
     */
    @Query("SELECT AVG(vtul.processingTimeMs) FROM VideoTemplateUsageLog vtul WHERE vtul.template.id = :templateId AND vtul.generationStatus = 'SUCCESS'")
    Double calculateAverageProcessingTime(@Param("templateId") Long templateId);

    /**
     * Find usage logs within a date range.
     */
    @Query("SELECT vtul FROM VideoTemplateUsageLog vtul WHERE vtul.usedAt BETWEEN :startDate AND :endDate")
    List<VideoTemplateUsageLog> findByDateRange(@Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Get template usage statistics.
     */
    @Query("SELECT vtul.template.id, COUNT(vtul), " +
            "SUM(CASE WHEN vtul.generationStatus = 'SUCCESS' THEN 1 ELSE 0 END), " +
            "AVG(vtul.processingTimeMs) " +
            "FROM VideoTemplateUsageLog vtul " +
            "WHERE vtul.usedAt >= :since " +
            "GROUP BY vtul.template.id")
    List<Object[]> getTemplateUsageStatistics(@Param("since") Instant since);
}
