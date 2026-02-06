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
 * Tax Compliance Entity - Tracks GST filings and tax compliance status
 */
@Entity
@Table(name = "tax_compliance", indexes = {
        @Index(name = "idx_tc_sme", columnList = "sme_business_id"),
        @Index(name = "idx_tc_filing_period", columnList = "filing_period"),
        @Index(name = "idx_tc_due_date", columnList = "due_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_type", nullable = false, length = 20)
    private FilingType filingType;

    @Column(name = "filing_period", nullable = false, length = 20)
    private String filingPeriod; // e.g., "Jan-2026", "Q3-2025", "FY2025-26"

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "filed_date")
    private LocalDate filedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", nullable = false, length = 20)
    @Builder.Default
    private FilingStatus filingStatus = FilingStatus.PENDING;

    // Tax Amounts
    @Column(name = "tax_liability", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxLiability = BigDecimal.ZERO;

    @Column(name = "input_tax_credit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal inputTaxCredit = BigDecimal.ZERO;

    @Column(name = "net_tax_payable", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netTaxPayable = BigDecimal.ZERO;

    @Column(name = "tax_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxPaid = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;

    // Filing Details
    @Column(name = "arn_number", length = 50)
    private String arnNumber; // Acknowledgement Reference Number

    @Column(name = "filing_reference", length = 100)
    private String filingReference;

    // Compliance Score (0-100)
    @Column(name = "compliance_score")
    private Integer complianceScore;

    @Column(name = "days_delayed")
    private Integer daysDelayed;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum FilingType {
        GSTR1, // Outward supplies
        GSTR3B, // Monthly summary
        GSTR9, // Annual return
        GSTR9C, // Reconciliation
        TDS, // Tax Deducted at Source
        ADVANCE_TAX, // Quarterly advance tax
        ITR // Income Tax Return
    }

    public enum FilingStatus {
        PENDING,
        FILED_ON_TIME,
        FILED_LATE,
        OVERDUE,
        UNDER_REVIEW,
        REJECTED,
        AMENDED
    }
}
