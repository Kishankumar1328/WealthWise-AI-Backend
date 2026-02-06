package ai.wealthwise.repository;

import ai.wealthwise.model.entity.CashFlowRecord;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashFlowRecordRepository extends JpaRepository<CashFlowRecord, Long> {

    List<CashFlowRecord> findBySmeBusinessOrderByRecordDateDesc(SmeBusiness smeBusiness);

    List<CashFlowRecord> findBySmeBusinessAndRecordDateBetweenOrderByRecordDateAsc(
            SmeBusiness smeBusiness, LocalDate startDate, LocalDate endDate);

    Optional<CashFlowRecord> findBySmeBusinessAndRecordDate(SmeBusiness smeBusiness, LocalDate date);

    @Query("SELECT cf FROM CashFlowRecord cf WHERE cf.smeBusiness = :smeBusiness " +
            "ORDER BY cf.recordDate DESC LIMIT 30")
    List<CashFlowRecord> findLast30Days(SmeBusiness smeBusiness);

    @Query("SELECT SUM(cf.totalInflow) FROM CashFlowRecord cf WHERE cf.smeBusiness = :smeBusiness " +
            "AND cf.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal sumInflowsForPeriod(SmeBusiness smeBusiness, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(cf.totalOutflow) FROM CashFlowRecord cf WHERE cf.smeBusiness = :smeBusiness " +
            "AND cf.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal sumOutflowsForPeriod(SmeBusiness smeBusiness, LocalDate startDate, LocalDate endDate);

    @Query("SELECT AVG(cf.netCashFlow) FROM CashFlowRecord cf WHERE cf.smeBusiness = :smeBusiness " +
            "AND cf.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal avgNetCashFlowForPeriod(SmeBusiness smeBusiness, LocalDate startDate, LocalDate endDate);
}
