package ai.content.auto.repository;

import ai.content.auto.entity.ContentVersionComparison;
import ai.content.auto.entity.ContentVersionComparison.TestStatus;
import ai.content.auto.entity.ContentVersionComparison.PerformanceWinner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ContentVersionComparison entity.
 * Provides data access methods for A/B testing and version comparison
 * functionality.
 * 
 * Requirements: 1.3
 */
@Repository
public interface ContentVersionComparisonRepository extends JpaRepository<ContentVersionComparison, Long> {

    /**
     * Find all comparisons for a specific content.
     * 
     * @param contentId The content ID
     * @return List of comparisons
     */
    List<ContentVersionComparison> findByContentIdOrderByComparedAtDesc(Long contentId);

    /**
     * Find all comparisons for a specific content with pagination.
     * 
     * @param contentId The content ID
     * @param pageable  Pagination information
     * @return Page of comparisons
     */
    Page<ContentVersionComparison> findByContentIdOrderByComparedAtDesc(Long contentId, Pageable pageable);

    /**
     * Find comparison between two specific versions.
     * 
     * @param contentId The content ID
     * @param versionA  Version A number
     * @param versionB  Version B number
     * @return Optional comparison
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE cvc.contentId = :contentId " +
            "AND ((cvc.versionA = :versionA AND cvc.versionB = :versionB) " +
            "OR (cvc.versionA = :versionB AND cvc.versionB = :versionA))")
    Optional<ContentVersionComparison> findByContentIdAndVersions(
            @Param("contentId") Long contentId,
            @Param("versionA") Integer versionA,
            @Param("versionB") Integer versionB);

    /**
     * Find comparisons by test status.
     * 
     * @param testStatus Test status
     * @param pageable   Pagination information
     * @return Page of comparisons
     */
    Page<ContentVersionComparison> findByTestStatusOrderByComparedAtDesc(TestStatus testStatus, Pageable pageable);

    /**
     * Find active comparisons (tests still running).
     * 
     * @param pageable Pagination information
     * @return Page of active comparisons
     */
    default Page<ContentVersionComparison> findActiveComparisons(Pageable pageable) {
        return findByTestStatusOrderByComparedAtDesc(TestStatus.ACTIVE, pageable);
    }

    /**
     * Find completed comparisons with statistical significance.
     * 
     * @param minSignificance Minimum statistical significance threshold
     * @param pageable        Pagination information
     * @return Page of significant comparisons
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE cvc.testStatus = 'COMPLETED' " +
            "AND cvc.statisticalSignificance >= :minSignificance " +
            "ORDER BY cvc.statisticalSignificance DESC, cvc.comparedAt DESC")
    Page<ContentVersionComparison> findSignificantComparisons(
            @Param("minSignificance") java.math.BigDecimal minSignificance, Pageable pageable);

    /**
     * Find comparisons created by a specific user.
     * 
     * @param comparedBy User ID
     * @param pageable   Pagination information
     * @return Page of comparisons
     */
    Page<ContentVersionComparison> findByComparedByOrderByComparedAtDesc(Long comparedBy, Pageable pageable);

