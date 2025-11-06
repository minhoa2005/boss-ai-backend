package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "openai_response_log")
public class OpenaiResponseLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "content_input")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> contentInput;

  @Column(name = "openai_result")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> openaiResult;

  @Column(name = "create_at")
  private Instant createAt;

  @Column(name = "response_time")
  private Instant responseTime;

  @Size(max = 50)
  @Column(name = "model", length = 50)
  private String model;

  @Size(max = 255)
  @Column(name = "openai_response_id", length = 255)
  private String openaiResponseId;
}
