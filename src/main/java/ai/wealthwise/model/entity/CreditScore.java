package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Credit Score Entity - AI-calculated creditworthiness assessment for SMEs
 */
@Entity
@Table(name = "credit_scores", indexes = {
        @Index(name = "idx_cs_sme", columnList = "sme_business_id"),
        @Index(name = "idx_cs_assessed_at", columnList = "assessed_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    // Overall Score (0-900, similar to CIBIL)
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    // Component Scores (0-100 each)
    @Column(name = "payment_history_score")
    private Integer paymentHistoryScore; // 40% weight

    @Column(name = "credit_utilization_score")
    private Integer creditUtilizationScore; // 20% weight

    @Column(name = "business_age_score")
    private Integer businessAgeScore; // 15% weight

    @Column(name = "financial_health_score")
    private Integer financialHealthScore; // 15% weight

    @Column(name = "compliance_score")
    private Integer complianceScore; // 10% weight

    // Financial Ratios Used
    @Column(name = "current_ratio", precision = 5, scale = 2)
    private BigDecimal currentRatio;

    @Column(name = "debt_equity_ratio", precision = 5, scale = 2)
    private BigDecimal debtEquityRatio;

    @Column(name = "interest_coverage_ratio", precision = 5, scale = 2)
    private BigDecimal interestCoverageRatio;

    @Column(name = "profit_margin", precision = 5, scale = 2)
    private BigDecimal profitMargin;

    @Column(name = "asset_turnover_ratio", precision = 5, scale = 2)
    private BigDecimal assetTurnoverRatio;

    // Risk Assessment
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors; // JSON array of risk factors

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations; // JSON array of recommendations

    // Loan Eligibility
    @Column(name = "max_loan_eligible", precision = 15, scale = 2)
    private BigDecimal maxLoanEligible;

    @Column(name = "suggested_interest_rate", precision = 5, scale = 2)
    private BigDecimal suggestedInterestRate;

    // Metadata
    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "assessment_version", length = 20)
    @Builder.Default
    private String assessmentVersion = "1.0";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum RiskLevel {
        EXCELLENT, // Score 750-900
        GOOD, // Score 650-749
        FAIR, // Score 550-649
        POOR, // Score 400-549
        CRITICAL // Score 0-399
    }

    // ==================== Helper Methods ====================

    public static RiskLevel calculateRiskLevel(int score) {
        if (score >= 750)
            return RiskLevel.EXCELLENT;
        if (score >= 650)
            return RiskLevel.GOOD;
        if (score >= 550)
            return RiskLevel.FAIR;
        if (score >= 400)
            return RiskLevel.POOR;
        return RiskLevel.CRITICAL;
    }
}
