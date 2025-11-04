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

@Getter
@Setter
@Entity
@Table(name = "workspace_content_shares", uniqueConstraints = @UniqueConstraint(name = "uk_workspace_content_shares_workspace_content", columnNames = {
        "workspace_id", "content_id" }))
public class WorkspaceContentShare {
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
    @JoinColumn(name = "content_id", nullable = false)
    private ContentGeneration content;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "shared_by", nullable = false)
    private User sharedBy;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'VIEW'")
    @Column(name = "permission_level", nullable = false, length = 20)
    private String permissionLevel;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "allow_comments", nullable = false)
    private Boolean allowComments = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "allow_suggestions", nullable = false)
    private Boolean allowSuggestions = true;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "allow_editing", nullable = false)
    private Boolean allowEditing = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "allow_downloading", nullable = false)
    private Boolean allowDownloading = false;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "allow_copying", nullable = false)
    private Boolean allowCopying = true;

    @Size(max = 500)
    @Column(name = "share_title", length = 500)
    private String shareTitle;

    @Column(name = "share_description", length = Integer.MAX_VALUE)
    private String shareDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @ColumnDefault("'{}'")
    @Column(name = "share_tags", columnDefinition = "text[]")
    private List<String> shareTags;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_public_link", nullable = false)
    private Boolean isPublicLink = false;

    @Size(max = 255)
    @Column(name = "public_link_token")
    private String publicLinkToken;

    @Column(name = "public_link_expires_at")
    private Instant publicLinkExpiresAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "password_protected", nullable = false)
    private Boolean passwordProtected = false;

    @Size(max = 255)
    @Column(name = "access_password_hash")
    private String accessPasswordHash;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "last_viewed_at")
    private Instant lastViewedAt;

    @Column(name = "last_commented_at")
    private Instant lastCommentedAt;

    @Column(name = "shared_version_number")
    private Integer sharedVersionNumber;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "auto_update_shared_version", nullable = false)
    private Boolean autoUpdateSharedVersion = true;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'APPROVED'")
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = Integer.MAX_VALUE)
    private String rejectionReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "archived_by")
    private User archivedBy;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "shared_at", nullable = false)
    private Instant sharedAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Enum classes for type safety
    public enum PermissionLevel {
        VIEW, COMMENT, EDIT, ADMIN
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}