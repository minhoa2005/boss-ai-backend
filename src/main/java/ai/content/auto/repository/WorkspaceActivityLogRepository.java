package ai.content.auto.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.content.auto.entity.User;
import ai.content.auto.entity.Workspace;
import ai.content.auto.entity.WorkspaceActivityLog;

@Repository
public interface WorkspaceActivityLogRepository extends JpaRepository<WorkspaceActivityLog, Long> {

        /**
         * Find activity logs by workspace
         */
        Page<WorkspaceActivityLog> findByWorkspaceOrderByActivityTimestampDesc(Workspace workspace, Pageable pageable);

        /**
         * Find activity logs by workspace and user
         */
        Page<WorkspaceActivityLog> findByWorkspaceAndUserOrderByActivityTimestampDesc(Workspace workspace, User user,
                        Pageable pageable);

        /**
         * Find activity logs by workspace and activity type
         */
        List<WorkspaceActivityLog> findByWorkspaceAndActivityTypeOrderByActivityTimestampDesc(Workspace workspace,
                        String activityType);

        /**
         * Find activity logs by workspace and activity category
         */
        List<WorkspaceActivityLog> findByWorkspaceAndActivityCategoryOrderByActivityTimestampDesc(Workspace workspace,
                        String activityCategory);

        /**
         * Find activity logs by workspace and severity
         */
        List<WorkspaceActivityLog> findByWorkspaceAndSeverityOrderByActivityTimestampDesc(Workspace workspace,
                        String severity);

        /**
         * Find activity logs by workspace and status
         */
        List<WorkspaceActivityLog> findByWorkspaceAndStatusOrderByActivityTimestampDesc(Workspace workspace,
                        String status);

        /**
         * Find activity logs by workspace and impact level
         */
        List<WorkspaceActivityLog> findByWorkspaceAndImpactLevelOrderByActivityTimestampDesc(Workspace workspace,
                        String impactLevel);

        /**
         * Find activity logs in date range
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.activityTimestamp BETWEEN :startDate AND :endDate ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findActivityLogsBetween(@Param("workspace") Workspace workspace,
                        @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

        /**
         * Find recent activity logs
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.activityTimestamp >= :since ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findRecentActivityLogs(@Param("workspace") Workspace workspace,
                        @Param("since") Instant since, Pageable pageable);

        /**
         * Find error activity logs
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.severity IN ('ERROR', 'FATAL') ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findErrorLogs(@Param("workspace") Workspace workspace);

        /**
         * Find activity logs by target resource
         */
        List<WorkspaceActivityLog> findByWorkspaceAndTargetResourceTypeAndTargetResourceIdOrderByActivityTimestampDesc(
                        Workspace workspace, String targetResourceType, Long targetResourceId);

        /**
         * Find activity logs by correlation ID
         */
        List<WorkspaceActivityLog> findByCorrelationIdOrderByActivityTimestampAsc(String correlationId);

        /**
         * Find activity logs by parent activity
         */
        List<WorkspaceActivityLog> findByParentActivityOrderByActivityTimestampAsc(WorkspaceActivityLog parentActivity);

        /**
         * Find activity logs by session ID
         */
        List<WorkspaceActivityLog> findByWorkspaceAndSessionIdOrderByActivityTimestampDesc(Workspace workspace,
                        String sessionId);

        /**
         * Find activity logs by IP address
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.ipAddress = :ipAddress ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findByWorkspaceAndIpAddress(@Param("workspace") Workspace workspace,
                        @Param("ipAddress") String ipAddress);

        /**
         * Count activity logs by type
         */
        long countByWorkspaceAndActivityType(Workspace workspace, String activityType);

        /**
         * Count activity logs by category
         */
        long countByWorkspaceAndActivityCategory(Workspace workspace, String activityCategory);

        /**
         * Count activity logs by status
         */
        long countByWorkspaceAndStatus(Workspace workspace, String status);

