package ai.content.auto.repository;

import ai.content.auto.entity.Workspace;
import ai.content.auto.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /**
     * Find workspaces by owner
     */
    List<Workspace> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Find workspaces by owner and status
     */
    List<Workspace> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, String status);

    /**
     * Find workspaces by subscription plan
     */
    List<Workspace> findBySubscriptionPlanOrderByCreatedAtDesc(String subscriptionPlan);

    /**
     * Find workspaces by billing status
     */
    List<Workspace> findByBillingStatusOrderByCreatedAtDesc(String billingStatus);

    /**
     * Find workspaces with expiring subscriptions
     */
    @Query("SELECT w FROM Workspace w WHERE w.subscriptionExpiresAt IS NOT NULL AND w.subscriptionExpiresAt <= :expiryDate AND w.billingStatus = 'ACTIVE'")
    List<Workspace> findWorkspacesWithExpiringSubscriptions(@Param("expiryDate") Instant expiryDate);

    /**
     * Find workspaces that need usage reset
     */
    @Query("SELECT w FROM Workspace w WHERE w.usageResetDate <= :currentDate")
    List<Workspace> findWorkspacesNeedingUsageReset(@Param("currentDate") Instant currentDate);

    /**
     * Find workspaces by industry
     */
    List<Workspace> findByIndustryOrderByCreatedAtDesc(String industry);

    /**
     * Find workspaces approaching member limit
     */
    @Query("SELECT w FROM Workspace w WHERE w.currentMemberCount >= (w.memberLimit * 0.9)")
    List<Workspace> findWorkspacesApproachingMemberLimit();

    /**
     * Find workspaces approaching content limit
     */
    @Query("SELECT w FROM Workspace w WHERE w.currentContentCount >= (w.contentLimit * 0.9)")
    List<Workspace> findWorkspacesApproachingContentLimit();

    /**
     * Find workspaces approaching storage limit
     */
    @Query("SELECT w FROM Workspace w WHERE w.currentStorageUsedMb >= (w.storageLimitMb * 0.9)")
    List<Workspace> findWorkspacesApproachingStorageLimit();

    /**
     * Find workspaces approaching API limit
     */
    @Query("SELECT w FROM Workspace w WHERE w.currentApiCallsUsed >= (w.apiCallsLimit * 0.9)")
    List<Workspace> findWorkspacesApproachingApiLimit();

    /**
     * Find public workspaces
     */
    List<Workspace> findByVisibilityAndStatusOrderByCreatedAtDesc(String visibility, String status);

    /**
     * Search workspaces by name (case-insensitive)
     */
    @Query("SELECT w FROM Workspace w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%')) AND w.status = 'ACTIVE'")
    Page<Workspace> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Find workspace by custom domain
     */
    Optional<Workspace> findByCustomDomainAndStatus(String customDomain, String status);

    /**
     * Count workspaces by subscription plan
     */
    long countBySubscriptionPlan(String subscriptionPlan);

    /**
     * Count workspaces by billing status
     */
    long countByBillingStatus(String billingStatus);

    /**
     * Count active workspaces
     */
    long countByStatus(String status);

    /**
     * Get workspace statistics
     */
    @Query("SELECT " +
            "COUNT(w) as totalWorkspaces, " +
            "COUNT(CASE WHEN w.status = 'ACTIVE' THEN 1 END) as activeWorkspaces, " +
            "COUNT(CASE WHEN w.subscriptionPlan = 'FREE' THEN 1 END) as freeWorkspaces, " +
            "COUNT(CASE WHEN w.subscriptionPlan != 'FREE' THEN 1 END) as paidWorkspaces, " +
            "AVG(w.currentMemberCount) as avgMemberCount, " +
            "AVG(w.currentContentCount) as avgContentCount " +
            "FROM Workspace w")
    Object[] getWorkspaceStatistics();

    /**
     * Find workspaces created in date range
     */
    @Query("SELECT w FROM Workspace w WHERE w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    List<Workspace> findWorkspacesCreatedBetween(@Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find workspaces with high usage
     * Use floating-point math by multiplying by 1.0 to avoid integer division in JPQL.
     */
    @Query("SELECT w FROM Workspace w WHERE " +
            "(1.0 * w.currentMemberCount / w.memberLimit) > 0.8 OR " +
            "(1.0 * w.currentContentCount / w.contentLimit) > 0.8 OR " +
            "(1.0 * w.currentStorageUsedMb / w.storageLimitMb) > 0.8 OR " +
            "(1.0 * w.currentApiCallsUsed / w.apiCallsLimit) > 0.8")
    List<Workspace> findHighUsageWorkspaces();

    /**
     * Check if workspace name exists for owner
     */
    boolean existsByOwnerAndName(User owner, String name);

    /**
     * Check if custom domain is available
     */
    boolean existsByCustomDomain(String customDomain);
}