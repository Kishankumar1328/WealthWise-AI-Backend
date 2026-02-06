package ai.wealthwise.model.dto.sme;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingCapitalResponse {
    private Long businessId;

    // Core Metrics
    private Integer cashConversionCycle; // in days
    private Integer daysInventoryOutstanding;
    private Integer daysSalesOutstanding;
    private Integer daysPayablesOutstanding;

    // Optimization Targets
    private Integer targetCashConversionCycle;
    private BigDecimal potentialCashUnlocking;

    // Receivables Aging
    private List<AgingBucket> receivablesAging;
    private List<OptimizationRecommendation> receivablesRecommendations;

    // Payables Optimization
    private List<ScheduledPayment> optimalPaymentSchedule;

    // Inventory Optimization
    private List<OptimizationRecommendation> inventoryRecommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingBucket {
        private String range; // e.g., "0-30", "31-60"
        private BigDecimal amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationRecommendation {
        private String title;
        private String description;
        private String impact; // e.g., "HIGH", "MEDIUM"
        private String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduledPayment {
        private String invoiceNumber;
        private String vendorName;
        private BigDecimal amount;
        private String dueDate;
        private String suggestedPaymentDate;
        private String reasoning;
    }
}
