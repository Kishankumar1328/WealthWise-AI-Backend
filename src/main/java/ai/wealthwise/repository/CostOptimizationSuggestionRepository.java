package ai.wealthwise.repository;

import ai.wealthwise.model.entity.CostOptimizationSuggestion;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostOptimizationSuggestionRepository extends JpaRepository<CostOptimizationSuggestion, Long> {

    List<CostOptimizationSuggestion> findByBusiness(SmeBusiness business);

    List<CostOptimizationSuggestion> findByBusinessOrderByPriorityAsc(SmeBusiness business);

    List<CostOptimizationSuggestion> findByBusinessAndStatus(SmeBusiness business,
            CostOptimizationSuggestion.Status status);
}
