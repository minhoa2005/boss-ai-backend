package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "content_generations")
public class ContentGeneration {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Size(max = 20)
  @NotNull
  @Column(name = "content_type", nullable = false, length = 20)
  private String contentType;

  @Size(max = 20)
  @NotNull
  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Size(max = 50)
  @NotNull
  @Column(name = "ai_provider", nullable = false, length = 50)
  private String aiProvider;

  @Size(max = 100)
  @Column(name = "ai_model", length = 100)
  private String aiModel;

  @Column(name = "prompt", length = Integer.MAX_VALUE)
  private String prompt;

  @Column(name = "generated_content", length = Integer.MAX_VALUE)
  private String generatedContent;

  @Size(max = 500)
  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "word_count")
  private Integer wordCount;

  @Column(name = "character_count")
  private Integer characterCount;

  @Column(name = "tokens_used")
  private Integer tokensUsed;

  @Column(name = "generation_cost", precision = 10, scale = 6)
  private BigDecimal generationCost;

  @Column(name = "processing_time_ms")
  private Long processingTimeMs;

  @Column(name = "quality_score", precision = 5, scale = 2)
  private BigDecimal qualityScore;

  @Column(name = "readability_score", precision = 5, scale = 2)
  private BigDecimal readabilityScore;

  @Column(name = "sentiment_score", precision = 5, scale = 2)
  private BigDecimal sentimentScore;

  @Column(name = "template_id")
  private Long templateId;

  @Size(max = 100)
  @Column(name = "industry", length = 100)
  private String industry;

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

  @Size(max = 1000)
  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "retry_count", nullable = false)
  private Integer retryCount;

  @NotNull
  @ColumnDefault("3")
  @Column(name = "max_retries", nullable = false)
  private Integer maxRetries;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "failed_at")
  private Instant failedAt;

  @NotNull
  @ColumnDefault("true")
  @Column(name = "is_billable", nullable = false)
  private Boolean isBillable = false;

  @Column(name = "subscription_id")
  private Long subscriptionId;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at")
  private Instant updatedAt;

  @ColumnDefault("0")
  @Column(name = "version")
  private Long version;

  @ColumnDefault("1")
  @Column(name = "current_version")
  private Integer currentVersion;

  @Size(max = 255)
  @Column(name = "openai_response_id", length = 255)
  private String openaiResponseId;
}
