package ai.wealthwise.model.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import ai.wealthwise.model.entity.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Expense Request DTO - For creating/updating expenses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private Expense.ExpenseCategory category;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Transaction date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    private Expense.PaymentMethod paymentMethod;
    private String merchant;
    private String notes;
    private Boolean isRecurring;
    @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
    private Expense.TransactionType type;

    private String tags;
}
