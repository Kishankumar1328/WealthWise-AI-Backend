package ai.wealthwise.controller;

import ai.wealthwise.model.dto.budget.BudgetRequest;
import ai.wealthwise.model.dto.budget.BudgetResponse;
import ai.wealthwise.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Endpoints for managing personal budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "Get all budgets for current user")
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @GetMapping("/month/{year}/{month}")
    @Operation(summary = "Get budget for a specific month/year")
    public ResponseEntity<BudgetResponse> getBudgetByMonthYear(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(budgetService.getBudgetByMonthYear(month, year));
    }

    @PostMapping
    @Operation(summary = "Create a new main budget plan")
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        return new ResponseEntity<>(budgetService.createBudget(request), HttpStatus.CREATED);
    }

    @PostMapping("/{budgetId}/categories")
    @Operation(summary = "Add a category limit to a budget")
    public ResponseEntity<ai.wealthwise.model.dto.budget.CategoryBudgetResponse> addCategoryBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody ai.wealthwise.model.dto.budget.CategoryBudgetRequest request) {
        return new ResponseEntity<>(budgetService.addCategoryBudget(budgetId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing budget")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
