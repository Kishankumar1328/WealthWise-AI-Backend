package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for Industry Benchmarks
 * Module 7: Cost Optimization Strategies
 */
@Entity
@Table(name = "industry_benchmarks", indexes = {
        @Index(name = "idx_benchmark_sector", columnList = "sector"),
        @Index(name = "idx_benchmark_category", columnList = "expense_category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndustryBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sector", nullable = false)
    private String sector; // e.g., "IT_SERVICES", "RETAIL", "MANUFACTURING"

    @Column(name = "expense_category", nullable = false)
    private String expenseCategory; // e.g., "Marketing", "Rent"

    @Column(name = "benchmark_ratio_low", precision = 5, scale = 2)
    private BigDecimal benchmarkRatioLow; // Low end of healthy % of revenue

    @Column(name = "benchmark_ratio_high", precision = 5, scale = 2)
    private BigDecimal benchmarkRatioHigh; // High end of healthy % of revenue

    @Column(name = "benchmark_ratio_avg", precision = 5, scale = 2)
    private BigDecimal benchmarkRatioAvg; // Average % of revenue

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "source", length = 100)
    private String source; // e.g., "Industry Report 2025"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
