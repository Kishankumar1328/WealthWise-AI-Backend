package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for Cost Optimization Suggestions
 * Module 7: Cost Optimization Strategies
 */
@Entity
@Table(name = "cost_optimization_suggestions", indexes = {
        @Index(name = "idx_opt_business", columnList = "business_id"),
        @Index(name = "idx_opt_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostOptimizationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @Column(name = "expense_category", nullable = false)
    private String expenseCategory;

    @Column(name = "current_monthly_spend", precision = 15, scale = 2)
    private BigDecimal currentMonthlySpend;

    @Column(name = "projected_monthly_saving", precision = 15, scale = 2)
    private BigDecimal projectedMonthlySaving;

    @Column(name = "suggestion_title", nullable = false)
    private String suggestionTitle;

    @Column(name = "suggestion_details", length = 1000)
    private String suggestionDetails;

    @Column(name = "action_type")
    @Enumerated(EnumType.STRING)
    private ActionType actionType; // e.g. NEGOTIATE, SWITCH_VENDOR, CUT_USAGE

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private Priority priority; // HIGH, MEDIUM, LOW

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;

    public enum Status {
        NEW,
        VIEWED,
        IMPLEMENTED,
        DISMISSED,
        IN_PROGRESS
    }

    public enum ActionType {
        NEGOTIATE,
        SWITCH_VENDOR,
        REDUCE_CONSUMPTION,
        SWITCH_PLAN,
        CONSOLIDATE
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
