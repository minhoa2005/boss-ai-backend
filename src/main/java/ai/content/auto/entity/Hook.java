package ai.content.auto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hooks")
public class Hook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hook_id", nullable = false)
    private Long hook_id;
    @Column(name = "hook", nullable = false, length = 2000)
    private String hook;
    @Column(name = "industry", length = 100)
    private String industry;
    @Column(name = "target_audience", length = 100)
    private String target_audience;
    @Column(name = "tone", length = 100)
    private String tone;
}
