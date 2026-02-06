package ai.wealthwise.repository;

import ai.wealthwise.model.entity.IndustryBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndustryBenchmarkRepository extends JpaRepository<IndustryBenchmark, Long> {

    List<IndustryBenchmark> findBySector(String sector);

    Optional<IndustryBenchmark> findBySectorAndExpenseCategory(String sector, String expenseCategory);
}
