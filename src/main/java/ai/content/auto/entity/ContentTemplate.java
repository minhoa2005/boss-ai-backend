package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
@Table(name = "content_templates")
public class ContentTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Size(max = 200)
  @NotNull
  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", length = Integer.MAX_VALUE)
  private String description;

  @Size(max = 100)
  @NotNull
  @Column(name = "category", nullable = false, length = 100)
  private String category;

  @NotNull
  @Column(name = "prompt_template", nullable = false, length = Integer.MAX_VALUE)
  private String promptTemplate;

  @NotNull
  @ColumnDefault("'{}'")
  @Column(name = "default_params", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> defaultParams;

  @Size(max = 100)
  @Column(name = "industry", length = 100)
  private String industry;

  @Size(max = 50)
  @NotNull
  @Column(name = "content_type", nullable = false, length = 50)
  private String contentType;

  @Size(max = 200)
  @Column(name = "target_audience", length = 200)
  private String targetAudience;

  @Size(max = 50)
  @Column(name = "tone", length = 50)
  private String tone;

  @Size(max = 10)
  @ColumnDefault("'vi'")
  @Column(name = "language", length = 10)
  private String language;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "usage_count", nullable = false)
  private Integer usageCount;

  @ColumnDefault("0.00")
  @Column(name = "average_rating", precision = 3, scale = 2)
  private BigDecimal averageRating;

  @ColumnDefault("0.00")
  @Column(name = "success_rate", precision = 5, scale = 2)
  private BigDecimal successRate;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "total_ratings", nullable = false)
  private Integer totalRatings;

  @Size(max = 20)
  @NotNull
  @ColumnDefault("'ACTIVE'")
  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Size(max = 20)
  @NotNull
  @ColumnDefault("'PUBLIC'")
  @Column(name = "visibility", nullable = false, length = 20)
  private String visibility;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_system_template", nullable = false)
  private Boolean isSystemTemplate = false;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_featured", nullable = false)
  private Boolean isFeatured = false;

  @ColumnDefault("'{}'")
  @Column(name = "tags")
  private List<String> tags;

  @ColumnDefault("'{}'")
  @Column(name = "required_fields")
  private List<String> requiredFields;

  @ColumnDefault("'{}'")
  @Column(name = "optional_fields")
  private List<String> optionalFields;

  @ColumnDefault("'{}'")
  @Column(name = "validation_rules")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> validationRules;

  @Column(name = "average_generation_time_ms")
  private Long averageGenerationTimeMs;

  @Column(name = "average_tokens_used")
  private Integer averageTokensUsed;

  @Column(name = "average_cost", precision = 10, scale = 6)
  private BigDecimal averageCost;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.RESTRICT)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @ColumnDefault("0")
  @Column(name = "version")
  private Long version;
}
