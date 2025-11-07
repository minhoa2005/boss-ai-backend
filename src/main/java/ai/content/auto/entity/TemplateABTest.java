package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Entity for template A/B testing
 */
@Entity
@Table(name = "template_ab_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateABTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_a_template_id", nullable = false)
    private ContentTemplate variantATemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_b_template_id", nullable = false)
    private ContentTemplate variantBTemplate;

    @Column(name = "traffic_split", nullable = false)
    private Integer trafficSplit = 50; // Percentage to variant A

    @Column(name = "metric_to_optimize", nullable = false, length = 50)
    private String metricToOptimize;

    @Column(name = "min_sample_size", nullable = false)
    private Integer minSampleSize = 30;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
