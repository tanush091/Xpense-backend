package com.xpense.service;

import com.xpense.exception.BadRequestException;
import com.xpense.exception.ResourceNotFoundException;
import com.xpense.model.SavingsGoal;
import com.xpense.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public SavingsGoal getGoalById(String id) {
        return savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with id: " + id));
    }

    @Transactional
    public SavingsGoal createGoal(SavingsGoal goal) {
        if (goal.getUserId() == null || goal.getUserId().isEmpty()) {
            goal.setUserId(UserProfileService.DEFAULT_USER_ID);
        }
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(BigDecimal.ZERO);
        }
        if (goal.getStatus() == null) {
            goal.setStatus("in_progress");
        }
        return savingsGoalRepository.save(goal);
    }

    @Transactional
    public SavingsGoal updateGoal(String id, SavingsGoal updateData) {
        SavingsGoal goal = getGoalById(id);
        if (updateData.getTitle() != null) goal.setTitle(updateData.getTitle());
        if (updateData.getTargetAmount() != null) goal.setTargetAmount(updateData.getTargetAmount());
        if (updateData.getCurrentAmount() != null) goal.setCurrentAmount(updateData.getCurrentAmount());
        if (updateData.getTargetDate() != null) goal.setTargetDate(updateData.getTargetDate());
        if (updateData.getIcon() != null) goal.setIcon(updateData.getIcon());
        if (updateData.getCategory() != null) goal.setCategory(updateData.getCategory());
        if (updateData.getStatus() != null) goal.setStatus(updateData.getStatus());
        return savingsGoalRepository.save(goal);
    }

    @Transactional
    public void deleteGoal(String id) {
        SavingsGoal goal = getGoalById(id);
        // Money saved in the goal returns to the user's unbudgeted balance
        BigDecimal saved = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        if (saved.compareTo(BigDecimal.ZERO) > 0) {
            userProfileService.adjustBalance(goal.getUserId(), saved);
        }
        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoal contributeToGoal(String id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Contribution amount must be greater than zero");
        }
        SavingsGoal goal = getGoalById(id);

        // Savings come out of money that is not already set aside in a wallet
        BigDecimal unbudgeted = walletService.getUnbudgetedBalance(goal.getUserId());
        if (unbudgeted.compareTo(amount) < 0) {
            throw new BadRequestException("You only have ₹" + unbudgeted.toPlainString()
                    + " that is not in a budget yet. Add money first or choose a smaller amount.");
        }

        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal newAmount = current.add(amount);
        goal.setCurrentAmount(newAmount);

        if (goal.getTargetAmount() != null && newAmount.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("completed");
        }

        SavingsGoal saved = savingsGoalRepository.save(goal);

        // Deduct contributed amount from overall user balance
        userProfileService.adjustBalance(goal.getUserId(), amount.negate());

        return saved;
    }
}
