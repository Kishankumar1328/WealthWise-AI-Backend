package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialForecast;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinancialForecastRepository extends JpaRepository<FinancialForecast, Long> {
    List<FinancialForecast> findBySmeBusinessOrderByForecastDateAsc(SmeBusiness business);

    List<FinancialForecast> findBySmeBusinessAndForecastDateBetweenOrderByForecastDateAsc(
            SmeBusiness business, LocalDate start, LocalDate end);

    void deleteBySmeBusiness(SmeBusiness business);
}
