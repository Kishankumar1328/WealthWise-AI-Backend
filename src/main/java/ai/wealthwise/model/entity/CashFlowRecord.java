package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cash Flow Record Entity - Tracks daily/weekly cash inflows and outflows
 */
@Entity
@Table(name = "cash_flow_records", indexes = {
        @Index(name = "idx_cf_sme", columnList = "sme_business_id"),
        @Index(name = "idx_cf_date", columnList = "record_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 20)
    @Builder.Default
    private PeriodType periodType = PeriodType.DAILY;

    // Cash Inflows
    @Column(name = "sales_receipts", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal salesReceipts = BigDecimal.ZERO;

    @Column(name = "other_income", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherIncome = BigDecimal.ZERO;

    @Column(name = "loan_receipts", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal loanReceipts = BigDecimal.ZERO;

    @Column(name = "total_inflow", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInflow = BigDecimal.ZERO;

    // Cash Outflows
    @Column(name = "supplier_payments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal supplierPayments = BigDecimal.ZERO;

    @Column(name = "salary_wages", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal salaryWages = BigDecimal.ZERO;

    @Column(name = "rent_utilities", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal rentUtilities = BigDecimal.ZERO;

    @Column(name = "loan_repayments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal loanRepayments = BigDecimal.ZERO;

    @Column(name = "tax_payments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxPayments = BigDecimal.ZERO;

    @Column(name = "other_expenses", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherExpenses = BigDecimal.ZERO;

    @Column(name = "total_outflow", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalOutflow = BigDecimal.ZERO;

    // Net Position
    @Column(name = "net_cash_flow", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netCashFlow = BigDecimal.ZERO;

    @Column(name = "opening_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 30)
    @Builder.Default
    private DataSource source = DataSource.MANUAL;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== Enums ====================

    public enum PeriodType {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    public enum DataSource {
        MANUAL,
        BANK_API,
        IMPORTED,
        CALCULATED
    }
}
