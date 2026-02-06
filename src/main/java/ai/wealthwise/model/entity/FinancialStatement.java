package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Financial Statement Entity - Stores parsed financial data from uploaded
 * documents
 */
@Entity
@Table(name = "financial_statements", indexes = {
        @Index(name = "idx_fs_sme", columnList = "sme_business_id"),
        @Index(name = "idx_fs_fiscal_year", columnList = "fiscal_year")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 30)
    private StatementType statementType;

    @Column(name = "fiscal_year", nullable = false, length = 9)
    private String fiscalYear; // e.g., "2025-2026"

    @Column(name = "quarter", length = 2)
    private String quarter; // Q1, Q2, Q3, Q4

    // Revenue & Expenses
    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "cost_of_goods_sold", precision = 15, scale = 2)
    private BigDecimal costOfGoodsSold;

    @Column(name = "gross_profit", precision = 15, scale = 2)
    private BigDecimal grossProfit;

    @Column(name = "operating_expenses", precision = 15, scale = 2)
    private BigDecimal operatingExpenses;

    @Column(name = "operating_income", precision = 15, scale = 2)
    private BigDecimal operatingIncome;

    @Column(name = "interest_expense", precision = 15, scale = 2)
    private BigDecimal interestExpense;

    @Column(name = "tax_expense", precision = 15, scale = 2)
    private BigDecimal taxExpense;

    @Column(name = "net_profit", precision = 15, scale = 2)
    private BigDecimal netProfit;

    // Balance Sheet Items
    @Column(name = "total_assets", precision = 15, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "current_assets", precision = 15, scale = 2)
    private BigDecimal currentAssets;

    @Column(name = "fixed_assets", precision = 15, scale = 2)
    private BigDecimal fixedAssets;

    @Column(name = "total_liabilities", precision = 15, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "current_liabilities", precision = 15, scale = 2)
    private BigDecimal currentLiabilities;

    @Column(name = "long_term_debt", precision = 15, scale = 2)
    private BigDecimal longTermDebt;

    @Column(name = "equity", precision = 15, scale = 2)
    private BigDecimal equity;

    // Inventory & Receivables
    @Column(name = "inventory", precision = 15, scale = 2)
    private BigDecimal inventory;

    @Column(name = "accounts_receivable", precision = 15, scale = 2)
    private BigDecimal accountsReceivable;

    @Column(name = "accounts_payable", precision = 15, scale = 2)
    private BigDecimal accountsPayable;

    // Raw data storage (JSON)
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private FinancialDocument sourceDocument;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum StatementType {
        BALANCE_SHEET,
        PROFIT_LOSS,
        CASH_FLOW,
        TRIAL_BALANCE
    }
}
