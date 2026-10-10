package com.xpense.service;

import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.Budget;
import com.xpense.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public List<Budget> getBudgetsByUserId(String userId) {
        return budgetRepository.findByUserIdOrderByCategoryAsc(userId);
    }

    public Budget getOwnedBudget(String id, String userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Spending limit not found."));
    }

    /** Creates or updates the user's monthly limit for a category (one per category). */
    @Transactional
    public Budget saveBudget(String userId, Budget input) {
        if (input.getCategory() == null || input.getCategory().isBlank()) {
            throw new BadRequestException("Choose a category.");
        }
        if (input.getLimitAmount() == null || input.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("The limit must be more than zero.");
        }
        Budget budget = budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, input.getCategory().trim())
                .orElseGet(() -> new Budget(null, userId, input.getCategory().trim(), input.getLimitAmount(), BigDecimal.ZERO,
                        input.getPeriod()));
        budget.setLimitAmount(input.getLimitAmount());
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(String id, String userId) {
        Budget budget = getOwnedBudget(id, userId);
        budgetRepository.delete(budget);
    }

    @Transactional
    public void recordExpense(String userId, String category, BigDecimal amount) {
        if (category == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, category).ifPresent(budget -> {
            BigDecimal currentSpent = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
            budget.setSpentAmount(currentSpent.add(amount));
            budgetRepository.save(budget);
        });
    }

    @Transactional
    public void revertExpense(String userId, String category, BigDecimal amount) {
        if (category == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        budgetRepository.findByUserIdAndCategoryIgnoreCase(userId, category).ifPresent(budget -> {
            BigDecimal currentSpent = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
            BigDecimal newSpent = currentSpent.subtract(amount);
            if (newSpent.compareTo(BigDecimal.ZERO) < 0) {
                newSpent = BigDecimal.ZERO;
            }
            budget.setSpentAmount(newSpent);
            budgetRepository.save(budget);
        });
    }
}
