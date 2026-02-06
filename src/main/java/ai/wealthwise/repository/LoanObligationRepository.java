package ai.wealthwise.repository;

import ai.wealthwise.model.entity.LoanObligation;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanObligationRepository extends JpaRepository<LoanObligation, Long> {

    List<LoanObligation> findBySmeBusinessOrderByDisbursementDateDesc(SmeBusiness smeBusiness);

    List<LoanObligation> findBySmeBusinessAndLoanStatus(SmeBusiness smeBusiness, LoanObligation.LoanStatus status);

    @Query("SELECT lo FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness " +
            "AND lo.loanStatus IN ('ACTIVE', 'OVERDUE') ORDER BY lo.nextEmiDate ASC")
    List<LoanObligation> findActiveLoans(SmeBusiness smeBusiness);

    @Query("SELECT SUM(lo.outstandingBalance) FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness " +
            "AND lo.loanStatus IN ('ACTIVE', 'OVERDUE')")
    BigDecimal sumTotalOutstanding(SmeBusiness smeBusiness);

    @Query("SELECT SUM(lo.emiAmount) FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness " +
            "AND lo.loanStatus = 'ACTIVE'")
    BigDecimal sumMonthlyEmiObligation(SmeBusiness smeBusiness);

    @Query("SELECT lo FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness AND lo.overdueDays > 0")
    List<LoanObligation> findOverdueLoans(SmeBusiness smeBusiness);

    @Query("SELECT SUM(lo.overdueAmount) FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness")
    BigDecimal sumOverdueAmount(SmeBusiness smeBusiness);

    @Query("SELECT COUNT(lo) FROM LoanObligation lo WHERE lo.smeBusiness = :smeBusiness " +
            "AND lo.loanStatus = 'ACTIVE'")
    Long countActiveLoans(SmeBusiness smeBusiness);
}
