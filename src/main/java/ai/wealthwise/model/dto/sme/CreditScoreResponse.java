package ai.wealthwise.model.dto.sme;

import ai.wealthwise.model.entity.CreditScore;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditScoreResponse {
    private Long id;
    private Long smeBusinessId;
    private String businessName;

    // Main Score
    private Integer overallScore;
    private CreditScore.RiskLevel riskLevel;
    private String riskLevelDescription;

    // Component Scores
    private Integer paymentHistoryScore;
    private Integer creditUtilizationScore;
    private Integer businessAgeScore;
    private Integer financialHealthScore;
    private Integer complianceScore;

    // Financial Ratios
    private BigDecimal currentRatio;
    private BigDecimal debtEquityRatio;
    private BigDecimal interestCoverageRatio;
    private BigDecimal profitMargin;

    // Loan Eligibility
    private BigDecimal maxLoanEligible;
    private BigDecimal suggestedInterestRate;

    // Risk & Recommendations
    private List<String> riskFactors;
    private List<String> recommendations;

    // Metadata
    private LocalDateTime assessedAt;
    private LocalDateTime validUntil;

    public static CreditScoreResponse fromEntity(CreditScore entity) {
        String riskDescription = switch (entity.getRiskLevel()) {
            case EXCELLENT -> "Excellent credit profile. Eligible for best rates.";
            case GOOD -> "Good credit standing. Competitive rates available.";
            case FAIR -> "Fair credit score. Some limitations may apply.";
            case POOR -> "Below average. Improvement needed for better terms.";
            case CRITICAL -> "High risk. Immediate financial restructuring recommended.";
        };

        return CreditScoreResponse.builder()
                .id(entity.getId())
                .smeBusinessId(entity.getSmeBusiness().getId())
                .businessName(entity.getSmeBusiness().getBusinessName())
                .overallScore(entity.getOverallScore())
                .riskLevel(entity.getRiskLevel())
                .riskLevelDescription(riskDescription)
                .paymentHistoryScore(entity.getPaymentHistoryScore())
                .creditUtilizationScore(entity.getCreditUtilizationScore())
                .businessAgeScore(entity.getBusinessAgeScore())
                .financialHealthScore(entity.getFinancialHealthScore())
                .complianceScore(entity.getComplianceScore())
                .currentRatio(entity.getCurrentRatio())
                .debtEquityRatio(entity.getDebtEquityRatio())
                .interestCoverageRatio(entity.getInterestCoverageRatio())
                .profitMargin(entity.getProfitMargin())
                .maxLoanEligible(entity.getMaxLoanEligible())
                .suggestedInterestRate(entity.getSuggestedInterestRate())
                .assessedAt(entity.getAssessedAt())
                .validUntil(entity.getValidUntil())
                .build();
    }
}
