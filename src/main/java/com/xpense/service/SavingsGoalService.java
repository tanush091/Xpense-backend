package com.xpense.service;

import com.xpense.dto.GoalUpdateRequest;
import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.SavingsGoal;
import com.xpense.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserProfileService userProfileService;
    private final WalletService walletService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              UserProfileService userProfileService,
                              WalletService walletService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userProfileService = userProfileService;
        this.walletService = walletService;
    }

    public List<SavingsGoal> getGoalsByUserId(String userId) {
        return savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public SavingsGoal getOwnedGoal(String id, String userId) {
        return savingsGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));
    }

    /** New goals start at zero; money is added through deposits so totals stay correct. */
    @Transactional
    public SavingsGoal createGoal(SavingsGoal goal) {
        goal.setTitle(Inputs.requiredText(goal.getTitle(), 100, "The goal name", "Give your goal a name."));
        if (goal.getTargetAmount() == null) {
            throw new BadRequestException("Set how much you want to save.");
        }
        Inputs.requireAmount(goal.getTargetAmount());
        if (goal.getTargetDate() != null && goal.getTargetDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Pick a date in the future for your goal.");
        }
        goal.setCategory(Inputs.text(goal.getCategory(), 64, "The category"));
        goal.setId(null);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setStatus("in_progress");
        boolean emergency = Boolean.TRUE.equals(goal.getIsEmergency());
        goal.setIsEmergency(emergency);
        SavingsGoal saved = savingsGoalRepository.save(goal);
        if (emergency) {
            clearOtherEmergencyGoals(saved.getUserId(), saved.getId());
        }
        return saved;
    }

    /** Edits only the fields that were sent. The saved amount only changes through deposits. */
    @Transactional
    public SavingsGoal updateGoal(String id, String userId, GoalUpdateRequest update) {
        SavingsGoal goal = getOwnedGoal(id, userId);
        if (update.getTitle() != null) {
            goal.setTitle(Inputs.requiredText(update.getTitle(), 100, "The goal name", "The goal name can't be empty."));
        }
        if (update.getTargetAmount() != null) {
            Inputs.requireAmount(update.getTargetAmount());
            goal.setTargetAmount(update.getTargetAmount());
        }
        if (update.getTargetDate() != null) goal.setTargetDate(update.getTargetDate());
        if (update.getIcon() != null) goal.setIcon(Inputs.text(update.getIcon(), 64, "The icon"));
        if (update.getCategory() != null) goal.setCategory(Inputs.text(update.getCategory(), 64, "The category"));
        if (update.getIsEmergency() != null) {
            goal.setIsEmergency(update.getIsEmergency());
            if (update.getIsEmergency()) {
                clearOtherEmergencyGoals(userId, goal.getId());
            }
        }
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        goal.setStatus(current.compareTo(goal.getTargetAmount()) >= 0 ? "completed" : "in_progress");
        return savingsGoalRepository.save(goal);
    }

    /** Money saved in the goal returns to the user's unbudgeted balance. */
    @Transactional
    public void deleteGoal(String id, String userId) {
        userProfileService.lockForMoneyChange(userId);
        SavingsGoal goal = getOwnedGoal(id, userId);
        BigDecimal saved = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        if (saved.compareTo(BigDecimal.ZERO) > 0) {
            userProfileService.adjustBalance(userId, saved);
        }
        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoal contributeToGoal(String id, String userId, BigDecimal amount) {
        userProfileService.lockForMoneyChange(userId);
        Inputs.requireAmount(amount);
        SavingsGoal goal = getOwnedGoal(id, userId);

        // Savings come out of money that is not already set aside in a wallet
        BigDecimal unbudgeted = walletService.getUnbudgetedBalance(userId);
        if (unbudgeted.compareTo(amount) < 0) {
            throw new BadRequestException("You only have " + Inputs.inr(unbudgeted)
                    + " that is not in a budget yet. Add money first or choose a smaller amount.");
        }

        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal newAmount = current.add(amount);
        goal.setCurrentAmount(newAmount);

        if (goal.getTargetAmount() != null && newAmount.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("completed");
        }

        SavingsGoal saved = savingsGoalRepository.save(goal);
        userProfileService.adjustBalance(userId, amount.negate());
        return saved;
    }

    /** Only one goal can be the emergency fund. */
    private void clearOtherEmergencyGoals(String userId, String keepId) {
        savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsEmergency()) && !g.getId().equals(keepId))
                .forEach(g -> {
                    g.setIsEmergency(false);
                    savingsGoalRepository.save(g);
                });
    }
}
