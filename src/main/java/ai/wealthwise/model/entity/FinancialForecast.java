package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_forecasts", indexes = {
        @Index(name = "idx_forecast_business", columnList = "sme_business_id"),
        @Index(name = "idx_forecast_date", columnList = "forecast_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sme_business_id", nullable = false)
    private SmeBusiness smeBusiness;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate; // The future date being predicted

    @Column(name = "predicted_revenue", precision = 15, scale = 2)
    private BigDecimal predictedRevenue;

    @Column(name = "predicted_expense", precision = 15, scale = 2)
    private BigDecimal predictedExpense;

    @Column(name = "predicted_cash_flow", precision = 15, scale = 2)
    private BigDecimal predictedCashFlow;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "lower_bound", precision = 15, scale = 2)
    private BigDecimal lowerBound; // Lower confidence interval

    @Column(name = "upper_bound", precision = 15, scale = 2)
    private BigDecimal upperBound; // Upper confidence interval

    @Column(name = "explanation", length = 1000)
    private String explanation; // Plain-English driver explanation

    @Column(name = "risk_flag")
    private Boolean riskFlag; // High risk indicators (e.g., negative cash flow probability)

    @Column(name = "seasonal_impact")
    private Double seasonalImpact; // -1.0 to 1.0 (multiplier/factor)

    @Column(name = "forecast_type", length = 20)
    private String forecastType; // e.g., DAILY, MONTHLY

    @Column(name = "trend_factor")
    private Double trendFactor; // Upward/Downward trend indicator

    @Column(name = "model_version", length = 50)
    private String modelVersion; // Which AI model version generated this

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
