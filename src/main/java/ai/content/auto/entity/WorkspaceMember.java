package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "workspace_members", uniqueConstraints = @UniqueConstraint(name = "uk_workspace_members_workspace_user", columnNames = {
        "workspace_id", "user_id" }))
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'MEMBER'")
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'{}'")
    @Column(name = "permissions", nullable = false)
    private Map<String, Object> permissions;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Size(max = 255)
    @Column(name = "invitation_token")
    private String invitationToken;

    @Column(name = "invitation_expires_at")
    private Instant invitationExpiresAt;

    @Column(name = "invitation_sent_at")
    private Instant invitationSentAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_content_created", nullable = false)
    private Integer totalContentCreated;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_content_shared", nullable = false)
    private Integer totalContentShared;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_comments_made", nullable = false)
    private Integer totalCommentsMade;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'{}'")
    @Column(name = "notification_preferences", nullable = false)
    private Map<String, Object> notificationPreferences;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'{}'")
    @Column(name = "workspace_settings", nullable = false)
    private Map<String, Object> workspaceSettings;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "can_invite_members", nullable = false)
    private Boolean canInviteMembers = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "can_manage_content", nullable = false)
    private Boolean canManageContent = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "can_export_data", nullable = false)
    private Boolean canExportData = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "can_view_analytics", nullable = false)
    private Boolean canViewAnalytics = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @ColumnDefault("'{}'")
    @Column(name = "training_modules_completed", columnDefinition = "text[]")
    private List<String> trainingModulesCompleted;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Enum classes for type safety
    public enum Role {
        OWNER, ADMIN, EDITOR, VIEWER, MEMBER, GUEST
    }

    public enum Status {
        ACTIVE, INVITED, SUSPENDED, LEFT, REMOVED, EXPIRED
    }
}