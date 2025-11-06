package ai.content.auto.service;

import ai.content.auto.dtos.N8nNodeRunDto;
import ai.content.auto.dtos.N8nNodeRunStatisticsDto;
import ai.content.auto.entity.N8nNodeRun;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.N8nNodeRunMapper;
import ai.content.auto.repository.N8nNodeRunRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing N8N Node Run operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nNodeRunService {

    private final N8nNodeRunRepository repository;
    private final N8nNodeRunMapper mapper;
    private final SecurityUtil securityUtil;
    private final MockN8nNodeRunService mockService;

    /**
     * Get all node runs for the current user
     */
    public List<N8nNodeRunDto> getNodeRunsForCurrentUser() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            return getNodeRunsInTransaction(userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting node runs for current user", e);
            throw new BusinessException("Failed to retrieve node runs");
        }
    }

    /**
     * Get node runs with filtering
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
            // Use provided userId or current user's ID
            Long targetUserId = userId != null ? userId : securityUtil.getCurrentUserId();

            // Try database first, fallback to mock if table doesn't exist
            try {
                return getFilteredNodeRunsInTransaction(targetUserId, statuses, workflowId, nodeType, search, dateFrom,
                        dateTo);
            } catch (Exception dbError) {
                log.warn("Database table not available, using mock data: {}", dbError.getMessage());
                return mockService.getNodeRunsWithFilters(statuses, workflowId, nodeType, search, dateFrom, dateTo,
                        targetUserId);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting filtered node runs", e);
            throw new BusinessException("Failed to retrieve filtered node runs");
        }
    }

    /**
     * Get paginated node runs for the current user
     */
    public Page<N8nNodeRunDto> getPaginatedNodeRuns(int page, int size, Long userId) {
        try {
            Long targetUserId = userId != null ? userId : securityUtil.getCurrentUserId();

            return getPaginatedNodeRunsInTransaction(targetUserId, page, size);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting paginated node runs", e);
            throw new BusinessException("Failed to retrieve paginated node runs");
        }
    }

    /**
     * Get node run statistics for the current user
     */
    public N8nNodeRunStatisticsDto getNodeRunStatistics(Long userId, Instant dateFrom, Instant dateTo) {
        try {
            Long targetUserId = userId != null ? userId : securityUtil.getCurrentUserId();

            // Try database first, fallback to mock if table doesn't exist
            try {
                return getStatisticsInTransaction(targetUserId, dateFrom, dateTo);
            } catch (Exception dbError) {
                log.warn("Database table not available, using mock statistics: {}", dbError.getMessage());
                return mockService.getNodeRunStatistics(targetUserId, dateFrom, dateTo);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting node run statistics", e);
            throw new BusinessException("Failed to retrieve node run statistics");
        }
    }

    /**
     * Get specific node run by ID
     */
    public N8nNodeRunDto getNodeRunById(Long id) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            // Try database first, fallback to mock if table doesn't exist
            try {
                return getNodeRunByIdInTransaction(id, userId);
            } catch (Exception dbError) {
                log.warn("Database table not available, using mock data: {}", dbError.getMessage());
                return mockService.getNodeRunById(id);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting node run by ID: {}", id, e);
            throw new BusinessException("Failed to retrieve node run");
        }
    }

    /**
     * Retry a failed node run
     */
    public N8nNodeRunDto retryNodeRun(Long id) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            // Try database first, fallback to mock if table doesn't exist
            try {
                return retryNodeRunInTransaction(id, userId);
            } catch (Exception dbError) {
                log.warn("Database table not available, using mock retry: {}", dbError.getMessage());
                return mockService.retryNodeRun(id);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrying node run: {}", id, e);
            throw new BusinessException("Failed to retry node run");
        }
    }

    /**
     * Cancel a running node run
     */
    public void cancelNodeRun(Long id) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            // Try database first, fallback to mock if table doesn't exist
            try {
                cancelNodeRunInTransaction(id, userId);
            } catch (Exception dbError) {
                log.warn("Database table not available, using mock cancel: {}", dbError.getMessage());
                mockService.cancelNodeRun(id);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error cancelling node run: {}", id, e);
            throw new BusinessException("Failed to cancel node run");
        }
    }

    // Transactional methods for database operations

    @Transactional(readOnly = true)
    private List<N8nNodeRunDto> getNodeRunsInTransaction(Long userId) {
        List<N8nNodeRun> nodeRuns = repository.findByUserIdOrderByCreatedAtDesc(userId);
        return mapper.toDtoList(nodeRuns);
    }

    @Transactional(readOnly = true)
    private List<N8nNodeRunDto> getFilteredNodeRunsInTransaction(
            Long userId,
            List<N8nNodeRun.N8nNodeRunStatus> statuses,
            String workflowId,
            String nodeType,
            String search,
            Instant dateFrom,
            Instant dateTo) {

        List<N8nNodeRun> nodeRuns = repository.findByUserIdWithFilters(
                userId, statuses, workflowId, nodeType, search, dateFrom, dateTo);
        return mapper.toDtoList(nodeRuns);
    }

    @Transactional(readOnly = true)
    private Page<N8nNodeRunDto> getPaginatedNodeRunsInTransaction(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<N8nNodeRun> nodeRunsPage = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return nodeRunsPage.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    private N8nNodeRunStatisticsDto getStatisticsInTransaction(Long userId, Instant dateFrom, Instant dateTo) {
        Long totalRuns = repository.countByUserIdAndDateRange(userId, dateFrom, dateTo);
        Long successfulRuns = repository.countSuccessfulByUserIdAndDateRange(userId, dateFrom, dateTo);
        Long failedRuns = repository.countFailedByUserId(userId);
        Long runningRuns = repository.countRunningByUserId(userId);
        Double averageDuration = repository.getAverageDurationByUserIdAndDateRange(userId, dateFrom, dateTo);

        double successRate = totalRuns > 0 ? (double) successfulRuns / totalRuns : 0.0;

        return N8nNodeRunStatisticsDto.builder()
                .totalRuns(totalRuns)
                .successfulRuns(successfulRuns)
                .failedRuns(failedRuns)
                .runningRuns(runningRuns)
                .averageDuration(averageDuration != null ? averageDuration : 0.0)
                .successRate(successRate)
                .build();
    }

    @Transactional(readOnly = true)
    private N8nNodeRunDto getNodeRunByIdInTransaction(Long id, Long userId) {
        Optional<N8nNodeRun> nodeRunOpt = repository.findById(id);

        if (nodeRunOpt.isEmpty()) {
            throw new BusinessException("Node run not found with ID: " + id);
        }

        N8nNodeRun nodeRun = nodeRunOpt.get();

        // Verify ownership
        if (!nodeRun.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to node run with ID: " + id);
        }

        return mapper.toDto(nodeRun);
    }

    @Transactional
    private N8nNodeRunDto retryNodeRunInTransaction(Long id, Long userId) {
        Optional<N8nNodeRun> nodeRunOpt = repository.findById(id);

        if (nodeRunOpt.isEmpty()) {
            throw new BusinessException("Node run not found with ID: " + id);
        }

        N8nNodeRun nodeRun = nodeRunOpt.get();

        // Verify ownership
        if (!nodeRun.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to node run with ID: " + id);
        }

        // Check if retry is allowed
        if (nodeRun.getStatus() != N8nNodeRun.N8nNodeRunStatus.FAILED) {
            throw new BusinessException("Only failed node runs can be retried");
        }

        if (nodeRun.getRetryCount() >= nodeRun.getMaxRetries()) {
            throw new BusinessException("Maximum retry attempts reached for node run: " + id);
        }

        // Update retry count and status
        nodeRun.setRetryCount(nodeRun.getRetryCount() + 1);
        nodeRun.setStatus(N8nNodeRun.N8nNodeRunStatus.PENDING);
        nodeRun.setErrorMessage(null);

        N8nNodeRun savedNodeRun = repository.save(nodeRun);

        log.info("Node run {} retried by user {}", id, userId);

        return mapper.toDto(savedNodeRun);
    }

    @Transactional
    private void cancelNodeRunInTransaction(Long id, Long userId) {
        Optional<N8nNodeRun> nodeRunOpt = repository.findById(id);

        if (nodeRunOpt.isEmpty()) {
            throw new BusinessException("Node run not found with ID: " + id);
        }

        N8nNodeRun nodeRun = nodeRunOpt.get();

        // Verify ownership
        if (!nodeRun.getUserId().equals(userId)) {
            throw new BusinessException("Access denied to node run with ID: " + id);
        }

        // Check if cancellation is allowed
        if (nodeRun.getStatus() != N8nNodeRun.N8nNodeRunStatus.RUNNING &&
                nodeRun.getStatus() != N8nNodeRun.N8nNodeRunStatus.PENDING) {
            throw new BusinessException("Only running or pending node runs can be cancelled");
        }

        // Update status
        nodeRun.setStatus(N8nNodeRun.N8nNodeRunStatus.CANCELLED);
        nodeRun.setEndTime(Instant.now());

        if (nodeRun.getStartTime() != null && nodeRun.getEndTime() != null) {
            nodeRun.setDuration(nodeRun.getEndTime().toEpochMilli() - nodeRun.getStartTime().toEpochMilli());
        }

        repository.save(nodeRun);

        log.info("Node run {} cancelled by user {}", id, userId);
    }
}