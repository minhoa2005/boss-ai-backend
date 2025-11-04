package ai.content.auto.repository;

import ai.content.auto.entity.AuditLog;
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
 * Repository for AuditLog entity.
 * Provides data access methods for audit log operations.
 * 
 * Requirements: 7.1, 7.2
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID with pagination.
     */
    Page<AuditLog> findByUserIdOrderByEventTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by event type with pagination.
     */
    Page<AuditLog> findByEventTypeOrderByEventTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find audit logs by resource type and resource ID.
     */
    List<AuditLog> findByResourceTypeAndResourceIdOrderByEventTimestampDesc(String resourceType, String resourceId);

    /**
     * Find audit logs by correlation ID.
     */
    List<AuditLog> findByCorrelationIdOrderByEventTimestampDesc(String correlationId);

    /**
     * Find audit logs within a time range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY a.eventTimestamp DESC")
    Page<AuditLog> findByEventTimestampBetween(@Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Find audit logs by event ID.
     */
    Optional<AuditLog> findByEventId(String eventId);

    /**
     * Find audit logs that require GDPR compliance.
     */
    Page<AuditLog> findByGdprRelevantTrueOrderByEventTimestampDesc(Pageable pageable);

    /**
     * Find audit logs involving PII.
     */
    Page<AuditLog> findByPiiInvolvedTrueOrderByEventTimestampDesc(Pageable pageable);

    /**
     * Find audit logs by risk level.
     */
    Page<AuditLog> findByRiskLevelOrderByEventTimestampDesc(String riskLevel, Pageable pageable);

    /**
     * Find audit logs that triggered alerts.
     */
    Page<AuditLog> findByAlertTriggeredTrueOrderByEventTimestampDesc(Pageable pageable);

    /**
     * Count audit logs by user ID within a time range.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.eventTimestamp BETWEEN :startTime AND :endTime")
    long countByUserIdAndEventTimestampBetween(@Param("userId") Long userId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find audit logs by action performed.
     */
    Page<AuditLog> findByActionPerformedOrderByEventTimestampDesc(String actionPerformed, Pageable pageable);

    /**
     * Find audit logs by event category.
     */
    Page<AuditLog> findByEventCategoryOrderByEventTimestampDesc(String eventCategory, Pageable pageable);

    /**
     * Find audit logs that are not archived.
     */
    Page<AuditLog> findByIsArchivedFalseOrderByEventTimestampDesc(Pageable pageable);

    /**
     * Find audit logs by data sensitivity level.
     */
    Page<AuditLog> findByDataSensitivityLevelOrderByEventTimestampDesc(String dataSensitivityLevel, Pageable pageable);

    /**
     * Search audit logs by event description containing text.
     */
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.eventDescription) LIKE LOWER(CONCAT('%', :searchText, '%')) ORDER BY a.eventTimestamp DESC")
    Page<AuditLog> searchByEventDescription(@Param("searchText") String searchText, Pageable pageable);

    /**
     * Find audit logs by multiple criteria for advanced search.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
            "(:actionPerformed IS NULL OR a.actionPerformed = :actionPerformed) AND " +
            "(:startTime IS NULL OR a.eventTimestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.eventTimestamp <= :endTime) " +
            "ORDER BY a.eventTimestamp DESC")
    Page<AuditLog> findByMultipleCriteria(@Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("resourceType") String resourceType,
            @Param("actionPerformed") String actionPerformed,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}