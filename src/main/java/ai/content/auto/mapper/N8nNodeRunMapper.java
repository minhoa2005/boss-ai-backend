package ai.content.auto.mapper;

import ai.content.auto.dtos.N8nNodeRunDto;
import ai.content.auto.entity.N8nNodeRun;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mapper for N8nNodeRun entity and DTO conversions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class N8nNodeRunMapper {

  private final ObjectMapper objectMapper;

  /**
   * Convert entity to DTO
   */
  public N8nNodeRunDto toDto(N8nNodeRun entity) {
    if (entity == null) {
      return null;
    }

    return N8nNodeRunDto.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .workflowId(entity.getWorkflowId())
        .workflowName(entity.getWorkflowName())
        .executionId(entity.getExecutionId())
        .nodeId(entity.getNodeId())
        .nodeName(entity.getNodeName())
        .nodeType(entity.getNodeType())
        .status(entity.getStatus())
        .startTime(entity.getStartTime())
        .endTime(entity.getEndTime())
        .duration(entity.getDuration())
        .inputData(parseJsonToMap(entity.getInputData()))
        .outputData(parseJsonToMap(entity.getOutputData()))
        .errorMessage(entity.getErrorMessage())
        .retryCount(entity.getRetryCount())
        .maxRetries(entity.getMaxRetries())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  /**
   * Convert DTO to entity
   */
  public N8nNodeRun toEntity(N8nNodeRunDto dto) {
    if (dto == null) {
      return null;
    }

    return N8nNodeRun.builder()
        .id(dto.getId())
        .userId(dto.getUserId())
        .workflowId(dto.getWorkflowId())
        .workflowName(dto.getWorkflowName())
        .executionId(dto.getExecutionId())
        .nodeId(dto.getNodeId())
        .nodeName(dto.getNodeName())
        .nodeType(dto.getNodeType())
        .status(dto.getStatus())
        .startTime(dto.getStartTime())
        .endTime(dto.getEndTime())
        .duration(dto.getDuration())
        .inputData(parseMapToJson(dto.getInputData()))
        .outputData(parseMapToJson(dto.getOutputData()))
        .errorMessage(dto.getErrorMessage())
        .retryCount(dto.getRetryCount())
        .maxRetries(dto.getMaxRetries())
        .createdAt(dto.getCreatedAt())
        .updatedAt(dto.getUpdatedAt())
        .build();
  }

  /**
   * Convert list of entities to DTOs
   */
  public List<N8nNodeRunDto> toDtoList(List<N8nNodeRun> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }

    return entities.stream()
        .map(this::toDto)
        .toList();
  }

  /**
   * Convert list of DTOs to entities
   */
  public List<N8nNodeRun> toEntityList(List<N8nNodeRunDto> dtos) {
    if (dtos == null) {
      return Collections.emptyList();
    }

    return dtos.stream()
        .map(this::toEntity)
        .toList();
  }

  /**
   * Parse JSON string to Map
   */
  private Map<String, Object> parseJsonToMap(String json) {
    if (json == null || json.trim().isEmpty()) {
      return Collections.emptyMap();
    }

    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
      });
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse JSON to Map: {}", json, e);
      return Collections.emptyMap();
    }
  }

  /**
   * Parse Map to JSON string
   */
  private String parseMapToJson(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse Map to JSON: {}", map, e);
      return null;
    }
  }
}