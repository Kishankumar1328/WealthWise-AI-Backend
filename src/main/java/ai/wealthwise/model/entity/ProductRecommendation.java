package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for AI Financial Product Recommendations
 * Module 8: Financial Product Recommendations
 */
@Entity
@Table(name = "product_recommendations", indexes = {
        @Index(name = "idx_rec_business", columnList = "business_id"),
        @Index(name = "idx_rec_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FinancialProduct product;

    @Column(name = "match_score", nullable = false)
    private Double matchScore; // 0.0 to 1.0 (or 100)

    @Column(name = "match_reason", length = 500)
    private String matchReason; // Why this fits (e.g. "Good credit score match")

    @Column(name = "estimated_eligibility_amount", precision = 15, scale = 2)
    private BigDecimal estimatedEligibilityAmount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    public enum Status {
        NEW,
        VIEWED,
        APPLIED,
        NOT_INTERESTED
    }

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
