package ai.wealthwise.service;

import ai.wealthwise.exception.ResourceNotFoundException;
import ai.wealthwise.model.dto.expense.ExpenseRequest;
import ai.wealthwise.model.dto.expense.ExpenseResponse;
import ai.wealthwise.model.entity.Expense;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.ExpenseRepository;
import ai.wealthwise.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expense Service - Business logic for expense management
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAllExpenses(Pageable pageable) {
        User user = securityUtils.getCurrentUser();
        return expenseRepository.findByUserOrderByTransactionDateDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = securityUtils.getCurrentUser();

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .paymentMethod(request.getPaymentMethod())
                .merchant(request.getMerchant())
                .notes(request.getNotes())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .type(request.getType() != null ? request.getType() : Expense.TransactionType.EXPENSE)
                .tags(request.getTags())
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        return mapToResponse(savedExpense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = securityUtils.getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        // Ensure user owns this expense
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this expense");
        }

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setTransactionDate(request.getTransactionDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setMerchant(request.getMerchant());
        expense.setNotes(request.getNotes());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setType(request.getType() != null ? request.getType() : expense.getType());
        expense.setTags(request.getTags());

        Expense updatedExpense = expenseRepository.save(expense);
        return mapToResponse(updatedExpense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        User user = securityUtils.getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this expense");
        }

        expenseRepository.delete(expense);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .transactionDate(expense.getTransactionDate())
                .paymentMethod(expense.getPaymentMethod())
                .merchant(expense.getMerchant())
                .notes(expense.getNotes())
                .isRecurring(expense.getIsRecurring())
                .type(expense.getType())
                .tags(expense.getTags())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
