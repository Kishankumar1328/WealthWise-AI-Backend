package ai.wealthwise.repository;

import ai.wealthwise.model.entity.Budget;
import ai.wealthwise.model.entity.BudgetCategory;
import ai.wealthwise.model.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    List<BudgetCategory> findByBudget(Budget budget);

    Optional<BudgetCategory> findByBudgetAndCategory(Budget budget, Expense.ExpenseCategory category);
}
