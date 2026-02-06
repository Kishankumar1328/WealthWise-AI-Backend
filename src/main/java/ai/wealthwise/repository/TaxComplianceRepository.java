package ai.wealthwise.repository;

import ai.wealthwise.model.entity.TaxCompliance;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaxComplianceRepository extends JpaRepository<TaxCompliance, Long> {

    List<TaxCompliance> findBySmeBusinessOrderByDueDateDesc(SmeBusiness smeBusiness);

    List<TaxCompliance> findBySmeBusinessAndFilingType(SmeBusiness smeBusiness, TaxCompliance.FilingType filingType);

    List<TaxCompliance> findBySmeBusinessAndFilingStatus(SmeBusiness smeBusiness, TaxCompliance.FilingStatus status);

    @Query("SELECT tc FROM TaxCompliance tc WHERE tc.smeBusiness = :smeBusiness " +
            "AND tc.filingStatus IN ('PENDING', 'OVERDUE') ORDER BY tc.dueDate ASC")
    List<TaxCompliance> findPendingFilings(SmeBusiness smeBusiness);

    @Query("SELECT tc FROM TaxCompliance tc WHERE tc.smeBusiness = :smeBusiness " +
            "AND tc.dueDate < :date AND tc.filingStatus = 'PENDING'")
    List<TaxCompliance> findOverdueFilings(SmeBusiness smeBusiness, LocalDate date);

    @Query("SELECT tc FROM TaxCompliance tc WHERE tc.smeBusiness = :smeBusiness " +
            "AND tc.dueDate BETWEEN :startDate AND :endDate ORDER BY tc.dueDate ASC")
    List<TaxCompliance> findUpcomingFilings(SmeBusiness smeBusiness, LocalDate startDate, LocalDate endDate);

    @Query("SELECT AVG(tc.complianceScore) FROM TaxCompliance tc WHERE tc.smeBusiness = :smeBusiness " +
            "AND tc.complianceScore IS NOT NULL")
    Double calculateAverageComplianceScore(SmeBusiness smeBusiness);

    @Query("SELECT COUNT(tc) FROM TaxCompliance tc WHERE tc.smeBusiness = :smeBusiness " +
            "AND tc.filingStatus = 'FILED_LATE'")
    Long countLateFilings(SmeBusiness smeBusiness);
}
