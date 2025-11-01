package ai.content.auto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/** Mapping for DB view */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "v_user_configs")
public class VUserConfig {
  @Id
  @Column(name = "id")
  private Long id;

  @Size(max = 100)
  @Column(name = "category", length = 100)
  private String category;

  @Size(max = 100)
  @Column(name = "value", length = 100)
  private String value;

  @Size(max = 200)
  @Column(name = "label", length = 200)
  private String label;

  @Size(max = 200)
  @Column(name = "display_label", length = 200)
  private String displayLabel;

  @Size(max = 500)
  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "config_active")
  private Boolean configActive;

  @Size(max = 10)
  @Column(name = "language", length = 10)
  private String language;

  @Column(name = "config_created_at")
  private Instant configCreatedAt;

  @Column(name = "config_updated_at")
  private Instant configUpdatedAt;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "is_selected")
  private Boolean isSelected;

  @Column(name = "user_selection_created_at")
  private Instant userSelectionCreatedAt;

  @Column(name = "user_selection_updated_at")
  private Instant userSelectionUpdatedAt;
}
