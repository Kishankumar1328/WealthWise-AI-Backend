package ai.wealthwise.repository;

import ai.wealthwise.model.entity.ParsedTransaction;
import ai.wealthwise.model.entity.FinancialDocument;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ParsedTransactionRepository extends JpaRepository<ParsedTransaction, Long> {

        List<ParsedTransaction> findByDocumentOrderByTransactionDateDesc(FinancialDocument document);

        List<ParsedTransaction> findByBusinessOrderByTransactionDateDesc(SmeBusiness business);

        List<ParsedTransaction> findByBusinessAndTransactionDateBetweenOrderByTransactionDateDesc(
                        SmeBusiness business, LocalDate startDate, LocalDate endDate);

        List<ParsedTransaction> findByBusinessAndCategoryOrderByTransactionDateDesc(
                        SmeBusiness business, String category);

        List<ParsedTransaction> findByBusinessAndTransactionTypeOrderByTransactionDateDesc(
                        SmeBusiness business, ParsedTransaction.TransactionType transactionType);

        @Query("SELECT SUM(p.amount) FROM ParsedTransaction p WHERE p.business = :business " +
                        "AND p.transactionType = 'CREDIT' AND p.transactionDate BETWEEN :startDate AND :endDate")
        BigDecimal sumCreditsForPeriod(@Param("business") SmeBusiness business,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(p.amount) FROM ParsedTransaction p WHERE p.business = :business " +
                        "AND p.transactionType = 'DEBIT' AND p.transactionDate BETWEEN :startDate AND :endDate")
        BigDecimal sumDebitsForPeriod(@Param("business") SmeBusiness business,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT p.category, SUM(p.amount) FROM ParsedTransaction p WHERE p.business = :business " +
                        "AND p.transactionType = 'DEBIT' AND p.transactionDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY p.category ORDER BY SUM(p.amount) DESC")
        List<Object[]> getExpensesByCategory(@Param("business") SmeBusiness business,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT p.partyName, SUM(p.amount), COUNT(p) FROM ParsedTransaction p WHERE p.business = :business " +
                        "AND p.transactionType = 'DEBIT' AND p.transactionDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY p.partyName ORDER BY SUM(p.amount) DESC")
        List<Object[]> getTopVendors(@Param("business") SmeBusiness business,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT p.partyName, SUM(p.amount), COUNT(p) FROM ParsedTransaction p WHERE p.business = :business " +
                        "AND p.transactionType = 'CREDIT' AND p.transactionDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY p.partyName ORDER BY SUM(p.amount) DESC")
        List<Object[]> getTopCustomers(@Param("business") SmeBusiness business,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        long countByDocument(FinancialDocument document);

        void deleteByDocument(FinancialDocument document);

        List<ParsedTransaction> findByBusinessAndIsVerifiedFalse(SmeBusiness business);

        @Query("SELECT p FROM ParsedTransaction p WHERE p.business.id = :businessId " +
                        "AND p.transactionDate >= :cutoffDate ORDER BY p.transactionDate DESC")
        List<ParsedTransaction> findRecentTransactions(@Param("businessId") Long businessId,
                        @Param("cutoffDate") LocalDate cutoffDate);

        default List<ParsedTransaction> findRecentTransactions(Long businessId, int daysToCheck) {
                return findRecentTransactions(businessId, LocalDate.now().minusDays(daysToCheck));
        }
}
