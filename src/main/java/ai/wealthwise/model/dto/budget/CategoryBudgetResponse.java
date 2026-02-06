package ai.wealthwise.model.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBudgetResponse {
    private Long id;
    private String category;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
}
