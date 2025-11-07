package ai.content.auto.repository;

import ai.content.auto.entity.TemplateUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TemplateUsageLogRepository extends JpaRepository<TemplateUsageLog, Long> {

        /**
         * Find usage logs by template ID
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE tul.template.id = :templateId ORDER BY tul.usedAt DESC")
        Page<TemplateUsageLog> findByTemplateId(@Param("templateId") Long templateId, Pageable pageable);

        /**
         * Find usage logs by user ID
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE tul.user.id = :userId ORDER BY tul.usedAt DESC")
        Page<TemplateUsageLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

        /**
         * Find usage logs by template and user
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.user.id = :userId " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findByTemplateIdAndUserId(@Param("templateId") Long templateId,
                        @Param("userId") Long userId);

        /**
         * Count usage by template in date range
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate")
        Long countUsageByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Get average generation time for template
         */
        @Query("SELECT AVG(tul.generationTimeMs) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.generationTimeMs IS NOT NULL")
        Double getAverageGenerationTimeByTemplate(@Param("templateId") Long templateId);

        /**
         * Get average quality score for template
         */
        @Query("SELECT AVG(tul.qualityScore) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.qualityScore IS NOT NULL")
        Double getAverageQualityScoreByTemplate(@Param("templateId") Long templateId);

        /**
         * Get success rate for template
         */
        @Query("SELECT " +
                        "CAST(SUM(CASE WHEN tul.wasSuccessful = true THEN 1 ELSE 0 END) AS double) / COUNT(tul) * 100 "
                        +
                        "FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.wasSuccessful IS NOT NULL")
        Double getSuccessRateByTemplate(@Param("templateId") Long templateId);

        /**
         * Find most popular templates by usage count
         */
        @Query("SELECT tul.template.id, COUNT(tul) as usageCount FROM TemplateUsageLog tul " +
                        "WHERE tul.usedAt >= :startDate " +
                        "GROUP BY tul.template.id " +
                        "ORDER BY usageCount DESC")
        List<Object[]> findMostPopularTemplates(@Param("startDate") OffsetDateTime startDate, Pageable pageable);

        /**
         * Find usage statistics by user in date range
         */
        @Query("SELECT " +
                        "COUNT(tul) as totalUsage, " +
                        "AVG(tul.generationTimeMs) as avgGenerationTime, " +
                        "AVG(tul.qualityScore) as avgQualityScore, " +
                        "SUM(tul.generationCost) as totalCost " +
                        "FROM TemplateUsageLog tul WHERE " +
                        "tul.user.id = :userId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate")
        Object[] getUserUsageStatistics(
                        @Param("userId") Long userId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Find recent usage logs for analytics
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.usedAt >= :startDate " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findRecentUsage(@Param("startDate") OffsetDateTime startDate);

        /**
         * Find failed usage attempts for troubleshooting
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.wasSuccessful = false AND " +
                        "tul.usedAt >= :startDate " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findFailedUsage(@Param("startDate") OffsetDateTime startDate);

        /**
         * Get template usage trends by day
         */
        @Query("SELECT " +
                        "DATE(tul.usedAt) as usageDate, " +
                        "COUNT(tul) as usageCount " +
                        "FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt >= :startDate " +
                        "GROUP BY DATE(tul.usedAt) " +
                        "ORDER BY usageDate")
        List<Object[]> getUsageTrendsByTemplate(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate);

        /**
         * Find usage logs by user ID and date range
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.user.id = :userId AND " +
                        "tul.usedAt >= :since " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findByUserIdAndUsedAtAfter(
                        @Param("userId") Long userId,
                        @Param("since") OffsetDateTime since);

        /**
         * Find usage logs after a specific date
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.usedAt >= :since " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findByUsedAtAfter(@Param("since") OffsetDateTime since);

        /**
         * Count successful usages by template and user list
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.user.id IN :userIds AND " +
                        "tul.wasSuccessful = :wasSuccessful")
        Long countByTemplateIdAndUserIdInAndWasSuccessful(
                        @Param("templateId") Long templateId,
                        @Param("userIds") List<Long> userIds,
                        @Param("wasSuccessful") Boolean wasSuccessful);

        /**
         * Count total usage by template ID
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE tul.template.id = :templateId")
        Long countByTemplateId(@Param("templateId") Long templateId);

        /**
         * Count usage by template ID and success status
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.wasSuccessful = :wasSuccessful")
        Long countByTemplateIdAndWasSuccessful(
                        @Param("templateId") Long templateId,
                        @Param("wasSuccessful") Boolean wasSuccessful);

        /**
         * Find usage logs by template ID and success status
         */
        @Query("SELECT tul FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.wasSuccessful = :wasSuccessful " +
                        "ORDER BY tul.usedAt DESC")
        List<TemplateUsageLog> findByTemplateIdAndWasSuccessful(
                        @Param("templateId") Long templateId,
                        @Param("wasSuccessful") Boolean wasSuccessful);

        /**
         * Get rating distribution by template
         */
        @Query("SELECT tul.userRating, COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.userRating IS NOT NULL " +
                        "GROUP BY tul.userRating")
        List<Object[]> getRatingDistributionByTemplate(@Param("templateId") Long templateId);

        /**
         * Count successful usage by template in date range
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate AND " +
                        "tul.wasSuccessful = :wasSuccessful")
        Long countSuccessfulUsageByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate,
                        @Param("wasSuccessful") Boolean wasSuccessful);

        /**
         * Get average rating by template in date range
         */
        @Query("SELECT AVG(tul.userRating) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate AND " +
                        "tul.userRating IS NOT NULL")
        Double getAverageRatingByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Get average quality by template in date range
         */
        @Query("SELECT AVG(tul.qualityScore) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate AND " +
                        "tul.qualityScore IS NOT NULL")
        Double getAverageQualityByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Get average generation time by template in date range
         */
        @Query("SELECT AVG(tul.generationTimeMs) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate AND " +
                        "tul.generationTimeMs IS NOT NULL")
        Double getAverageGenerationTimeByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Count completed usage by template in date range (using wasSuccessful as
         * proxy)
         */
        @Query("SELECT COUNT(tul) FROM TemplateUsageLog tul WHERE " +
                        "tul.template.id = :templateId AND " +
                        "tul.usedAt BETWEEN :startDate AND :endDate AND " +
                        "tul.wasSuccessful = :wasCompleted")
        Long countCompletedUsageByTemplateInDateRange(
                        @Param("templateId") Long templateId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate,
                        @Param("wasCompleted") Boolean wasCompleted);
}