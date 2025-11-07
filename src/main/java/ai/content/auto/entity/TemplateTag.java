package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "template_tags")
public class TemplateTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Size(max = 100)
    @Column(name = "slug", length = 100, unique = true)
    private String slug;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Size(max = 20)
    @Column(name = "color", length = 20)
    private String color;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (slug == null && name != null) {
            slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}