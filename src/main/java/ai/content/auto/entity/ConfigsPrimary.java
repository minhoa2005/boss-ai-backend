package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "configs_primary")
public class ConfigsPrimary {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Size(max = 100)
  @NotNull
  @Column(name = "category", nullable = false, length = 100)
  private String category;

  @Size(max = 100)
  @NotNull
  @Column(name = "value", nullable = false, length = 100)
  private String value;

  @Size(max = 200)
  @NotNull
  @Column(name = "label", nullable = false, length = 200)
  private String label;

  @Size(max = 200)
  @NotNull
  @Column(name = "display_label", nullable = false, length = 200)
  private String displayLabel;

  @Size(max = 500)
  @Column(name = "description", length = 500)
  private String description;

  @NotNull
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @NotNull
  @ColumnDefault("true")
  @Column(name = "active", nullable = false)
  private Boolean active = false;

  @Size(max = 10)
  @NotNull
  @ColumnDefault("'vi'")
  @Column(name = "language", nullable = false, length = 10)
  private String language;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Size(max = 100)
  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Size(max = 100)
  @Column(name = "updated_by", length = 100)
  private String updatedBy;
}
