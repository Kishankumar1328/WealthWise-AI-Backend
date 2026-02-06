package ai.wealthwise.service;

import ai.wealthwise.model.dto.dashboard.DashboardSummaryResponse;
import ai.wealthwise.model.entity.Expense;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.ExpenseRepository;
import ai.wealthwise.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final ExpenseRepository expenseRepository;
        private final ai.wealthwise.repository.BudgetRepository budgetRepository;
        private final SecurityUtils securityUtils;

        @Transactional(readOnly = true)
        public DashboardSummaryResponse getSummary() {
                User user = securityUtils.getCurrentUser();
                LocalDate now = LocalDate.now();
                LocalDate startOfMonth = now.withDayOfMonth(1);
                LocalDate sevenDaysAgo = now.minusDays(6);

                // 1. Calculate Total Balance (Total Income - Total Expense)
                BigDecimal totalIncome = expenseRepository.getTotalAmountByUserAndType(user,
                                Expense.TransactionType.INCOME);
                BigDecimal totalExpense = expenseRepository.getTotalAmountByUserAndType(user,
                                Expense.TransactionType.EXPENSE);

                if (totalIncome == null)
                        totalIncome = BigDecimal.ZERO;
                if (totalExpense == null)
                        totalExpense = BigDecimal.ZERO;

                BigDecimal totalBalance = totalIncome.subtract(totalExpense);

                // 2. Monthly Spending (Expense type only this month)
                BigDecimal monthlySpending = expenseRepository.getTotalByUserAndTypeAndDateRange(
                                user, Expense.TransactionType.EXPENSE, startOfMonth, now);
                if (monthlySpending == null)
                        monthlySpending = BigDecimal.ZERO;

                // 3. Total Investments (Sum of category INVESTMENTS)
                // Note: We'll sum both Income and Expense types for investments if needed,
                // but typically it's an expense to move money to an investment account.
                // For simplicity: sum of all transaction amounts in category INVESTMENTS
                List<Expense> investmentTxs = expenseRepository.findByUserAndCategory(user,
                                Expense.ExpenseCategory.INVESTMENTS);
                BigDecimal totalInvestments = investmentTxs.stream()
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 4. Recent transactions
                List<DashboardSummaryResponse.RecentTransaction> recent = expenseRepository
                                .findByUserOrderByTransactionDateDesc(user, PageRequest.of(0, 5))
                                .getContent().stream()
                                .map(e -> new DashboardSummaryResponse.RecentTransaction(
                                                e.getId(),
                                                e.getDescription(),
                                                e.getCategory().getDisplayName(),
                                                e.getAmount(),
                                                e.getTransactionDate().format(DateTimeFormatter.ISO_DATE),
                                                e.getType().name()))
                                .collect(Collectors.toList());

                // 5. Category breakdown (Expenses only this month)
                List<Object[]> categorySums = expenseRepository.getTotalsByCategory(
                                user, Expense.TransactionType.EXPENSE, startOfMonth, now);
                List<DashboardSummaryResponse.CategoryBreakdown> categories = new ArrayList<>();
                String[] colors = { "#4f46e5", "#0ea5e9", "#10b981", "#f59e0b", "#ef4444" };
                int i = 0;
                for (Object[] result : categorySums) {
                        String categoryName;
                        if (result[0] instanceof Expense.ExpenseCategory) {
                                categoryName = ((Expense.ExpenseCategory) result[0]).getDisplayName();
                        } else {
                                categoryName = result[0].toString();
                        }

                        BigDecimal sum = (result[1] instanceof BigDecimal) ? (BigDecimal) result[1] : BigDecimal.ZERO;

                        categories.add(new DashboardSummaryResponse.CategoryBreakdown(
                                        categoryName,
                                        sum,
                                        colors[i % colors.length]));
                        i++;
                }

                // 6. Real spending trends (Last 7 days)
                List<DashboardSummaryResponse.SpendingTrend> trends = new ArrayList<>();
                List<Object[]> dailyTotals = expenseRepository.getDailyTotals(
                                user, Expense.TransactionType.EXPENSE, sevenDaysAgo, now);

                // Map to ensure all days are present
                Map<LocalDate, BigDecimal> dailyMap = dailyTotals.stream()
                                .collect(Collectors.toMap(
                                                res -> (LocalDate) res[0],
                                                res -> (BigDecimal) res[1]));

                for (int j = 0; j < 7; j++) {
                        LocalDate date = sevenDaysAgo.plusDays(j);
                        String dayName = date.format(DateTimeFormatter.ofPattern("EEE"));
                        BigDecimal spent = dailyMap.getOrDefault(date, BigDecimal.ZERO);
                        trends.add(new DashboardSummaryResponse.SpendingTrend(dayName, spent));
                }

                // 7. Calculate real budget data
                BigDecimal totalBudget = BigDecimal.ZERO;
                BigDecimal budgetUsage = BigDecimal.ZERO;

                java.util.Optional<ai.wealthwise.model.entity.Budget> activeBudget = budgetRepository
                                .findByUserAndMonthAndYearAndIsActiveTrue(user, now.getMonthValue(), now.getYear());

                if (activeBudget.isPresent()) {
                        totalBudget = activeBudget.get().getTotalBudgetAmount();
                        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                                budgetUsage = monthlySpending.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));
                        }
                }

                // Count categories over budget
                int overBudgetCount = 0;
                if (activeBudget.isPresent() && activeBudget.get().getCategories() != null) {
                        Map<String, BigDecimal> spentMap = categorySums.stream()
                                        .collect(Collectors.toMap(
                                                        res -> (res[0] instanceof Expense.ExpenseCategory)
                                                                        ? ((Expense.ExpenseCategory) res[0]).name()
                                                                        : res[0].toString(),
                                                        res -> (res[1] instanceof BigDecimal) ? (BigDecimal) res[1]
                                                                        : BigDecimal.ZERO));

                        for (ai.wealthwise.model.entity.BudgetCategory cat : activeBudget.get().getCategories()) {
                                BigDecimal spent = spentMap.getOrDefault(cat.getCategory().name(), BigDecimal.ZERO);
                                if (spent.compareTo(cat.getBudgetAmount()) > 0) {
                                        overBudgetCount++;
                                }
                        }
                }

                return DashboardSummaryResponse.builder()
                                .totalBalance(totalBalance)
                                .monthlySpending(monthlySpending)
                                .totalInvestments(totalInvestments)
                                .pendingBills(overBudgetCount) // Use this for over-budget alerts
                                .totalBudget(totalBudget)
                                .budgetUsagePercentage(budgetUsage)
                                .spendingTrends(trends)
                                .categoryBreakdown(categories)
                                .recentTransactions(recent)
                                .build();
        }
}
