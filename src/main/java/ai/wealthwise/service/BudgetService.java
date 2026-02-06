package ai.wealthwise.service;

import ai.wealthwise.exception.ConflictException;
import ai.wealthwise.exception.ResourceNotFoundException;
import ai.wealthwise.model.dto.budget.BudgetRequest;
import ai.wealthwise.model.dto.budget.BudgetResponse;
import ai.wealthwise.model.dto.budget.CategoryBudgetRequest;
import ai.wealthwise.model.dto.budget.CategoryBudgetResponse;
import ai.wealthwise.model.entity.Budget;
import ai.wealthwise.model.entity.BudgetCategory;
import ai.wealthwise.model.entity.Expense;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.BudgetCategoryRepository;
import ai.wealthwise.repository.BudgetRepository;
import ai.wealthwise.repository.ExpenseRepository;
import ai.wealthwise.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        User user = securityUtils.getCurrentUser();
        return budgetRepository.findByUserAndIsActiveTrue(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetByMonthYear(int month, int year) {
        User user = securityUtils.getCurrentUser();
        Budget budget = budgetRepository.findByUserAndMonthAndYearAndIsActiveTrue(user, month, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget not found for month " + month + " and year " + year));
        return mapToResponse(budget);
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = securityUtils.getCurrentUser();

        budgetRepository.findByUserAndMonthAndYearAndIsActiveTrue(user, request.getMonth(), request.getYear())
                .ifPresent(b -> {
                    throw new ConflictException("A budget for this month/year already exists");
                });

        Budget budget = Budget.builder()
                .user(user)
                .name(request.getName())
                .month(request.getMonth())
                .year(request.getYear())
                .totalBudgetAmount(request.getTotalBudgetAmount())
                .period(Budget.BudgetPeriod.valueOf(request.getPeriod()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .categories(new java.util.ArrayList<>())
                .build();

        return mapToResponse(budgetRepository.save(budget));
    }

    @Transactional
    public CategoryBudgetResponse addCategoryBudget(Long budgetId,
            CategoryBudgetRequest request) {
        User user = securityUtils.getRequiredCurrentUser();
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ConflictException("Unauthorized access to budget");
        }

        // Check if category already budgeted
        budgetCategoryRepository.findByBudgetAndCategory(budget, request.getCategory())
                .ifPresent(c -> {
                    throw new ConflictException(
                            "Category '" + request.getCategory() + "' is already budgeted for this plan");
                });

        BudgetCategory category = BudgetCategory.builder()
                .budget(budget)
                .category(request.getCategory())
                .budgetAmount(request.getBudgetAmount())
                .spentAmount(BigDecimal.ZERO)
                .build();

        BudgetCategory savedCategory = budgetCategoryRepository.saveAndFlush(category);

        // Explicitly sync parent collection and save parent if needed
        if (budget.getCategories() == null) {
            budget.setCategories(new java.util.ArrayList<>());
        }
        budget.getCategories().add(savedCategory);
        budgetRepository.saveAndFlush(budget);

        return mapCategoryToResponse(savedCategory, new ArrayList<>());
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        User user = securityUtils.getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ConflictException("Unauthorized access to budget");
        }

        budget.setName(request.getName());
        budget.setTotalBudgetAmount(request.getTotalBudgetAmount());
        budget.setPeriod(Budget.BudgetPeriod.valueOf(request.getPeriod()));
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());

        return mapToResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long id) {
        User user = securityUtils.getRequiredCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ConflictException("Unauthorized access to budget");
        }

        budgetRepository.delete(budget);
        budgetRepository.flush();
    }

    private BudgetResponse mapToResponse(Budget budget) {
        if (budget == null)
            return null;

        // Fetch all totals once to avoid N+1 queries
        List<Object[]> totalsPerCategory;
        try {
            totalsPerCategory = expenseRepository.getTotalsByCategory(
                    budget.getUser(),
                    Expense.TransactionType.EXPENSE,
                    budget.getStartDate(),
                    budget.getEndDate());
        } catch (Exception e) {
            totalsPerCategory = new java.util.ArrayList<>();
        }

        List<CategoryBudgetResponse> categories = new java.util.ArrayList<>();
        if (budget.getCategories() != null) {
            for (ai.wealthwise.model.entity.BudgetCategory c : budget.getCategories()) {
                categories.add(this.mapCategoryToResponse(c, totalsPerCategory));
            }
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .name(budget.getName())
                .month(budget.getMonth())
                .year(budget.getYear())
                .totalBudgetAmount(budget.getTotalBudgetAmount())
                .categories(categories)
                .period(budget.getPeriod() != null ? budget.getPeriod().name() : "MONTHLY")
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    private CategoryBudgetResponse mapCategoryToResponse(ai.wealthwise.model.entity.BudgetCategory category,
            List<Object[]> totalsPerCategory) {
        java.math.BigDecimal categorySpent = java.math.BigDecimal.ZERO;

        if (category != null && category.getCategory() != null) {
            String targetCategory = category.getCategory().name();
            if (totalsPerCategory != null) {
                for (Object[] result : totalsPerCategory) {
                    if (result != null && result.length >= 2 && result[0] != null) {
                        String catName = result[0].toString();
                        if (catName.equals(targetCategory)) {
                            Object sum = result[1];
                            if (sum instanceof java.math.BigDecimal) {
                                categorySpent = (java.math.BigDecimal) sum;
                            } else if (sum instanceof Number) {
                                categorySpent = new java.math.BigDecimal(((Number) sum).toString());
                            }
                            break;
                        }
                    }
                }
            }
        }

        return CategoryBudgetResponse.builder()
                .id(category != null ? category.getId() : null)
                .category(category != null && category.getCategory() != null ? category.getCategory().name() : "OTHER")
                .budgetAmount(category != null ? category.getBudgetAmount() : java.math.BigDecimal.ZERO)
                .spentAmount(categorySpent)
                .build();
    }
}
