package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialStatement;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialStatementRepository extends JpaRepository<FinancialStatement, Long> {

    List<FinancialStatement> findBySmeBusinessOrderByFiscalYearDesc(SmeBusiness smeBusiness);

    List<FinancialStatement> findBySmeBusinessAndFiscalYear(SmeBusiness smeBusiness, String fiscalYear);

    Optional<FinancialStatement> findBySmeBusinessAndFiscalYearAndStatementType(
            SmeBusiness smeBusiness, String fiscalYear, FinancialStatement.StatementType statementType);

    @Query("SELECT fs FROM FinancialStatement fs WHERE fs.smeBusiness = :smeBusiness " +
            "AND fs.statementType = :type ORDER BY fs.fiscalYear DESC")
    List<FinancialStatement> findLatestByType(SmeBusiness smeBusiness, FinancialStatement.StatementType type);

    @Query("SELECT DISTINCT fs.fiscalYear FROM FinancialStatement fs WHERE fs.smeBusiness = :smeBusiness ORDER BY fs.fiscalYear DESC")
    List<String> findDistinctFiscalYears(SmeBusiness smeBusiness);
}
