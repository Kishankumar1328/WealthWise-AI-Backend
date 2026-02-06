package ai.wealthwise.repository;

import ai.wealthwise.model.entity.Budget;
import ai.wealthwise.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Budget Repository - Data access layer for Budget entity
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

        List<Budget> findByUserAndIsActiveTrue(User user);

        Optional<Budget> findByUserAndMonthAndYearAndIsActiveTrue(User user, int month, int year);

        List<Budget> findByUserAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        User user, LocalDate date1, LocalDate date2);

        @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true " +
                        "AND :currentDate BETWEEN b.startDate AND b.endDate")
        List<Budget> findActiveBudgetsByUser(
                        @Param("user") User user,
                        @Param("currentDate") LocalDate currentDate);

}
