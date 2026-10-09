package com.xpense.config;

import com.xpense.model.*;
import com.xpense.repository.*;
import com.xpense.service.UserProfileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataInitializer(UserProfileRepository userProfileRepository,
                           WalletRepository walletRepository,
                           BudgetRepository budgetRepository,
                           SavingsGoalRepository savingsGoalRepository,
                           TransactionRepository transactionRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.walletRepository = walletRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String userId = UserProfileService.DEFAULT_USER_ID;

        // 1. Seed User Profile if not exists
        if (!userProfileRepository.existsById(userId)) {
            UserProfile profile = new UserProfile(
                    userId,
                    "128003008@sastra.ac.in",
                    "Aditya Venkata Sai Burle",
                    "128003008@sastra.ac.in",
                    new BigDecimal("2450.00")
            );
            profile.setPasswordHash(passwordEncoder.encode("demo123"));
            userProfileRepository.save(profile);
        } else {
            userProfileRepository.findById(userId).ifPresent(p -> {
                if (p.getPasswordHash() == null || p.getPasswordHash().isEmpty()) {
                    p.setPasswordHash(passwordEncoder.encode("demo123"));
                    userProfileRepository.save(p);
                }
            });
        }

        // 2. Seed Wallets if empty
        if (walletRepository.findByUserIdOrderByCreatedAtAsc(userId).isEmpty()) {
            Wallet w1 = new Wallet("wallet-1", userId, "Food & Dining", "Food & Dining",
                    new BigDecimal("850.00"), new BigDecimal("2000.00"), "ShoppingBag", "#10B981");
            w1.setDailyAvg(new BigDecimal("1150.00"));
            w1.setStatus("Good");

            Wallet w2 = new Wallet("wallet-2", userId, "Transportation", "Transportation",
                    new BigDecimal("450.00"), new BigDecimal("800.00"), "Car", "#3B82F6");
            w2.setDailyAvg(new BigDecimal("350.00"));
            w2.setStatus("Good");

            Wallet w3 = new Wallet("wallet-3", userId, "Entertainment", "Entertainment",
                    new BigDecimal("200.00"), new BigDecimal("1000.00"), "Gamepad2", "#A855F7");
            w3.setDailyAvg(new BigDecimal("800.00"));
            w3.setStatus("Low");

            Wallet w4 = new Wallet("wallet-4", userId, "Shopping & Utilities", "Shopping",
                    new BigDecimal("950.00"), new BigDecimal("1500.00"), "ShoppingCart", "#F59E0B");
            w4.setDailyAvg(new BigDecimal("400.00"));
            w4.setStatus("Good");

            walletRepository.saveAll(List.of(w1, w2, w3, w4));
        }

        // 3. Seed Budgets if empty
        if (budgetRepository.findByUserIdOrderByCategoryAsc(userId).isEmpty()) {
            Budget b1 = new Budget("budget-1", userId, "Food & Dining", new BigDecimal("2000.00"), new BigDecimal("1150.00"), "monthly");
            Budget b2 = new Budget("budget-2", userId, "Transportation", new BigDecimal("800.00"), new BigDecimal("350.00"), "monthly");
            Budget b3 = new Budget("budget-3", userId, "Entertainment", new BigDecimal("1000.00"), new BigDecimal("800.00"), "monthly");
            Budget b4 = new Budget("budget-4", userId, "Shopping", new BigDecimal("1500.00"), new BigDecimal("550.00"), "monthly");

            budgetRepository.saveAll(List.of(b1, b2, b3, b4));
        }

        // 4. Seed Savings Goals if empty
        if (savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(userId).isEmpty()) {
            SavingsGoal g1 = new SavingsGoal("goal-1", userId, "Emergency Fund",
                    new BigDecimal("5000.00"), new BigDecimal("2500.00"), LocalDate.of(2026, 12, 31), "Target", "Savings");

            SavingsGoal g2 = new SavingsGoal("goal-2", userId, "Semester Break Trip",
                    new BigDecimal("20000.00"), new BigDecimal("8000.00"), LocalDate.of(2026, 11, 15), "Plane", "Travel");

            savingsGoalRepository.saveAll(List.of(g1, g2));
        }

        // 5. Seed Transactions if empty
        if (transactionRepository.findByUserIdOrderByDateDesc(userId).isEmpty()) {
            Transaction tx1 = new Transaction("tx-1", userId, "wallet-1", "Food & Dining",
                    "The Coffee House", new BigDecimal("100.00"), "expense", "Food & Dining", "The Coffee House", null);
            tx1.setPaymentMethod("UPI QR");
            tx1.setDate(LocalDateTime.now().minusHours(2));

            Transaction tx2 = new Transaction("tx-2", userId, "wallet-4", "Shopping & Utilities",
                    "Campus Bookstore", new BigDecimal("80.00"), "expense", "Education", "Campus Bookstore", null);
            tx2.setPaymentMethod("UPI");
            tx2.setDate(LocalDateTime.now().minusHours(5));

            Transaction tx3 = new Transaction("tx-3", userId, null, "Main Account",
                    "Monthly Allowance / Freelance", new BigDecimal("2500.00"), "income", "Salary", null, null);
            tx3.setPaymentMethod("Bank Transfer");
            tx3.setDate(LocalDateTime.now().minusDays(1));

            Transaction tx4 = new Transaction("tx-4", userId, "wallet-2", "Transportation",
                    "Metro Smart Card Recharge", new BigDecimal("200.00"), "expense", "Transportation", "Metro Rail Corp", null);
            tx4.setPaymentMethod("UPI");
            tx4.setDate(LocalDateTime.now().minusDays(2));

            Transaction tx5 = new Transaction("tx-5", userId, "wallet-3", "Entertainment",
                    "Movie Ticket - PVR Cinemas", new BigDecimal("320.00"), "expense", "Entertainment", "BookMyShow", null);
            tx5.setPaymentMethod("UPI");
            tx5.setDate(LocalDateTime.now().minusDays(3));

            transactionRepository.saveAll(List.of(tx1, tx2, tx3, tx4, tx5));
        }
    }
}
