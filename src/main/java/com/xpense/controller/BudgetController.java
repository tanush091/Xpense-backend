package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.model.Budget;
import com.xpense.security.SecurityUtils;
import com.xpense.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Budget>>> getBudgets() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        List<Budget> budgets = budgetService.getBudgetsByUserId(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Budget>> getBudgetById(@PathVariable String id) {
        Budget budget = budgetService.getOwnedBudget(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Budget>> createOrUpdateBudget(@Valid @RequestBody Budget budget) {
        Budget saved = budgetService.saveBudget(SecurityUtils.getAuthenticatedUserId(), budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Spending limit saved", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable String id) {
        budgetService.deleteBudget(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success("Spending limit deleted", null));
    }
}
