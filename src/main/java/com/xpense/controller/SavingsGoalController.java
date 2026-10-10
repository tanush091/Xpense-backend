package com.xpense.controller;

import com.xpense.dto.ApiResponse;
import com.xpense.dto.GoalDepositRequest;
import com.xpense.dto.GoalUpdateRequest;
import com.xpense.model.SavingsGoal;
import com.xpense.security.SecurityUtils;
import com.xpense.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoal>>> getGoals() {
        String effectiveUserId = SecurityUtils.getAuthenticatedUserId();
        List<SavingsGoal> goals = savingsGoalService.getGoalsByUserId(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> getGoalById(@PathVariable String id) {
        SavingsGoal goal = savingsGoalService.getOwnedGoal(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoal>> createGoal(@Valid @RequestBody SavingsGoal goal) {
        goal.setUserId(SecurityUtils.getAuthenticatedUserId());
        SavingsGoal created = savingsGoalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Goal created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> updateGoal(@PathVariable String id, @RequestBody GoalUpdateRequest goal) {
        SavingsGoal updated = savingsGoalService.updateGoal(id, SecurityUtils.getAuthenticatedUserId(), goal);
        return ResponseEntity.ok(ApiResponse.success("Goal updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable String id) {
        savingsGoalService.deleteGoal(id, SecurityUtils.getAuthenticatedUserId());
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<SavingsGoal>> depositToGoal(
            @PathVariable String id,
            @Valid @RequestBody GoalDepositRequest request) {
        SavingsGoal updated = savingsGoalService.contributeToGoal(id, SecurityUtils.getAuthenticatedUserId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Savings added", updated));
    }
}
