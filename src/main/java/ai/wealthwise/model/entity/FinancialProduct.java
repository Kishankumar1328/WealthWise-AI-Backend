package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for Financial Products Catalog
 * Module 8: Financial Product Recommendations
 */
@Entity
@Table(name = "financial_products", indexes = {
        @Index(name = "idx_prod_type", columnList = "product_type"),
        @Index(name = "idx_prod_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", nullable = false)
    private String providerName; // Bank or Institution Name

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "interest_rate_min", precision = 5, scale = 2)
    private BigDecimal interestRateMin;

    @Column(name = "interest_rate_max", precision = 5, scale = 2)
    private BigDecimal interestRateMax;

    @Column(name = "processing_fee_percentage", precision = 5, scale = 2)
    private BigDecimal processingFeePercentage;

    @Column(name = "min_credit_score_required")
    private Integer minCreditScoreRequired;

    @Column(name = "min_annual_revenue", precision = 15, scale = 2)
    private BigDecimal minAnnualRevenue;

    @Column(name = "max_loan_amount", precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    @Column(name = "application_link")
    private String applicationLink;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson; // Stored as JSON string

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ProductType {
        TERM_LOAN,
        WORKING_CAPITAL_LOAN,
        INVOICE_DISCOUNTING,
        BUSINESS_CREDIT_CARD,
        INSURANCE_HEALTH,
        INSURANCE_BUSINESS,
        EQUIPMENT_FINANCE
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
