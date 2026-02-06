package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scenario_analyses", indexes = {
        @Index(name = "idx_scenario_business", columnList = "business_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private SmeBusiness business;

    @Column(name = "scenario_name", nullable = false)
    private String scenarioName;

    @Column(name = "description")
    private String description;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson; // e.g. {"revenue_growth_pct": 10, "expense_growth_pct": 5}

    @Column(name = "result_summary_json", columnDefinition = "TEXT")
    private String resultSummaryJson; // e.g. {"projected_profit": 500000, "cash_flow_impact": 20000}

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.DRAFT;

    public enum Status {
        DRAFT, COMPLETED, ARCHIVED
    }
}
