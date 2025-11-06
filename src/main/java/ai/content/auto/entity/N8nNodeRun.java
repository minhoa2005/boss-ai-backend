package ai.content.auto.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * N8N Node Run Entity
 * Represents execution data for N8N workflow nodes
 */
@Entity
@Table(name = "n8n_node_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class N8nNodeRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "workflow_id", nullable = false, length = 255)
  private String workflowId;

  @Column(name = "workflow_name", nullable = false, length = 500)
  private String workflowName;

  @Column(name = "execution_id", nullable = false, length = 255)
  private String executionId;

  @Column(name = "node_id", nullable = false, length = 255)
  private String nodeId;

  @Column(name = "node_name", nullable = false, length = 500)
  private String nodeName;

  @Column(name = "node_type", nullable = false, length = 255)
  private String nodeType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private N8nNodeRunStatus status;

  @Column(name = "start_time", nullable = false)
  private Instant startTime;

  @Column(name = "end_time")
  private Instant endTime;

  @Column(name = "duration")
  private Long duration; // Duration in milliseconds

  @Column(name = "input_data", columnDefinition = "TEXT")
  private String inputData; // JSON string

  @Column(name = "output_data", columnDefinition = "TEXT")
  private String outputData; // JSON string

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "retry_count", nullable = false)
  @Builder.Default
  private Integer retryCount = 0;

  @Column(name = "max_retries", nullable = false)
  @Builder.Default
  private Integer maxRetries = 3;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * N8N Node Run Status Enum
   */
  public enum N8nNodeRunStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT
  }
}