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

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "workspace_activity_logs")
public class WorkspaceActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 50)
    @NotNull
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Size(max = 50)
    @NotNull
    @Column(name = "activity_category", nullable = false, length = 50)
    private String activityCategory;

    @NotNull
    @Column(name = "activity_description", nullable = false, length = Integer.MAX_VALUE)
    private String activityDescription;

    @Size(max = 50)
    @Column(name = "target_resource_type", length = 50)
    private String targetResourceType;

    @Column(name = "target_resource_id")
    private Long targetResourceId;

    @Size(max = 500)
    @Column(name = "target_resource_name", length = 500)
    private String targetResourceName;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnDefault("'{}'")
    @Column(name = "activity_metadata", nullable = false)
    private Map<String, Object> activityMetadata;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "user_agent", length = Integer.MAX_VALUE)
    private String userAgent;

    @Size(max = 100)
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Size(max = 100)
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Size(max = 2)
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 20)
    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Size(max = 50)
    @Column(name = "browser", length = 50)
    private String browser;

    @Size(max = 50)
    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'LOW'")
    @Column(name = "impact_level", nullable = false, length = 20)
    private String impactLevel;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'INFO'")
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_activity_id")
    private WorkspaceActivityLog parentActivity;

    @Size(max = 100)
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'SUCCESS'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;

    @Size(max = 50)
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @ColumnDefault("'{}'")
    @Column(name = "compliance_tags", columnDefinition = "text[]")
    private List<String> complianceTags;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'STANDARD'")
    @Column(name = "retention_policy", nullable = false, length = 50)
    private String retentionPolicy;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "activity_timestamp", nullable = false)
    private Instant activityTimestamp;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Enum classes for type safety
    public enum ImpactLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Severity {
        DEBUG, INFO, WARN, ERROR, FATAL
    }

    public enum Status {
        SUCCESS, FAILURE, PARTIAL, CANCELLED
    }

    public enum RetentionPolicy {
        STANDARD, EXTENDED, PERMANENT, MINIMAL
    }

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET, API, SYSTEM
    }

    // Common activity types
    public static class ActivityType {
        public static final String WORKSPACE_CREATED = "WORKSPACE_CREATED";
        public static final String WORKSPACE_UPDATED = "WORKSPACE_UPDATED";
        public static final String WORKSPACE_DELETED = "WORKSPACE_DELETED";
        public static final String MEMBER_INVITED = "MEMBER_INVITED";
        public static final String MEMBER_JOINED = "MEMBER_JOINED";
        public static final String MEMBER_LEFT = "MEMBER_LEFT";
        public static final String MEMBER_REMOVED = "MEMBER_REMOVED";
        public static final String MEMBER_ROLE_CHANGED = "MEMBER_ROLE_CHANGED";
        public static final String CONTENT_SHARED = "CONTENT_SHARED";
        public static final String CONTENT_UNSHARED = "CONTENT_UNSHARED";
        public static final String CONTENT_VIEWED = "CONTENT_VIEWED";
        public static final String CONTENT_DOWNLOADED = "CONTENT_DOWNLOADED";
        public static final String CONTENT_COMMENTED = "CONTENT_COMMENTED";
        public static final String CONTENT_EDITED = "CONTENT_EDITED";
        public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";
        public static final String PERMISSIONS_CHANGED = "PERMISSIONS_CHANGED";
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String API_CALL = "API_CALL";
    }

    // Common activity categories
    public static class ActivityCategory {
        public static final String WORKSPACE_MANAGEMENT = "WORKSPACE_MANAGEMENT";
        public static final String MEMBER_MANAGEMENT = "MEMBER_MANAGEMENT";
        public static final String CONTENT_MANAGEMENT = "CONTENT_MANAGEMENT";
        public static final String COLLABORATION = "COLLABORATION";
        public static final String SECURITY = "SECURITY";
        public static final String SYSTEM = "SYSTEM";
        public static final String BILLING = "BILLING";
        public static final String INTEGRATION = "INTEGRATION";
    }
}