package ai.content.auto.service;

import ai.content.auto.dtos.N8nNodeRunDto;
import ai.content.auto.dtos.N8nNodeRunStatisticsDto;
import ai.content.auto.entity.N8nNodeRun;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock N8N Node Run Service for testing without database table
 * This service provides sample data until the database table is created
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!production") // Only active in non-production environments
public class MockN8nNodeRunService {

    private final SecurityUtil securityUtil;

    /**
     * Generate mock node runs for testing
     */
    public List<N8nNodeRunDto> getNodeRunsWithFilters(
            List<N8nNodeRun.N8nNodeRunStatus> statuses,
            String workflowId,
            String nodeType,
            String search,
            Instant dateFrom,
            Instant dateTo,
            Long userId) {

        try {
            Long currentUserId = userId != null ? userId : securityUtil.getCurrentUserId();
            log.info("Generating mock node runs for user: {}", currentUserId);

            List<N8nNodeRunDto> mockData = generateMockNodeRuns(currentUserId);

            // Apply filters
            return mockData.stream()
                    .filter(run -> statuses == null || statuses.isEmpty() || statuses.contains(run.getStatus()))
                    .filter(run -> workflowId == null || workflowId.isEmpty() || run.getWorkflowId().equals(workflowId))
                    .filter(run -> nodeType == null || nodeType.isEmpty() || run.getNodeType().equals(nodeType))
                    .filter(run -> search == null || search.isEmpty() ||
                            run.getWorkflowName().toLowerCase().contains(search.toLowerCase()) ||
                            run.getNodeName().toLowerCase().contains(search.toLowerCase()) ||
                            run.getNodeType().toLowerCase().contains(search.toLowerCase()))
                    .filter(run -> dateFrom == null || run.getCreatedAt().isAfter(dateFrom))
                    .filter(run -> dateTo == null || run.getCreatedAt().isBefore(dateTo))
                    .toList();

        } catch (Exception e) {
            log.error("Error generating mock node runs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get paginated mock node runs
     */
    public Page<N8nNodeRunDto> getPaginatedNodeRuns(int page, int size, Long userId) {
        List<N8nNodeRunDto> allRuns = getNodeRunsWithFilters(null, null, null, null, null, null, userId);

        int start = page * size;
        int end = Math.min(start + size, allRuns.size());

        List<N8nNodeRunDto> pageContent = start < allRuns.size() ? allRuns.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pageContent, org.springframework.data.domain.PageRequest.of(page, size), allRuns.size());
    }

    /**
     * Get mock statistics
     */
    public N8nNodeRunStatisticsDto getNodeRunStatistics(Long userId, Instant dateFrom, Instant dateTo) {
        List<N8nNodeRunDto> runs = getNodeRunsWithFilters(null, null, null, null, dateFrom, dateTo, userId);

        long totalRuns = runs.size();
        long successfulRuns = runs.stream()
                .mapToLong(run -> run.getStatus() == N8nNodeRun.N8nNodeRunStatus.SUCCESS ? 1 : 0).sum();
        long failedRuns = runs.stream().mapToLong(run -> run.getStatus() == N8nNodeRun.N8nNodeRunStatus.FAILED ? 1 : 0)
                .sum();
        long runningRuns = runs.stream()
                .mapToLong(run -> run.getStatus() == N8nNodeRun.N8nNodeRunStatus.RUNNING ? 1 : 0).sum();

        double averageDuration = runs.stream()
                .filter(run -> run.getDuration() != null)
                .mapToLong(N8nNodeRunDto::getDuration)
                .average()
                .orElse(0.0);

        double successRate = totalRuns > 0 ? (double) successfulRuns / totalRuns : 0.0;

        return N8nNodeRunStatisticsDto.builder()
                .totalRuns(totalRuns)
                .successfulRuns(successfulRuns)
                .failedRuns(failedRuns)
                .runningRuns(runningRuns)
                .averageDuration(averageDuration)
                .successRate(successRate)
                .build();
    }

    /**
     * Get mock node run by ID
     */
    public N8nNodeRunDto getNodeRunById(Long id) {
        List<N8nNodeRunDto> runs = getNodeRunsWithFilters(null, null, null, null, null, null, null);
        return runs.stream()
                .filter(run -> run.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mock retry operation
     */
    public N8nNodeRunDto retryNodeRun(Long id) {
        N8nNodeRunDto nodeRun = getNodeRunById(id);
        if (nodeRun != null) {
            nodeRun.setStatus(N8nNodeRun.N8nNodeRunStatus.PENDING);
            nodeRun.setRetryCount(nodeRun.getRetryCount() + 1);
            log.info("Mock retry for node run: {}", id);
        }
        return nodeRun;
    }

    /**
     * Mock cancel operation
     */
    public void cancelNodeRun(Long id) {
        log.info("Mock cancel for node run: {}", id);
    }

    /**
     * Generate sample mock data
     */
    private List<N8nNodeRunDto> generateMockNodeRuns(Long userId) {
        List<N8nNodeRunDto> mockRuns = new ArrayList<>();
        Instant now = Instant.now();

        // Sample workflows and nodes
        String[][] workflows = {
                { "wf_001", "Content Generation Workflow" },
                { "wf_002", "Video Processing Workflow" },
                { "wf_003", "Data Analysis Workflow" },
                { "wf_004", "Email Marketing Workflow" }
        };

        String[][] nodes = {
                { "node_001", "OpenAI Text Generator", "openai" },
                { "node_002", "Content Formatter", "formatter" },
                { "node_003", "Video Processor", "video" },
                { "node_004", "Data Transformer", "transformer" },
                { "node_005", "Email Sender", "email" },
                { "node_006", "Database Writer", "database" }
        };

        N8nNodeRun.N8nNodeRunStatus[] statuses = N8nNodeRun.N8nNodeRunStatus.values();

        // Generate 20 mock runs
        for (int i = 1; i <= 20; i++) {
            String[] workflow = workflows[i % workflows.length];
            String[] node = nodes[i % nodes.length];
            N8nNodeRun.N8nNodeRunStatus status = statuses[i % statuses.length];

            Instant startTime = now.minus(i * 30, ChronoUnit.MINUTES);
            Instant endTime = status == N8nNodeRun.N8nNodeRunStatus.RUNNING ? null
                    : startTime.plus(i * 5, ChronoUnit.SECONDS);
            Long duration = endTime != null ? endTime.toEpochMilli() - startTime.toEpochMilli() : null;

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("prompt", "Generate content about AI technology");
            inputData.put("maxTokens", 1000);

            Map<String, Object> outputData = new HashMap<>();
            if (status == N8nNodeRun.N8nNodeRunStatus.SUCCESS) {
                outputData.put("result", "Generated content successfully");
                outputData.put("tokenCount", 850);
            }

            N8nNodeRunDto run = N8nNodeRunDto.builder()
                    .id((long) i)
                    .userId(userId)
                    .workflowId(workflow[0])
                    .workflowName(workflow[1])
                    .executionId("exec_" + String.format("%03d", i))
                    .nodeId(node[0])
                    .nodeName(node[1])
                    .nodeType(node[2])
                    .status(status)
                    .startTime(startTime)
                    .endTime(endTime)
                    .duration(duration)
                    .inputData(inputData)
                    .outputData(outputData)
                    .errorMessage(status == N8nNodeRun.N8nNodeRunStatus.FAILED ? "Mock error message" : null)
                    .retryCount(status == N8nNodeRun.N8nNodeRunStatus.FAILED ? 1 : 0)
                    .maxRetries(3)
                    .createdAt(startTime)
                    .updatedAt(endTime != null ? endTime : startTime)
                    .build();

            mockRuns.add(run);
        }

        return mockRuns;
    }
}