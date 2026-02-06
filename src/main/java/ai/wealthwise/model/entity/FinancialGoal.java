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
 * Financial Goal Entity - Represents user financial goals
 * 
 * Tracks savings goals like home purchase, education, retirement, etc.
 */
@Entity
@Table(name = "financial_goals", indexes = {
        @Index(name = "idx_goal_user_status", columnList = "user_id, status"),
        @Index(name = "idx_goal_target_date", columnList = "target_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GoalType goalType;

    @Column(name = "target_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", precision = 14, scale = 2, columnDefinition = "DECIMAL(14,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'IN_PROGRESS'")
    @Builder.Default
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'MEDIUM'")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "monthly_contribution", precision = 12, scale = 2)
    private BigDecimal monthlyContribution;

    @Column(name = "is_automated", columnDefinition = "BOOLEAN DEFAULT false")
    @Builder.Default
    private Boolean isAutomated = false;

    @Column(name = "icon_url")
    private String iconUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ==================== Enums ====================

    public enum GoalType {
        HOME_PURCHASE("Home Purchase"),
        EDUCATION("Education"),
        RETIREMENT("Retirement"),
        EMERGENCY_FUND("Emergency Fund"),
        VACATION("Vacation/Travel"),
        VEHICLE("Vehicle Purchase"),
        WEDDING("Wedding"),
        BUSINESS("Business/Startup"),
        DEBT_REPAYMENT("Debt Repayment"),
        INVESTMENT("Investment"),
        OTHER("Other");

        private final String displayName;

        GoalType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum GoalStatus {
        IN_PROGRESS,
        COMPLETED,
        PAUSED,
        ABANDONED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // ==================== Helper Methods ====================

    /**
     * Calculate progress percentage
     */
    public BigDecimal getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.divide(targetAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Calculate remaining amount
     */
    public BigDecimal getRemainingAmount() {
        return targetAmount.subtract(currentAmount);
    }

    /**
     * Check if goal is completed
     */
    public boolean isCompleted() {
        return currentAmount.compareTo(targetAmount) >= 0;
    }
}
