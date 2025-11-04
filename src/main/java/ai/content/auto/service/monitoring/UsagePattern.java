package ai.content.auto.service.monitoring;

import lombok.Builder;
import lombok.Data;

/**
 * Usage pattern information
 */
@Data
@Builder
public class UsagePattern {
    private String metric;
    private double trend; // slope of trend line
    private double average;
    private double variance;
    private String seasonality; // DAILY, WEEKLY, MONTHLY, NONE
}