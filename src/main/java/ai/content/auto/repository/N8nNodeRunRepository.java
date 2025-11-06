package ai.content.auto.repository;

import ai.content.auto.entity.N8nNodeRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for N8N Node Run entities
 */
@Repository
public interface N8nNodeRunRepository extends JpaRepository<N8nNodeRun, Long> {

        /**
         * Find all node runs for a specific user
         */
        List<N8nNodeRun> findByUserIdOrderByCreatedAtDesc(Long userId);

        /**
         * Find paginated node runs for a specific user
         */
        Page<N8nNodeRun> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        /**
         * Find node runs by user and status
         */
        List<N8nNodeRun> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId,
                        List<N8nNodeRun.N8nNodeRunStatus> statuses);

        /**
         * Find node runs by user and workflow ID
         */
        List<N8nNodeRun> findByUserIdAndWorkflowIdOrderByCreatedAtDesc(Long userId, String workflowId);

        /**
         * Find node runs by user and node type
         */
        List<N8nNodeRun> findByUserIdAndNodeTypeOrderByCreatedAtDesc(Long userId, String nodeType);

        /**
         * Find node runs with search functionality
         */
        @Query("SELECT n FROM N8nNodeRun n WHERE n.userId = :userId " +
                        "AND (LOWER(n.workflowName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(n.nodeName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(n.nodeType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "ORDER BY n.createdAt DESC")
        List<N8nNodeRun> findByUserIdAndSearchOrderByCreatedAtDesc(@Param("userId") Long userId,
                        @Param("search") String search);

        /**
         * Find node runs with complex filtering - Simplified for PostgreSQL
         * compatibility
         */
        @Query("SELECT n FROM N8nNodeRun n WHERE n.userId = :userId " +
                        "AND (COALESCE(:statuses, NULL) IS NULL OR n.status IN :statuses) " +
                        "AND (COALESCE(:workflowId, '') = '' OR n.workflowId = :workflowId) " +
                        "AND (COALESCE(:nodeType, '') = '' OR n.nodeType = :nodeType) " +
                        "AND (COALESCE(:search, '') = '' OR " +
                        "LOWER(n.workflowName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(n.nodeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(n.nodeType) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND (COALESCE(:dateFrom, NULL) IS NULL OR n.createdAt >= :dateFrom) " +
                        "AND (COALESCE(:dateTo, NULL) IS NULL OR n.createdAt <= :dateTo) " +
                        "ORDER BY n.createdAt DESC")
        List<N8nNodeRun> findByUserIdWithFilters(
                        @Param("userId") Long userId,
                        @Param("statuses") List<N8nNodeRun.N8nNodeRunStatus> statuses,
                        @Param("workflowId") String workflowId,
                        @Param("nodeType") String nodeType,
                        @Param("search") String search,
                        @Param("dateFrom") Instant dateFrom,
                        @Param("dateTo") Instant dateTo);

        /**
         * Get statistics for user's node runs
         */
        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId")
        Long countByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId AND n.status = 'SUCCESS'")
        Long countSuccessfulByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId AND n.status = 'FAILED'")
        Long countFailedByUserId(@Param("userId") Long userId);

        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId AND n.status = 'RUNNING'")
        Long countRunningByUserId(@Param("userId") Long userId);

        @Query("SELECT AVG(n.duration) FROM N8nNodeRun n WHERE n.userId = :userId AND n.duration IS NOT NULL")
        Double getAverageDurationByUserId(@Param("userId") Long userId);

        /**
         * Get statistics with date filtering
         */
        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId " +
                        "AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) " +
                        "AND (:dateTo IS NULL OR n.createdAt <= :dateTo)")
        Long countByUserIdAndDateRange(@Param("userId") Long userId, @Param("dateFrom") Instant dateFrom,
                        @Param("dateTo") Instant dateTo);

        @Query("SELECT COUNT(n) FROM N8nNodeRun n WHERE n.userId = :userId AND n.status = 'SUCCESS' " +
                        "AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) " +
                        "AND (:dateTo IS NULL OR n.createdAt <= :dateTo)")
        Long countSuccessfulByUserIdAndDateRange(@Param("userId") Long userId, @Param("dateFrom") Instant dateFrom,
                        @Param("dateTo") Instant dateTo);

        @Query("SELECT AVG(n.duration) FROM N8nNodeRun n WHERE n.userId = :userId AND n.duration IS NOT NULL " +
                        "AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) " +
                        "AND (:dateTo IS NULL OR n.createdAt <= :dateTo)")
        Double getAverageDurationByUserIdAndDateRange(@Param("userId") Long userId, @Param("dateFrom") Instant dateFrom,
                        @Param("dateTo") Instant dateTo);
}