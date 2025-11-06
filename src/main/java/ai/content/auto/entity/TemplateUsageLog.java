package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "template_usage_logs")
public class TemplateUsageLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "template_id", nullable = false)
  private ContentTemplate template;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  @JoinColumn(name = "content_generation_id")
  private ContentGeneration contentGeneration;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'GENERATION'")
  @Column(name = "usage_type", nullable = false, length = 50)
  private String usageType;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'WEB'")
  @Column(name = "usage_source", nullable = false, length = 50)
  private String usageSource;

  @Column(name = "generation_time_ms")
  private Long generationTimeMs;

  @Column(name = "tokens_used")
  private Integer tokensUsed;

  @Column(name = "generation_cost", precision = 10, scale = 6)
  private BigDecimal generationCost;

  @Column(name = "quality_score", precision = 5, scale = 2)
  private BigDecimal qualityScore;

  @Column(name = "user_rating")
  private Integer userRating;

  @Column(name = "user_feedback", length = Integer.MAX_VALUE)
  private String userFeedback;

  @Column(name = "was_successful")
  private Boolean wasSuccessful;

  @ColumnDefault("'{}'")
  @Column(name = "parameters_used")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> parametersUsed;

  @ColumnDefault("'{}'")
  @Column(name = "customizations_made")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> customizationsMade;

  @ColumnDefault("'{}'")
  @Column(name = "fields_modified")
  private List<String> fieldsModified;

  @Size(max = 100)
  @Column(name = "session_id", length = 100)
  private String sessionId;

  @Column(name = "user_agent", length = Integer.MAX_VALUE)
  private String userAgent;

  @Column(name = "ip_address")
  private InetAddress ipAddress;

  @Size(max = 2)
  @Column(name = "country_code", length = 2)
  private String countryCode;

  @Size(max = 50)
  @Column(name = "timezone", length = 50)
  private String timezone;

  @Size(max = 20)
  @Column(name = "device_type", length = 20)
  private String deviceType;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "used_at", nullable = false)
  private OffsetDateTime usedAt;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
