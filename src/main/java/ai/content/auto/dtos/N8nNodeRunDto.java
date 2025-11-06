package ai.content.auto.dtos;

import ai.content.auto.entity.N8nNodeRun;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * N8N Node Run DTO
 * Data Transfer Object for N8N workflow node execution information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class N8nNodeRunDto {

    private Long id;
    private Long userId;
    private String workflowId;
    private String workflowName;
    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private N8nNodeRun.N8nNodeRunStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant endTime;

    private Long duration; // Duration in milliseconds
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
}