package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialGoal;
import ai.wealthwise.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Financial Goal Repository - Data access layer for FinancialGoal entity
 */
@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    List<FinancialGoal> findByUserOrderByPriorityDescTargetDateAsc(User user);

    List<FinancialGoal> findByUserAndStatus(User user, FinancialGoal.GoalStatus status);

    List<FinancialGoal> findByUserAndGoalType(User user, FinancialGoal.GoalType goalType);

    Long countByUserAndStatus(User user, FinancialGoal.GoalStatus status);
}
