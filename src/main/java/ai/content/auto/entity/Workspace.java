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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'{}'")
    @Column(name = "settings", nullable = false)
    private Map<String, Object> settings;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'FREE'")
    @Column(name = "subscription_plan", nullable = false, length = 50)
    private String subscriptionPlan;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "billing_status", nullable = false, length = 20)
    private String billingStatus;

    @NotNull
    @ColumnDefault("5")
    @Column(name = "member_limit", nullable = false)
    private Integer memberLimit;

    @NotNull
    @ColumnDefault("100")
    @Column(name = "content_limit", nullable = false)
    private Integer contentLimit;

    @NotNull
    @ColumnDefault("1000")
    @Column(name = "storage_limit_mb", nullable = false)
    private Integer storageLimitMb;

    @NotNull
    @ColumnDefault("1000")
    @Column(name = "api_calls_limit", nullable = false)
    private Integer apiCallsLimit;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "current_member_count", nullable = false)
    private Integer currentMemberCount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "current_content_count", nullable = false)
    private Integer currentContentCount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "current_storage_used_mb", nullable = false)
    private Integer currentStorageUsedMb;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "current_api_calls_used", nullable = false)
    private Integer currentApiCallsUsed;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'PRIVATE'")
    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility;

    @Size(max = 500)
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Size(max = 7)
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Size(max = 7)
    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Size(max = 100)
    @Column(name = "custom_domain", length = 100)
    private String customDomain;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "allow_external_sharing", nullable = false)
    private Boolean allowExternalSharing = false;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "require_approval_for_sharing", nullable = false)
    private Boolean requireApprovalForSharing = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enable_comments", nullable = false)
    private Boolean enableComments = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enable_version_control", nullable = false)
    private Boolean enableVersionControl = true;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enable_real_time_collaboration", nullable = false)
    private Boolean enableRealTimeCollaboration = true;

    @Size(max = 100)
    @Column(name = "industry", length = 100)
    private String industry;

    @Size(max = 20)
    @Column(name = "company_size", length = 20)
    private String companySize;

    @Size(max = 100)
    @Column(name = "use_case", length = 100)
    private String useCase;

    @Size(max = 50)
    @ColumnDefault("'UTC'")
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Size(max = 10)
    @ColumnDefault("'vi'")
    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "subscription_started_at")
    private Instant subscriptionStartedAt;

    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    @Column(name = "last_billing_date")
    private Instant lastBillingDate;

    @Column(name = "next_billing_date")
    private Instant nextBillingDate;

    @ColumnDefault("0.00")
    @Column(name = "monthly_cost", precision = 10, scale = 2)
    private BigDecimal monthlyCost;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "usage_reset_date", nullable = false)
    private Instant usageResetDate;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ColumnDefault("0")
    @Column(name = "version")
    private Long version;

    // Enum classes for type safety
    public enum SubscriptionPlan {
        FREE, BASIC, PROFESSIONAL, ENTERPRISE, CUSTOM
    }

    public enum BillingStatus {
        ACTIVE, SUSPENDED, CANCELLED, PAST_DUE, TRIAL
    }

    public enum Status {
        ACTIVE, SUSPENDED, ARCHIVED, DELETED
    }

    public enum Visibility {
        PRIVATE, INTERNAL, PUBLIC
    }

    public enum CompanySize {
        SIZE_1_10("1-10"),
        SIZE_11_50("11-50"),
        SIZE_51_200("51-200"),
        SIZE_201_1000("201-1000"),
        SIZE_1000_PLUS("1000+");

        private final String value;

        CompanySize(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}