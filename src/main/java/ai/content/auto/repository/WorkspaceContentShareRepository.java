package ai.content.auto.repository;

import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import ai.content.auto.entity.Workspace;
import ai.content.auto.entity.WorkspaceContentShare;
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
public interface WorkspaceContentShareRepository extends JpaRepository<WorkspaceContentShare, Long> {

    /**
     * Find content shares by workspace
     */
    List<WorkspaceContentShare> findByWorkspaceOrderBySharedAtDesc(Workspace workspace);

    /**
     * Find content shares by workspace and approval status
     */
    List<WorkspaceContentShare> findByWorkspaceAndApprovalStatusOrderBySharedAtDesc(Workspace workspace,
            String approvalStatus);

    /**
     * Find content shares by workspace excluding archived
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.isArchived = false ORDER BY wcs.sharedAt DESC")
    List<WorkspaceContentShare> findActiveContentShares(@Param("workspace") Workspace workspace);

    /**
     * Find content shares by content
     */
    List<WorkspaceContentShare> findByContentOrderBySharedAtDesc(ContentGeneration content);

    /**
     * Find content shares by user who shared
     */
    List<WorkspaceContentShare> findBySharedByOrderBySharedAtDesc(User sharedBy);

    /**
     * Find content share by workspace and content
     */
    Optional<WorkspaceContentShare> findByWorkspaceAndContent(Workspace workspace, ContentGeneration content);

    /**
     * Find content shares by permission level
     */
    List<WorkspaceContentShare> findByWorkspaceAndPermissionLevelOrderBySharedAtDesc(Workspace workspace,
            String permissionLevel);

    /**
     * Find public link shares
     */
    List<WorkspaceContentShare> findByIsPublicLinkTrueOrderBySharedAtDesc();

    /**
     * Find content share by public link token
     */
    Optional<WorkspaceContentShare> findByPublicLinkToken(String publicLinkToken);

    /**
     * Find expired public links
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.isPublicLink = true AND wcs.publicLinkExpiresAt < :currentTime")
    List<WorkspaceContentShare> findExpiredPublicLinks(@Param("currentTime") Instant currentTime);

    /**
     * Find content shares expiring soon
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.expiresAt IS NOT NULL AND wcs.expiresAt BETWEEN :currentTime AND :expiryTime")
    List<WorkspaceContentShare> findContentSharesExpiringSoon(@Param("currentTime") Instant currentTime,
            @Param("expiryTime") Instant expiryTime);

    /**
     * Find most viewed content shares
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.isArchived = false ORDER BY wcs.viewCount DESC")
    Page<WorkspaceContentShare> findMostViewedContent(@Param("workspace") Workspace workspace, Pageable pageable);

    /**
     * Find most downloaded content shares
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.isArchived = false ORDER BY wcs.downloadCount DESC")
    Page<WorkspaceContentShare> findMostDownloadedContent(@Param("workspace") Workspace workspace, Pageable pageable);

    /**
     * Find most commented content shares
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.isArchived = false ORDER BY wcs.commentCount DESC")
    Page<WorkspaceContentShare> findMostCommentedContent(@Param("workspace") Workspace workspace, Pageable pageable);

    /**
     * Find recently viewed content shares
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.lastViewedAt >= :since ORDER BY wcs.lastViewedAt DESC")
    List<WorkspaceContentShare> findRecentlyViewedContent(@Param("workspace") Workspace workspace,
            @Param("since") Instant since);

    /**
     * Find content shares by a single tag (PostgreSQL array ANY)
     */
    @Query(value = "SELECT * FROM workspace_content_shares wcs " +
            "WHERE wcs.workspace_id = :#{#workspace.id} AND :tag = ANY(wcs.share_tags)",
            nativeQuery = true)
    List<WorkspaceContentShare> findByTag(@Param("workspace") Workspace workspace, @Param("tag") String tag);

    /**
     * Find content shares with any of the specified tags (PostgreSQL overlaps operator &&)
     */
    @Query(value = "SELECT * FROM workspace_content_shares wcs " +
            "WHERE wcs.workspace_id = :#{#workspace.id} AND wcs.share_tags && CAST(:tags AS text[])",
            nativeQuery = true)
    List<WorkspaceContentShare> findByAnyTag(@Param("workspace") Workspace workspace, @Param("tags") String[] tags);

    /**
     * Search content shares by title or description
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND " +
            "(LOWER(wcs.shareTitle) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(wcs.shareDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<WorkspaceContentShare> searchContentShares(@Param("workspace") Workspace workspace,
            @Param("searchTerm") String searchTerm);

    /**
     * Count content shares by approval status
     */
    long countByWorkspaceAndApprovalStatus(Workspace workspace, String approvalStatus);

    /**
     * Count active content shares
     */
    long countByWorkspaceAndIsArchivedFalse(Workspace workspace);

    /**
     * Count public link shares
     */
    long countByWorkspaceAndIsPublicLinkTrue(Workspace workspace);

    /**
     * Get content sharing statistics for workspace
     */
    @Query("SELECT " +
            "COUNT(wcs) as totalShares, " +
            "COUNT(CASE WHEN wcs.approvalStatus = 'APPROVED' THEN 1 END) as approvedShares, " +
            "COUNT(CASE WHEN wcs.approvalStatus = 'PENDING' THEN 1 END) as pendingShares, " +
            "COUNT(CASE WHEN wcs.isPublicLink = true THEN 1 END) as publicLinkShares, " +
            "COUNT(CASE WHEN wcs.isArchived = false THEN 1 END) as activeShares, " +
            "SUM(wcs.viewCount) as totalViews, " +
            "SUM(wcs.downloadCount) as totalDownloads, " +
            "SUM(wcs.commentCount) as totalComments, " +
            "AVG(wcs.viewCount) as avgViews " +
            "FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace")
    Object[] getContentSharingStatistics(@Param("workspace") Workspace workspace);

    /**
     * Find content shares shared in date range
     */
    @Query("SELECT wcs FROM WorkspaceContentShare wcs WHERE wcs.workspace = :workspace AND wcs.sharedAt BETWEEN :startDate AND :endDate ORDER BY wcs.sharedAt DESC")
    List<WorkspaceContentShare> findContentSharedBetween(@Param("workspace") Workspace workspace,
            @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find archived content shares
     */
    List<WorkspaceContentShare> findByWorkspaceAndIsArchivedTrueOrderByArchivedAtDesc(Workspace workspace);

    /**
     * Find content shares pending approval
     */
    List<WorkspaceContentShare> findByApprovalStatusOrderBySharedAtAsc(String approvalStatus);

    /**
     * Find content shares that allow editing
     */
    List<WorkspaceContentShare> findByWorkspaceAndAllowEditingTrueOrderBySharedAtDesc(Workspace workspace);

    /**
     * Find password protected shares
     */
    List<WorkspaceContentShare> findByWorkspaceAndPasswordProtectedTrueOrderBySharedAtDesc(Workspace workspace);

    /**
     * Check if content is already shared in workspace
     */
    boolean existsByWorkspaceAndContent(Workspace workspace, ContentGeneration content);

    /**
     * Check if public link token exists
     */
    boolean existsByPublicLinkToken(String publicLinkToken);

    /**
     * Find content shares by shared version number
     */
    List<WorkspaceContentShare> findByContentAndSharedVersionNumber(ContentGeneration content, Integer versionNumber);

    /**
     * Delete content shares by workspace
     */
    void deleteByWorkspace(Workspace workspace);

    /**
     * Delete content shares by content
     */
    void deleteByContent(ContentGeneration content);
}