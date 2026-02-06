package ai.wealthwise.model.dto.sme;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comprehensive financial health dashboard response for SME
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialHealthResponse {

    // Business Overview
    private Long smeBusinessId;
    private String businessName;
    private String industryType;

    // Credit Score Summary
    private Integer creditScore;
    private String riskLevel;
    private String creditScoreChange; // e.g., "+15 from last month"

    // Cash Flow Summary
    private BigDecimal totalCashInflow;
    private BigDecimal totalCashOutflow;
    private BigDecimal netCashFlow;
    private Integer daysOfCashRunway;

    // Receivables & Payables
    private BigDecimal totalReceivables;
    private BigDecimal totalPayables;
    private BigDecimal overdueReceivables;
    private Long overdueInvoiceCount;

    // Loan Summary
    private BigDecimal totalDebt;
    private BigDecimal monthlyEmiObligation;
    private Long activeLoansCount;

    // Tax Compliance
    private Integer complianceScore;
    private Long pendingFilings;
    private Long overdueFilings;

    // Key Financial Ratios
    private BigDecimal currentRatio;
    private BigDecimal debtEquityRatio;
    private BigDecimal profitMargin;

    // Alerts & Recommendations
    private List<HealthAlert> alerts;
    private List<String> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthAlert {
        private String type; // WARNING, INFO, CRITICAL
        private String category; // CASH_FLOW, COMPLIANCE, CREDIT, etc.
        private String message;
        private String actionRequired;
    }
}
