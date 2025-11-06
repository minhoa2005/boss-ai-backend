package ai.content.auto.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * N8N Node Run Statistics DTO
 * Contains aggregated statistics for N8N node runs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class N8nNodeRunStatisticsDto {

    private Long totalRuns;
    private Long successfulRuns;
    private Long failedRuns;
    private Long runningRuns;
    private Double averageDuration; // Average duration in milliseconds
    private Double successRate; // Success rate as decimal (0.0 to 1.0)
}