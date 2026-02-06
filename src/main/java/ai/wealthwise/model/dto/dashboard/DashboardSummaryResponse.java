package ai.wealthwise.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private BigDecimal totalBalance;
    private BigDecimal monthlySpending;
    private BigDecimal totalInvestments;
    private int pendingBills;
    private BigDecimal totalBudget;
    private BigDecimal budgetUsagePercentage;

    private List<SpendingTrend> spendingTrends;
    private List<CategoryBreakdown> categoryBreakdown;
    private List<RecentTransaction> recentTransactions;

    @Data
    @AllArgsConstructor
    public static class SpendingTrend {
        private String name;
        private BigDecimal spent;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String name;
        private BigDecimal amount;
        private String color;
    }

    @Data
    @AllArgsConstructor
    public static class RecentTransaction {
        private Long id;
        private String description;
        private String category;
        private BigDecimal amount;
        private String date;
        private String type;
    }
}
