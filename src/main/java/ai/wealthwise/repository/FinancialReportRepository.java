package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialReport;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {

    List<FinancialReport> findByBusinessOrderByGenerationDateDesc(SmeBusiness business);

    List<FinancialReport> findByBusinessAndReportTypeOrderByGenerationDateDesc(SmeBusiness business,
            FinancialReport.ReportType reportType);
}
