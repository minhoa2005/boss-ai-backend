package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing audit log entries for comprehensive system auditing.
 * Tracks all user actions, system events, and data changes for compliance and
 * debugging.
 * 
 * Requirements: 7.1, 7.2
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Audit event identification
    @Column(name = "event_id", unique = true, nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;

    @Column(name = "event_description", nullable = false, columnDefinition = "TEXT")
    private String eventDescription;

    // User and session information
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "impersonated_by")
    private Long impersonatedBy;

    // Resource and action details
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_name", length = 500)
    private String resourceName;

    @Column(name = "action_performed", nullable = false, length = 100)
    private String actionPerformed;

    @Column(name = "action_result", nullable = false, length = 20)
    @Builder.Default
    private String actionResult = "SUCCESS";

    // Request and response information
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_url", columnDefinition = "TEXT")
    private String requestUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> requestHeaders = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> requestBody = Map.of();

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> responseBody = Map.of();

    // Data changes tracking
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> oldValues = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> newValues = Map.of();

    @Column(name = "changed_fields", columnDefinition = "text[]")
    @Builder.Default
    private List<String> changedFields = List.of();

    @Column(name = "data_sensitivity_level", length = 20)
    @Builder.Default
    private String dataSensitivityLevel = "PUBLIC";

    // Security and compliance information
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geographic_location", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> geographicLocation = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "security_context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> securityContext = Map.of();

    @Column(name = "compliance_tags", columnDefinition = "text[]")
    @Builder.Default
    private List<String> complianceTags = List.of();

    // Risk and impact assessment
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private String riskLevel = "LOW";

    @Column(name = "impact_level", nullable = false, length = 20)
    @Builder.Default
    private String impactLevel = "LOW";

    @Column(name = "business_impact", columnDefinition = "TEXT")
    private String businessImpact;

    @Column(name = "security_implications", columnDefinition = "TEXT")
    private String securityImplications;

    // Error and exception details
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "exception_type", length = 100)
    private String exceptionType;

    // Performance and timing information
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "database_queries_count")
    @Builder.Default
    private Integer databaseQueriesCount = 0;

    @Column(name = "external_api_calls_count")
    @Builder.Default
    private Integer externalApiCallsCount = 0;

    // Correlation and tracing
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "parent_event_id", length = 100)
    private String parentEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_events", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> relatedEvents = List.of();

    // Workflow and approval information
    @Column(name = "workflow_id", length = 100)
    private String workflowId;

    @Column(name = "approval_status", length = 20)
    private String approvalStatus;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approval_timestamp")
    private Instant approvalTimestamp;

    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments;

    // Data retention and archival
    @Column(name = "retention_policy", nullable = false, length = 50)
    @Builder.Default
    private String retentionPolicy = "STANDARD";

    @Column(name = "archive_date")
    private Instant archiveDate;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    // Compliance and regulatory information
    @Column(name = "gdpr_relevant", nullable = false)
    @Builder.Default
    private Boolean gdprRelevant = false;

    @Column(name = "pii_involved", nullable = false)
    @Builder.Default
    private Boolean piiInvolved = false;

    @Column(name = "regulatory_requirements", columnDefinition = "text[]")
    @Builder.Default
    private List<String> regulatoryRequirements = List.of();

    @Column(name = "data_classification", length = 30)
    @Builder.Default
    private String dataClassification = "INTERNAL";

    // Alert and notification information
    @Column(name = "alert_triggered", nullable = false)
    @Builder.Default
    private Boolean alertTriggered = false;

    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    // Timestamp information
    @Column(name = "event_timestamp", nullable = false)
    @Builder.Default
    private Instant eventTimestamp = Instant.now();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}