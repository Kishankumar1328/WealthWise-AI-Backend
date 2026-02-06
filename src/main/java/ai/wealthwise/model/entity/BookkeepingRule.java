package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for Automated Bookkeeping Rules
 * Module 6: Automated Bookkeeping Assistance
 */
@Entity
@Table(name = "bookkeeping_rules", indexes = {
        @Index(name = "idx_rule_business", columnList = "business_id"),
        @Index(name = "idx_rule_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookkeepingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description")
    private String description;

    // Conditions
    @Column(name = "keyword_pattern")
    private String keywordPattern; // Regex or simple contains match

    @Column(name = "amount_min", precision = 15, scale = 2)
    private BigDecimal amountMin;

    @Column(name = "amount_max", precision = 15, scale = 2)
    private BigDecimal amountMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private ParsedTransaction.TransactionType transactionType;

    // Actions
    @Column(name = "target_category", nullable = false)
    private String targetCategory;

    @Column(name = "target_sub_category")
    private String targetSubCategory;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher = applied first

    @Column(name = "last_applied_at")
    private LocalDateTime lastAppliedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
