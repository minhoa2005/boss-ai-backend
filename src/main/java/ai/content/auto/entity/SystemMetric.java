package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "system_metrics")
public class SystemMetric {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Size(max = 100)
  @NotNull
  @Column(name = "service_name", nullable = false, length = 100)
  private String serviceName;

  @Size(max = 100)
  @Column(name = "instance_id", length = 100)
  private String instanceId;

  @Size(max = 20)
  @NotNull
  @ColumnDefault("'PRODUCTION'")
  @Column(name = "environment", nullable = false, length = 20)
  private String environment;

  @Size(max = 50)
  @Column(name = "version", length = 50)
  private String version;

  @Column(name = "response_time_avg", precision = 10, scale = 3)
  private BigDecimal responseTimeAvg;

  @Column(name = "response_time_p50", precision = 10, scale = 3)
  private BigDecimal responseTimeP50;

  @Column(name = "response_time_p95", precision = 10, scale = 3)
  private BigDecimal responseTimeP95;

  @Column(name = "response_time_p99", precision = 10, scale = 3)
  private BigDecimal responseTimeP99;

  @Column(name = "response_time_max", precision = 10, scale = 3)
  private BigDecimal responseTimeMax;

  @ColumnDefault("0.00")
  @Column(name = "requests_per_second", precision = 10, scale = 2)
  private BigDecimal requestsPerSecond;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "requests_total", nullable = false)
  private Integer requestsTotal;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "successful_requests", nullable = false)
  private Integer successfulRequests;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "failed_requests", nullable = false)
  private Integer failedRequests;

  @ColumnDefault("0.0000")
  @Column(name = "error_rate", precision = 5, scale = 4)
  private BigDecimal errorRate;

  @ColumnDefault("1.0000")
  @Column(name = "success_rate", precision = 5, scale = 4)
  private BigDecimal successRate;

  @ColumnDefault("100.00")
  @Column(name = "availability_percentage", precision = 5, scale = 2)
  private BigDecimal availabilityPercentage;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "uptime_seconds", nullable = false)
  private Integer uptimeSeconds;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "downtime_seconds", nullable = false)
  private Integer downtimeSeconds;

  @ColumnDefault("0.00")
  @Column(name = "cpu_usage_percentage", precision = 5, scale = 2)
  private BigDecimal cpuUsagePercentage;

  @ColumnDefault("0.00")
  @Column(name = "memory_usage_percentage", precision = 5, scale = 2)
  private BigDecimal memoryUsagePercentage;

  @ColumnDefault("0.00")
  @Column(name = "disk_usage_percentage", precision = 5, scale = 2)
  private BigDecimal diskUsagePercentage;

  @ColumnDefault("0")
  @Column(name = "network_io_bytes")
  private Long networkIoBytes;

  @ColumnDefault("0")
  @Column(name = "disk_io_bytes")
  private Long diskIoBytes;

  @ColumnDefault("0")
  @Column(name = "database_connections_active")
  private Integer databaseConnectionsActive;

  @Column(name = "database_connections_max")
  private Integer databaseConnectionsMax;

  @Column(name = "database_query_time_avg", precision = 10, scale = 3)
  private BigDecimal databaseQueryTimeAvg;

  @ColumnDefault("0")
  @Column(name = "database_slow_queries")
  private Integer databaseSlowQueries;

  @ColumnDefault("0")
  @Column(name = "database_deadlocks")
  private Integer databaseDeadlocks;

  @ColumnDefault("0.0000")
  @Column(name = "cache_hit_rate", precision = 5, scale = 4)
  private BigDecimal cacheHitRate;

  @ColumnDefault("0.0000")
  @Column(name = "cache_miss_rate", precision = 5, scale = 4)
  private BigDecimal cacheMissRate;

  @ColumnDefault("0")
  @Column(name = "cache_evictions")
  private Integer cacheEvictions;

  @ColumnDefault("0")
  @Column(name = "cache_memory_usage_mb")
  private Integer cacheMemoryUsageMb;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "ai_requests_total", nullable = false)
  private Integer aiRequestsTotal;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "ai_requests_successful", nullable = false)
  private Integer aiRequestsSuccessful;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "ai_requests_failed", nullable = false)
  private Integer aiRequestsFailed;

  @Column(name = "ai_response_time_avg", precision = 10, scale = 3)
  private BigDecimal aiResponseTimeAvg;

  @ColumnDefault("0")
  @Column(name = "ai_tokens_consumed")
  private Integer aiTokensConsumed;

  @ColumnDefault("0.000000")
  @Column(name = "ai_cost_total", precision = 12, scale = 6)
  private BigDecimal aiCostTotal;

  @ColumnDefault("0")
  @Column(name = "queue_size")
  private Integer queueSize;

  @Column(name = "queue_processing_time_avg", precision = 10, scale = 3)
  private BigDecimal queueProcessingTimeAvg;

  @ColumnDefault("0")
  @Column(name = "background_jobs_completed")
  private Integer backgroundJobsCompleted;

  @ColumnDefault("0")
  @Column(name = "background_jobs_failed")
  private Integer backgroundJobsFailed;

  @ColumnDefault("0")
  @Column(name = "security_events")
  private Integer securityEvents;

  @ColumnDefault("0")
  @Column(name = "failed_login_attempts")
  private Integer failedLoginAttempts;

  @ColumnDefault("0")
  @Column(name = "blocked_requests")
  private Integer blockedRequests;

  @ColumnDefault("0")
  @Column(name = "suspicious_activities")
  private Integer suspiciousActivities;

  @ColumnDefault("0")
  @Column(name = "active_users")
  private Integer activeUsers;

  @ColumnDefault("0")
  @Column(name = "new_registrations")
  private Integer newRegistrations;

  @ColumnDefault("0")
  @Column(name = "content_generations")
  private Integer contentGenerations;

  @ColumnDefault("0.00")
  @Column(name = "revenue_generated", precision = 12, scale = 2)
  private BigDecimal revenueGenerated;

  @ColumnDefault("0")
  @Column(name = "alerts_triggered")
  private Integer alertsTriggered;

  @ColumnDefault("0")
  @Column(name = "critical_alerts")
  private Integer criticalAlerts;

  @ColumnDefault("0")
  @Column(name = "warning_alerts")
  private Integer warningAlerts;

  @ColumnDefault("'[]'")
  @Column(name = "threshold_breaches")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> thresholdBreaches;

  @Size(max = 20)
  @NotNull
  @ColumnDefault("'MINUTE'")
  @Column(name = "measurement_period", nullable = false, length = 20)
  private String measurementPeriod;

  @NotNull
  @Column(name = "period_start_time", nullable = false)
  private OffsetDateTime periodStartTime;

  @NotNull
  @Column(name = "period_end_time", nullable = false)
  private OffsetDateTime periodEndTime;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "collection_timestamp", nullable = false)
  private OffsetDateTime collectionTimestamp;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'MONITORING_SYSTEM'")
  @Column(name = "data_source", nullable = false, length = 50)
  private String dataSource;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'AUTOMATIC'")
  @Column(name = "collection_method", nullable = false, length = 50)
  private String collectionMethod;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
