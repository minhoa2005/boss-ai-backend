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
@Table(name = "user_presets")
public class UserPreset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Size(max = 200)
  @NotNull
  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", length = Integer.MAX_VALUE)
  private String description;

  @NotNull
  @ColumnDefault("'{}'")
  @Column(name = "configuration", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> configuration;

  @Size(max = 100)
  @Column(name = "category", length = 100)
  private String category;

  @Size(max = 50)
  @Column(name = "content_type", length = 50)
  private String contentType;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_favorite", nullable = false)
  private Boolean isFavorite = false;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "usage_count", nullable = false)
  private Integer usageCount;

  @Column(name = "last_used_at")
  private OffsetDateTime lastUsedAt;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "is_shared", nullable = false)
  private Boolean isShared = false;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "shared_with_workspace", nullable = false)
  private Boolean sharedWithWorkspace = false;

  @Column(name = "workspace_id")
  private Long workspaceId;

  @ColumnDefault("'{}'")
  @Column(name = "tags")
  private List<String> tags;

  @Column(name = "average_generation_time_ms")
  private Long averageGenerationTimeMs;

  @Column(name = "average_quality_score", precision = 5, scale = 2)
  private BigDecimal averageQualityScore;

  @ColumnDefault("0.00")
  @Column(name = "success_rate", precision = 5, scale = 2)
  private BigDecimal successRate;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "total_uses", nullable = false)
  private Integer totalUses;

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
