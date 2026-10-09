package com.xpense.service;

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

    public Budget getBudgetById(String id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
    }

    @Transactional
    public Budget saveBudget(Budget budget) {
        if (budget.getUserId() == null || budget.getUserId().isEmpty()) {
            budget.setUserId(UserProfileService.DEFAULT_USER_ID);
        }
        if (budget.getSpentAmount() == null) {
            budget.setSpentAmount(BigDecimal.ZERO);
        }
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(String id) {
        Budget budget = getBudgetById(id);
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
