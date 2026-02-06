package ai.wealthwise.controller;

import ai.wealthwise.model.dto.financialgoal.FinancialGoalRequest;
import ai.wealthwise.model.dto.financialgoal.FinancialGoalResponse;
import ai.wealthwise.service.FinancialGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Financial Goals", description = "Endpoints for tracking financial goals")
@SecurityRequirement(name = "bearerAuth")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    @GetMapping
    @Operation(summary = "Get all financial goals for current user")
    public ResponseEntity<List<FinancialGoalResponse>> getAllGoals() {
        return ResponseEntity.ok(financialGoalService.getAllGoals());
    }

    @PostMapping
    @Operation(summary = "Create a new financial goal")
    public ResponseEntity<FinancialGoalResponse> createGoal(@Valid @RequestBody FinancialGoalRequest request) {
        return new ResponseEntity<>(financialGoalService.createGoal(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing financial goal")
    public ResponseEntity<FinancialGoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody FinancialGoalRequest request) {
        return ResponseEntity.ok(financialGoalService.updateGoal(id, request));
    }

    @PatchMapping("/{id}/add-funds")
    public FinancialGoalResponse addFunds(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        return financialGoalService.addFunds(id, amount);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a financial goal")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        financialGoalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
