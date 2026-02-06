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
 * Expense Entity - Represents user expenses/transactions
 * 
 * Core entity for expense tracking with auto-categorization support
 */
@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_user_date", columnList = "user_id, transaction_date"),
        @Index(name = "idx_expense_category", columnList = "category")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExpenseCategory category;

    @Column(length = 200)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String merchant;

    @Column(length = 500)
    private String notes;

    @Column(name = "is_recurring", columnDefinition = "BOOLEAN DEFAULT false")
    @Builder.Default
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'EXPENSE'")
    @Builder.Default
    private TransactionType type = TransactionType.EXPENSE;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(length = 100)
    private String tags;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Enums ====================

    public enum TransactionType {
        INCOME, EXPENSE
    }

    public enum ExpenseCategory {
        FOOD_DINING("Food & Dining"),
        GROCERIES("Groceries"),
        TRANSPORTATION("Transportation"),
        UTILITIES("Utilities"),
        HEALTHCARE("Healthcare"),
        ENTERTAINMENT("Entertainment"),
        SHOPPING("Shopping"),
        EDUCATION("Education"),
        RENT("Rent/Mortgage"),
        INSURANCE("Insurance"),
        SUBSCRIPTIONS("Subscriptions"),
        TRAVEL("Travel"),
        PERSONAL_CARE("Personal Care"),
        GIFTS("Gifts & Donations"),
        INVESTMENTS("Investments"),
        EMI("EMI/Loans"),
        OTHER("Other");

        private final String displayName;

        ExpenseCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PaymentMethod {
        CASH,
        DEBIT_CARD,
        CREDIT_CARD,
        UPI,
        NET_BANKING,
        WALLET,
        OTHER
    }
}