    /**
     * Find comparisons within a date range.
     * 
     * @param startDate Start date
     * @param endDate   End date
     * @param pageable  Pagination information
     * @return Page of comparisons
     */
    Page<ContentVersionComparison> findByComparedAtBetweenOrderByComparedAtDesc(
            Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Find comparisons by performance winner.
     * 
     * @param performanceWinner Performance winner
     * @param pageable          Pagination information
     * @return Page of comparisons
     */
    Page<ContentVersionComparison> findByPerformanceWinnerOrderByComparedAtDesc(
            PerformanceWinner performanceWinner, Pageable pageable);

    /**
     * Count comparisons for a content.
     * 
     * @param contentId The content ID
     * @return Comparison count
     */
    long countByContentId(Long contentId);

    /**
     * Count active tests.
     * 
     * @return Active test count
     */
    long countByTestStatus(TestStatus testStatus);

    /**
     * Count comparisons created by a user.
     * 
     * @param comparedBy User ID
     * @return Comparison count
     */
    long countByComparedBy(Long comparedBy);

    /**
     * Find long-running tests (active for more than specified days).
     * 
     * @param daysThreshold Number of days threshold
     * @param pageable      Pagination information
     * @return Page of long-running tests
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE cvc.testStatus = 'ACTIVE' " +
            "AND cvc.testStartDate IS NOT NULL " +
            "AND cvc.testStartDate <= :thresholdDate " +
            "ORDER BY cvc.testStartDate ASC")
    Page<ContentVersionComparison> findLongRunningTests(
            @Param("thresholdDate") Instant thresholdDate, Pageable pageable);

    /**
     * Get comparison statistics for a content.
     * 
     * @param contentId The content ID
     * @return Comparison statistics
     */
    @Query("SELECT new map(" +
            "COUNT(cvc) as totalComparisons, " +
            "COUNT(CASE WHEN cvc.testStatus = 'COMPLETED' THEN 1 END) as completedTests, " +
            "COUNT(CASE WHEN cvc.testStatus = 'ACTIVE' THEN 1 END) as activeTests, " +
            "AVG(cvc.statisticalSignificance) as avgSignificance, " +
            "COUNT(CASE WHEN cvc.performanceWinner = 'A' THEN 1 END) as versionAWins, " +
            "COUNT(CASE WHEN cvc.performanceWinner = 'B' THEN 1 END) as versionBWins, " +
            "COUNT(CASE WHEN cvc.performanceWinner = 'TIE' THEN 1 END) as ties" +
            ") FROM ContentVersionComparison cvc WHERE cvc.contentId = :contentId")
    java.util.Map<String, Object> getComparisonStatistics(@Param("contentId") Long contentId);

    /**
     * Find best performing versions based on comparison results.
     * 
     * @param contentId The content ID
     * @return List of version numbers ordered by performance
     */
    @Query("SELECT CASE WHEN cvc.performanceWinner = 'A' THEN cvc.versionA ELSE cvc.versionB END as winningVersion, " +
            "COUNT(*) as winCount " +
            "FROM ContentVersionComparison cvc " +
            "WHERE cvc.contentId = :contentId AND cvc.performanceWinner IN ('A', 'B') " +
            "GROUP BY winningVersion " +
            "ORDER BY winCount DESC")
    List<Object[]> findBestPerformingVersions(@Param("contentId") Long contentId);

    /**
     * Find comparisons involving a specific version.
     * 
     * @param contentId     The content ID
     * @param versionNumber The version number
     * @param pageable      Pagination information
     * @return Page of comparisons
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE cvc.contentId = :contentId " +
            "AND (cvc.versionA = :versionNumber OR cvc.versionB = :versionNumber) " +
            "ORDER BY cvc.comparedAt DESC")
    Page<ContentVersionComparison> findComparisonsInvolvingVersion(
            @Param("contentId") Long contentId,
            @Param("versionNumber") Integer versionNumber,
            Pageable pageable);

    /**
     * Find comparisons with improvement above threshold.
     * 
     * @param improvementThreshold Minimum improvement percentage
     * @param pageable             Pagination information
     * @return Page of comparisons with significant improvement
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE " +
            "((cvc.versionBEngagementRate - cvc.versionAEngagementRate) / cvc.versionAEngagementRate * 100 >= :improvementThreshold "
            +
            "OR (cvc.versionBConversionRate - cvc.versionAConversionRate) / cvc.versionAConversionRate * 100 >= :improvementThreshold) "
            +
            "AND cvc.versionAEngagementRate > 0 AND cvc.versionAConversionRate > 0 " +
            "ORDER BY cvc.comparedAt DESC")
    Page<ContentVersionComparison> findComparisonsWithSignificantImprovement(
            @Param("improvementThreshold") java.math.BigDecimal improvementThreshold, Pageable pageable);

    /**
     * Delete all comparisons for a content.
     * 
     * @param contentId The content ID
     * @return Number of deleted comparisons
     */
    long deleteByContentId(Long contentId);

    /**
     * Find comparisons that need statistical analysis update.
     * 
     * @param pageable Pagination information
     * @return Page of comparisons needing analysis
     */
    @Query("SELECT cvc FROM ContentVersionComparison cvc WHERE cvc.testStatus = 'ACTIVE' " +
            "AND cvc.sampleSizeA IS NOT NULL AND cvc.sampleSizeB IS NOT NULL " +
            "AND cvc.sampleSizeA > 30 AND cvc.sampleSizeB > 30 " +
            "AND cvc.statisticalSignificance IS NULL " +
            "ORDER BY cvc.testStartDate ASC")
    Page<ContentVersionComparison> findComparisonsNeedingStatisticalAnalysis(Pageable pageable);
}