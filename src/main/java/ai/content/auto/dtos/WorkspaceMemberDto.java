package ai.content.auto.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberDto {
    private Long id;
    private Long workspaceId;
    private String workspaceName;

    // User information
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    // Role and permissions
    private String role;
    private Map<String, Object> permissions;
    private String status;

    // Invitation details
    private Long invitedById;
    private String invitedByUsername;
    private String invitationToken;
    private Instant invitationExpiresAt;
    private Instant invitationSentAt;

    // Activity tracking
    private Instant lastActiveAt;
    private Integer totalContentCreated;
    private Integer totalContentShared;
    private Integer totalCommentsMade;

    // Preferences
    private Map<String, Object> notificationPreferences;
    private Map<String, Object> workspaceSettings;

    // Permissions flags
    private Boolean canInviteMembers;
    private Boolean canManageContent;
    private Boolean canExportData;
    private Boolean canViewAnalytics;

    // Onboarding
    private Boolean onboardingCompleted;
    private Instant onboardingCompletedAt;
    private List<String> trainingModulesCompleted;

    // Timestamps
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Calculated fields
    private Long daysSinceLastActive;
    private Long daysSinceJoined;
    private Double contentCreationRatePerDay;
    private Double commentRatePerDay;
    private String activityLevel; // HIGHLY_ACTIVE, MODERATELY_ACTIVE, LOW_ACTIVITY, INACTIVE, NEVER_ACTIVE
}