package ai.wealthwise.service;

import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioAnalysisService {

        private final ScenarioAnalysisRepository scenarioRepository;
        private final SmeBusinessRepository businessRepository;

        @Transactional
        public ScenarioAnalysis createScenario(Long businessId, String name, String description,
                        BigDecimal revenueGrowthPct, BigDecimal expenseGrowthPct) {
                if (businessId == null)
                        throw new IllegalArgumentException("Business ID cannot be null");

                SmeBusiness business = businessRepository.findById(businessId)
                                .orElseThrow(() -> new RuntimeException("Business not found"));

                // Store parameters in JSON (Simplified manual JSON construction for now)
                String params = String.format("{\"revenue_growth\": %s, \"expense_growth\": %s}", revenueGrowthPct,
                                expenseGrowthPct);

                // Run Logic (Simplified Projection)
                // Assume current revenue = 100,000 (mock). In reality, fetch from
                // TransactionRepository.
                BigDecimal currentRevenue = BigDecimal.valueOf(100000);
                BigDecimal projectedRevenue = currentRevenue
                                .multiply(BigDecimal.ONE.add(revenueGrowthPct.divide(BigDecimal.valueOf(100))));

                String results = String.format(
                                "{\"projected_revenue\": %s, \"note\": \"Simple linear projection based on provided growth rates.\"}",
                                projectedRevenue);

                ScenarioAnalysis scenario = ScenarioAnalysis.builder()
                                .business(business)
                                .scenarioName(name)
                                .description(description)
                                .parametersJson(params)
                                .resultSummaryJson(results)
                                .status(ScenarioAnalysis.Status.COMPLETED)
                                .build();

                return java.util.Objects.requireNonNull(scenarioRepository.save(scenario));
        }

        public List<ScenarioAnalysis> getScenarios(Long businessId) {
                if (businessId == null)
                        throw new IllegalArgumentException("Business ID cannot be null");
                SmeBusiness business = businessRepository.findById(businessId)
                                .orElseThrow(() -> new RuntimeException("Business not found"));
                return scenarioRepository.findByBusinessOrderByCreatedAtDesc(business);
        }
}
