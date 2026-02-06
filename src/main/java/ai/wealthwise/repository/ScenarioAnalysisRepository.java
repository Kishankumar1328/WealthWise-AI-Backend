package ai.wealthwise.repository;

import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.ScenarioAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScenarioAnalysisRepository extends JpaRepository<ScenarioAnalysis, Long> {
    List<ScenarioAnalysis> findByBusinessOrderByCreatedAtDesc(SmeBusiness business);
}