        /**
         * Count activity logs in date range
         */
        @Query("SELECT COUNT(wal) FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.activityTimestamp BETWEEN :startDate AND :endDate")
        long countActivityLogsBetween(@Param("workspace") Workspace workspace, @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        /**
         * Get activity statistics by category
         */
        @Query("SELECT wal.activityCategory, COUNT(wal), " +
                        "COUNT(CASE WHEN wal.status = 'SUCCESS' THEN 1 END), " +
                        "COUNT(CASE WHEN wal.status = 'FAILURE' THEN 1 END) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "GROUP BY wal.activityCategory ORDER BY COUNT(wal) DESC")
        List<Object[]> getActivityStatisticsByCategory(@Param("workspace") Workspace workspace);

        /**
         * Get activity statistics by type
         */
        @Query("SELECT wal.activityType, COUNT(wal), " +
                        "COUNT(CASE WHEN wal.status = 'SUCCESS' THEN 1 END), " +
                        "COUNT(CASE WHEN wal.status = 'FAILURE' THEN 1 END) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "GROUP BY wal.activityType ORDER BY COUNT(wal) DESC")
        List<Object[]> getActivityStatisticsByType(@Param("workspace") Workspace workspace);

        /**
         * Get activity statistics by user
         */
        @Query("SELECT wal.user, COUNT(wal), " +
                        "COUNT(CASE WHEN wal.status = 'SUCCESS' THEN 1 END), " +
                        "COUNT(CASE WHEN wal.status = 'FAILURE' THEN 1 END) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.user IS NOT NULL " +
                        "GROUP BY wal.user ORDER BY COUNT(wal) DESC")
        List<Object[]> getActivityStatisticsByUser(@Param("workspace") Workspace workspace);

        /**
         * Get hourly activity distribution
         */
        @Query("SELECT EXTRACT(HOUR FROM wal.activityTimestamp) as hour, COUNT(wal) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "AND wal.activityTimestamp >= :since " +
                        "GROUP BY EXTRACT(HOUR FROM wal.activityTimestamp) " +
                        "ORDER BY hour")
        List<Object[]> getHourlyActivityDistribution(@Param("workspace") Workspace workspace,
                        @Param("since") Instant since);

        /**
         * Get daily activity distribution
         */
        @Query("SELECT DATE(wal.activityTimestamp) as date, COUNT(wal) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "AND wal.activityTimestamp >= :since " +
                        "GROUP BY DATE(wal.activityTimestamp) " +
                        "ORDER BY date")
        List<Object[]> getDailyActivityDistribution(@Param("workspace") Workspace workspace,
                        @Param("since") Instant since);

        /**
         * Find most active users
         */
        @Query("SELECT wal.user, COUNT(wal) as activityCount " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "AND wal.user IS NOT NULL AND wal.activityTimestamp >= :since " +
                        "GROUP BY wal.user ORDER BY activityCount DESC")
        List<Object[]> findMostActiveUsers(@Param("workspace") Workspace workspace, @Param("since") Instant since,
                        Pageable pageable);

        /**
         * Find failed activities
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.status = 'FAILURE' ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findFailedActivities(@Param("workspace") Workspace workspace, Pageable pageable);

        /**
         * Find high impact activities
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.impactLevel IN ('HIGH', 'CRITICAL') ORDER BY wal.activityTimestamp DESC")
        List<WorkspaceActivityLog> findHighImpactActivities(@Param("workspace") Workspace workspace, Pageable pageable);

        /**
         * Find activities by compliance tag (PostgreSQL array ANY)
         */
        @Query(value = "SELECT * FROM workspace_activity_logs wal " +
                        "WHERE wal.workspace_id = :#{#workspace.id} AND :tag = ANY(wal.compliance_tags)", nativeQuery = true)
        List<WorkspaceActivityLog> findByComplianceTag(@Param("workspace") Workspace workspace,
                        @Param("tag") String tag);

        /**
         * Get average processing time by activity type
         */
        @Query("SELECT wal.activityType, AVG(wal.processingTimeMs) " +
                        "FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace " +
                        "AND wal.processingTimeMs IS NOT NULL " +
                        "GROUP BY wal.activityType ORDER BY AVG(wal.processingTimeMs) DESC")
        List<Object[]> getAverageProcessingTimeByType(@Param("workspace") Workspace workspace);

        /**
         * Find slow activities
         */
        @Query("SELECT wal FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.processingTimeMs > :thresholdMs ORDER BY wal.processingTimeMs DESC")
        List<WorkspaceActivityLog> findSlowActivities(@Param("workspace") Workspace workspace,
                        @Param("thresholdMs") Long thresholdMs);

        /**
         * Delete old activity logs based on retention policy
         */
        @Query("DELETE FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.retentionPolicy = 'MINIMAL' AND wal.activityTimestamp < :cutoffDate")
        void deleteOldMinimalRetentionLogs(@Param("workspace") Workspace workspace,
                        @Param("cutoffDate") Instant cutoffDate);

        /**
         * Delete old standard retention logs
         */
        @Query("DELETE FROM WorkspaceActivityLog wal WHERE wal.workspace = :workspace AND wal.retentionPolicy = 'STANDARD' AND wal.activityTimestamp < :cutoffDate")
        void deleteOldStandardRetentionLogs(@Param("workspace") Workspace workspace,
                        @Param("cutoffDate") Instant cutoffDate);
}