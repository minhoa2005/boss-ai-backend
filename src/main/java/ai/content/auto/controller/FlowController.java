package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.N8nNodeRunDto;
import ai.content.auto.dtos.N8nNodeRunStatisticsDto;
import ai.content.auto.entity.N8nNodeRun;
import ai.content.auto.service.N8nNodeRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Flow Controller
 * Handles N8N node run monitoring and management endpoints
 */
@RestController
@RequestMapping("/api/v1/flow")
@RequiredArgsConstructor
@Slf4j
public class FlowController {

    private final N8nNodeRunService nodeRunService;

    /**
     * Get N8N node runs for the current user with optional filtering
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<N8nNodeRunDto>>> getNodeRuns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        log.info("Getting node runs with filters - status: {}, workflowId: {}, nodeType: {}, search: {}",
                status, workflowId, nodeType, search);

        List<N8nNodeRun.N8nNodeRunStatus> statuses = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statuses = Arrays.stream(status.split(","))
                        .map(String::trim)
                        .map(N8nNodeRun.N8nNodeRunStatus::valueOf)
                        .toList();
            } catch (IllegalArgumentException e) {
                BaseResponse<List<N8nNodeRunDto>> response = new BaseResponse<List<N8nNodeRunDto>>()
                        .setErrorCode("INVALID_STATUS")
                        .setErrorMessage("Invalid status value: " + status);
                return ResponseEntity.badRequest().body(response);
            }
        }

        List<N8nNodeRunDto> nodeRuns = nodeRunService.getNodeRunsWithFilters(
                statuses, workflowId, nodeType, search, dateFrom, dateTo, userId);

        BaseResponse<List<N8nNodeRunDto>> response = new BaseResponse<List<N8nNodeRunDto>>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Node runs retrieved successfully")
                .setData(nodeRuns);

        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated N8N node runs
     */
    @GetMapping("/paginated")
    public ResponseEntity<BaseResponse<Page<N8nNodeRunDto>>> getPaginatedNodeRuns(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) Long userId) {

        log.info("Getting paginated node runs - page: {}, size: {}", page, size);

        Page<N8nNodeRunDto> nodeRunsPage = nodeRunService.getPaginatedNodeRuns(page, size, userId);

        BaseResponse<Page<N8nNodeRunDto>> response = new BaseResponse<Page<N8nNodeRunDto>>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Paginated node runs retrieved successfully")
                .setData(nodeRunsPage);

        return ResponseEntity.ok(response);
    }

    /**
     * Get N8N node run statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<BaseResponse<N8nNodeRunStatisticsDto>> getNodeRunStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        log.info("Getting node run statistics for user: {}", userId);

        N8nNodeRunStatisticsDto statistics = nodeRunService.getNodeRunStatistics(userId, dateFrom, dateTo);

        BaseResponse<N8nNodeRunStatisticsDto> response = new BaseResponse<N8nNodeRunStatisticsDto>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Node run statistics retrieved successfully")
                .setData(statistics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific N8N node run by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<N8nNodeRunDto>> getNodeRunById(@PathVariable Long id) {
        log.info("Getting node run by ID: {}", id);

        N8nNodeRunDto nodeRun = nodeRunService.getNodeRunById(id);

        BaseResponse<N8nNodeRunDto> response = new BaseResponse<N8nNodeRunDto>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Node run retrieved successfully")
                .setData(nodeRun);

        return ResponseEntity.ok(response);
    }

    /**
     * Retry a failed N8N node run
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<BaseResponse<N8nNodeRunDto>> retryNodeRun(@PathVariable Long id) {
        log.info("Retrying node run with ID: {}", id);

        N8nNodeRunDto nodeRun = nodeRunService.retryNodeRun(id);

        BaseResponse<N8nNodeRunDto> response = new BaseResponse<N8nNodeRunDto>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Node run retry initiated successfully")
                .setData(nodeRun);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a running N8N node run
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BaseResponse<Void>> cancelNodeRun(@PathVariable Long id) {
        log.info("Cancelling node run with ID: {}", id);

        nodeRunService.cancelNodeRun(id);

        BaseResponse<Void> response = new BaseResponse<Void>()
                .setErrorCode("SUCCESS")
                .setErrorMessage("Node run cancelled successfully");

        return ResponseEntity.ok(response);
    }
}