package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan Obligation Entity - Tracks credit/loan obligations for SMEs
 */
@Entity
@Table(name = "loan_obligations", indexes = {
        @Index(name = "idx_lo_sme", columnList = "sme_business_id"),
        @Index(name = "idx_lo_status", columnList = "loan_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Column(name = "lender_name", nullable = false, length = 200)
    private String lenderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "lender_type", length = 30)
    private LenderType lenderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 50)
    private LoanType loanType;

    @Column(name = "loan_account_number", length = 50)
    private String loanAccountNumber;

    // Principal & Interest
    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // Annual percentage

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", length = 20)
    @Builder.Default
    private InterestType interestType = InterestType.REDUCING;

    // EMI Details
    @Column(name = "emi_amount", precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "remaining_tenure_months")
    private Integer remainingTenureMonths;

    // Balances
    @Column(name = "disbursed_amount", precision = 15, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "total_interest_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(name = "total_principal_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    // Dates
    @Column(name = "sanction_date")
    private LocalDate sanctionDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_status", nullable = false, length = 20)
    @Builder.Default
    private LoanStatus loanStatus = LoanStatus.ACTIVE;

    @Column(name = "overdue_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    @Column(name = "overdue_days")
    @Builder.Default
    private Integer overdueDays = 0;

    // Collateral
    @Column(name = "is_secured")
    @Builder.Default
    private Boolean isSecured = false;

    @Column(name = "collateral_details", length = 500)
    private String collateralDetails;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum LenderType {
        PUBLIC_SECTOR_BANK,
        PRIVATE_BANK,
        NBFC,
        COOPERATIVE_BANK,
        MICROFINANCE,
        FINTECH,
        OTHER
    }

    public enum LoanType {
        TERM_LOAN,
        WORKING_CAPITAL,
        OVERDRAFT,
        EQUIPMENT_FINANCE,
        INVOICE_FINANCING,
        MSME_LOAN,
        MUDRA_LOAN,
        CREDIT_LINE,
        VEHICLE_LOAN,
        PROPERTY_LOAN,
        OTHER
    }

    public enum InterestType {
        FIXED,
        FLOATING,
        REDUCING
    }

    public enum LoanStatus {
        APPLIED,
        SANCTIONED,
        ACTIVE,
        OVERDUE,
        NPA,
        CLOSED,
        RESTRUCTURED,
        WRITTEN_OFF
    }
}
