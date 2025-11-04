package ai.content.auto.repository;

import ai.content.auto.entity.Workspace;
import ai.content.auto.entity.WorkspaceMember;
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
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    /**
     * Find workspace members by workspace
     */
    List<WorkspaceMember> findByWorkspaceOrderByJoinedAtDesc(Workspace workspace);

    /**
     * Find workspace members by workspace and status
     */
    List<WorkspaceMember> findByWorkspaceAndStatusOrderByJoinedAtDesc(Workspace workspace, String status);

    /**
     * Find workspace members by user
     */
    List<WorkspaceMember> findByUserOrderByJoinedAtDesc(User user);

    /**
     * Find workspace members by user and status
     */
    List<WorkspaceMember> findByUserAndStatusOrderByJoinedAtDesc(User user, String status);

    /**
     * Find workspace member by workspace and user
     */
    Optional<WorkspaceMember> findByWorkspaceAndUser(Workspace workspace, User user);

    /**
     * Find workspace members by role
     */
    List<WorkspaceMember> findByWorkspaceAndRoleOrderByJoinedAtDesc(Workspace workspace, String role);

    /**
     * Find workspace owners
     */
    List<WorkspaceMember> findByRoleOrderByJoinedAtDesc(String role);

    /**
     * Find workspace members by invitation token
     */
    Optional<WorkspaceMember> findByInvitationToken(String invitationToken);

    /**
     * Find expired invitations
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.status = 'INVITED' AND wm.invitationExpiresAt < :currentTime")
    List<WorkspaceMember> findExpiredInvitations(@Param("currentTime") Instant currentTime);

    /**
     * Find active members in workspace
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.status = 'ACTIVE'")
    List<WorkspaceMember> findActiveMembers(@Param("workspace") Workspace workspace);

    /**
     * Count active members in workspace
     */
    long countByWorkspaceAndStatus(Workspace workspace, String status);

    /**
     * Count members by role in workspace
     */
    long countByWorkspaceAndRole(Workspace workspace, String role);

    /**
     * Find members with specific permissions
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.canInviteMembers = true")
    List<WorkspaceMember> findMembersWhoCanInvite(@Param("workspace") Workspace workspace);

    /**
     * Find members who can manage content
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.canManageContent = true")
    List<WorkspaceMember> findMembersWhoCanManageContent(@Param("workspace") Workspace workspace);

    /**
     * Find members who can view analytics
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.canViewAnalytics = true")
    List<WorkspaceMember> findMembersWhoCanViewAnalytics(@Param("workspace") Workspace workspace);

    /**
     * Find recently active members
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.lastActiveAt >= :since ORDER BY wm.lastActiveAt DESC")
    List<WorkspaceMember> findRecentlyActiveMembers(@Param("workspace") Workspace workspace,
            @Param("since") Instant since);

    /**
     * Find inactive members
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.status = 'ACTIVE' AND (wm.lastActiveAt IS NULL OR wm.lastActiveAt < :since)")
    List<WorkspaceMember> findInactiveMembers(@Param("workspace") Workspace workspace, @Param("since") Instant since);

    /**
     * Find members who haven't completed onboarding
     */
    List<WorkspaceMember> findByWorkspaceAndOnboardingCompletedFalseAndStatus(Workspace workspace, String status);

    /**
     * Find most active members by content creation
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.status = 'ACTIVE' ORDER BY wm.totalContentCreated DESC")
    Page<WorkspaceMember> findMostActiveContentCreators(@Param("workspace") Workspace workspace, Pageable pageable);

    /**
     * Find most collaborative members by comments
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace = :workspace AND wm.status = 'ACTIVE' ORDER BY wm.totalCommentsMade DESC")
    Page<WorkspaceMember> findMostCollaborativeMembers(@Param("workspace") Workspace workspace, Pageable pageable);

    /**
     * Get member activity statistics for workspace
     */
    @Query("SELECT " +
            "COUNT(wm) as totalMembers, " +
            "COUNT(CASE WHEN wm.status = 'ACTIVE' THEN 1 END) as activeMembers, " +
            "COUNT(CASE WHEN wm.status = 'INVITED' THEN 1 END) as pendingInvitations, " +
            "COUNT(CASE WHEN wm.onboardingCompleted = true THEN 1 END) as completedOnboarding, " +
            "AVG(wm.totalContentCreated) as avgContentCreated, " +
            "AVG(wm.totalCommentsMade) as avgCommentsMade " +
            "FROM WorkspaceMember wm WHERE wm.workspace = :workspace")
    Object[] getMemberStatistics(@Param("workspace") Workspace workspace);

    /**
     * Find members invited by specific user
     */
    List<WorkspaceMember> findByInvitedByOrderByInvitationSentAtDesc(User invitedBy);

    /**
     * Check if user is member of workspace
     */
    boolean existsByWorkspaceAndUser(Workspace workspace, User user);

    /**
     * Check if user is active member of workspace
     */
    boolean existsByWorkspaceAndUserAndStatus(Workspace workspace, User user, String status);

    /**
     * Find workspaces where user is owner
     */
    @Query("SELECT wm.workspace FROM WorkspaceMember wm WHERE wm.user = :user AND wm.role = 'OWNER'")
    List<Workspace> findWorkspacesOwnedByUser(@Param("user") User user);

    /**
     * Find workspaces where user is admin
     */
    @Query("SELECT wm.workspace FROM WorkspaceMember wm WHERE wm.user = :user AND wm.role IN ('OWNER', 'ADMIN')")
    List<Workspace> findWorkspacesAdministeredByUser(@Param("user") User user);

    /**
     * Delete member by workspace and user
     */
    void deleteByWorkspaceAndUser(Workspace workspace, User user);
}