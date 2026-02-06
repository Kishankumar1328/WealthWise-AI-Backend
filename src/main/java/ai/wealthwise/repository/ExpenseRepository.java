package ai.wealthwise.repository;

import ai.wealthwise.model.entity.Expense;
import ai.wealthwise.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Expense Repository - Data access layer for Expense entity
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

        Page<Expense> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);

        List<Expense> findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
                        User user, LocalDate startDate, LocalDate endDate);

        List<Expense> findByUserAndCategory(User user, Expense.ExpenseCategory category);

        @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user " +
                        "AND e.type = :type AND e.transactionDate BETWEEN :startDate AND :endDate")
        BigDecimal getTotalByUserAndTypeAndDateRange(
                        @Param("user") User user,
                        @Param("type") Expense.TransactionType type,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user = :user " +
                        "AND e.type = :type AND e.transactionDate BETWEEN :startDate AND :endDate GROUP BY e.category")
        List<Object[]> getTotalsByCategory(
                        @Param("user") User user,
                        @Param("type") Expense.TransactionType type,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT e.transactionDate, SUM(e.amount) FROM Expense e WHERE e.user = :user " +
                        "AND e.type = :type AND e.transactionDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY e.transactionDate ORDER BY e.transactionDate ASC")
        List<Object[]> getDailyTotals(
                        @Param("user") User user,
                        @Param("type") Expense.TransactionType type,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.type = :type")
        BigDecimal getTotalAmountByUserAndType(
                        @Param("user") User user,
                        @Param("type") Expense.TransactionType type);

        @Query("SELECT COUNT(e) FROM Expense e WHERE e.user = :user " +
                        "AND e.transactionDate BETWEEN :startDate AND :endDate")
        Long countExpensesByUserAndDateRange(
                        @Param("user") User user,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
