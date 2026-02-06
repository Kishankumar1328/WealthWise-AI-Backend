package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a transaction extracted from uploaded financial documents
 * (bank statements, invoices, etc.)
 */
@Entity
@Table(name = "parsed_transactions", indexes = {
        @Index(name = "idx_parsed_tx_document", columnList = "document_id"),
        @Index(name = "idx_parsed_tx_business", columnList = "business_id"),
        @Index(name = "idx_parsed_tx_date", columnList = "transaction_date"),
        @Index(name = "idx_parsed_tx_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private FinancialDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType transactionType;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "running_balance", precision = 15, scale = 2)
    private BigDecimal runningBalance;

    // AI-categorized fields
    @Column(length = 50)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    // Vendor/Party information
    @Column(name = "party_name", length = 200)
    private String partyName;

    @Column(name = "party_account", length = 50)
    private String partyAccount;

    // Tax-related
    @Column(name = "gst_amount", precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "tds_amount", precision = 15, scale = 2)
    private BigDecimal tdsAmount;

    @Column(name = "is_tax_deductible")
    @Builder.Default
    private Boolean isTaxDeductible = false;

    // Duplicate Detection (Module 6)
    @Column(name = "is_possible_duplicate")
    @Builder.Default
    private Boolean isPossibleDuplicate = false;

    @Column(name = "duplicate_group_id")
    private String duplicateGroupId; // UUID or hash grouping similar txs

    // Metadata
    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    @Column(name = "raw_text", length = 1000)
    private String rawText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionType {
        CREDIT, // Money coming in
        DEBIT, // Money going out
        TRANSFER, // Internal transfer
        REVERSAL, // Reversal of previous transaction
        INTEREST, // Interest earned/paid
        CHARGES, // Bank charges
        TAX // Tax deducted
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
