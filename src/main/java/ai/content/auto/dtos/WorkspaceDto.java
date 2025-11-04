package ai.content.auto.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDto {
    private Long id;
    private String name;
    private String description;
    private Map<String, Object> settings;

    // Owner information
    private Long ownerId;
    private String ownerUsername;
    private String ownerEmail;

    // Subscription and billing
    private String subscriptionPlan;
    private String billingStatus;
    private BigDecimal monthlyCost;
    private Instant subscriptionExpiresAt;

    // Limits and usage
    private Integer memberLimit;
    private Integer contentLimit;
    private Integer storageLimitMb;
    private Integer apiCallsLimit;
    private Integer currentMemberCount;
    private Integer currentContentCount;
    private Integer currentStorageUsedMb;
    private Integer currentApiCallsUsed;

    // Status and visibility
    private String status;
    private String visibility;

    // Branding
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String customDomain;

    // Collaboration settings
    private Boolean allowExternalSharing;
    private Boolean requireApprovalForSharing;
    private Boolean enableComments;
    private Boolean enableVersionControl;
    private Boolean enableRealTimeCollaboration;

    // Metadata
    private String industry;
    private String companySize;
    private String useCase;
    private String timezone;
    private String language;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // Calculated fields
    private Double memberUsagePercent;
    private Double contentUsagePercent;
    private Double storageUsagePercent;
    private Double apiUsagePercent;
    private Boolean approachingMemberLimit;
    private Boolean approachingContentLimit;
    private Boolean approachingStorageLimit;
    private Boolean approachingApiLimit;
    private Long daysSinceCreation;
}