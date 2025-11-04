package ai.content.auto.service.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Provider cost summary data
 */
@Data
@Builder
public class ProviderCostSummary {
    private String providerName;
    private BigDecimal dailyCost;
    private BigDecimal monthlyCost;
    private BigDecimal totalCost;
    private BigDecimal dailyBudget;
    private BigDecimal monthlyBudget;
    private BigDecimal dailyBudgetUsagePercent;
    private BigDecimal monthlyBudgetUsagePercent;
    private boolean dailyBudgetExceeded;
    private boolean monthlyBudgetExceeded;
    private boolean dailyBudgetWarning;
    private boolean monthlyBudgetWarning;

    /**
     * Check if daily budget is exceeded
     */
    public boolean isDailyBudgetExceeded() {
        return dailyCost.compareTo(dailyBudget) > 0;
    }

    /**
     * Check if monthly budget is exceeded
     */
    public boolean isMonthlyBudgetExceeded() {
        return monthlyCost.compareTo(monthlyBudget) > 0;
    }

    /**
     * Check if daily budget warning threshold is reached (80%)
     */
    public boolean isDailyBudgetWarning() {
        return dailyCost.compareTo(dailyBudget.multiply(BigDecimal.valueOf(0.8))) > 0;
    }

    /**
     * Check if monthly budget warning threshold is reached (90%)
     */
    public boolean isMonthlyBudgetWarning() {
        return monthlyCost.compareTo(monthlyBudget.multiply(BigDecimal.valueOf(0.9))) > 0;
    }
}