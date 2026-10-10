package com.xpense.service;

import com.xpense.dto.AnalyticsSummaryDTO;
import com.xpense.model.SavingsGoal;
import com.xpense.model.Transaction;
import com.xpense.repository.SavingsGoalRepository;
import com.xpense.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.xpense.dto.AlertDTO;
import com.xpense.dto.MonthlyTotalDTO;
import com.xpense.dto.PayeeDTO;
import java.time.YearMonth;
import java.time.format.TextStyle;
import com.xpense.model.Budget;
import com.xpense.model.Wallet;
import com.xpense.repository.BudgetRepository;
import com.xpense.repository.WalletRepository;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BudgetRepository budgetRepository;
    private final WalletRepository walletRepository;

    public AnalyticsService(TransactionRepository transactionRepository,
                            SavingsGoalRepository savingsGoalRepository,
                            BudgetRepository budgetRepository,
                            WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.budgetRepository = budgetRepository;
        this.walletRepository = walletRepository;
    }

    public List<AlertDTO> getAlerts(String userId) {
        List<AlertDTO> alerts = new ArrayList<>();

        // 1. Wallets health check (ADR-008: <10% Warning, <30% Low)
        List<Wallet> wallets = walletRepository.findByUserIdOrderByCreatedAtAsc(userId);
        for (Wallet w : wallets) {
            if (w.getBudgetLimit() != null && w.getBudgetLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bal = w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO;
                double ratio = bal.doubleValue() / w.getBudgetLimit().doubleValue();
                if (bal.signum() == 0 && transactionRepository.findFiltered(userId, null, "expense", w.getId()).isEmpty()) {
                    continue; // never filled: not a problem yet
                }
                if (ratio < 0.10) {
                    alerts.add(new AlertDTO(
                            UUID.randomUUID().toString(),
                            "DANGER",
                            "Budget almost empty",
                            w.getName() + " has less than 10% left.",
                            "WALLET",
                            w.getId()
                    ));
                } else if (ratio < 0.30) {
                    alerts.add(new AlertDTO(
                            UUID.randomUUID().toString(),
                            "WARN",
                            "Budget running low",
                            w.getName() + " has less than 30% left.",
                            "WALLET",
                            w.getId()
                    ));
                }
            }
        }

        // 2. Budgets check (Thresholds: 80% approaching, 100% exceeded)
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);
        for (Budget b : budgets) {
            if (b.getLimitAmount() != null && b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal spent = b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO;
                double ratio = spent.doubleValue() / b.getLimitAmount().doubleValue();
                if (ratio >= 1.0) {
                    alerts.add(new AlertDTO(
                            UUID.randomUUID().toString(),
                            "DANGER",
                            "Budget Exceeded",
                            "You have exceeded your monthly limit for " + b.getCategory() + " (" + Math.round(ratio * 100) + "% spent).",
                            "BUDGET",
                            b.getId()
                    ));
                } else if (ratio >= 0.80) {
                    alerts.add(new AlertDTO(
                            UUID.randomUUID().toString(),
                            "WARN",
                            "Budget Approaching Limit",
                            "You have used " + Math.round(ratio * 100) + "% of your " + b.getCategory() + " envelope.",
                            "BUDGET",
                            b.getId()
                    ));
                }
            }
        }

        return alerts;
    }

    public AnalyticsSummaryDTO getAnalyticsSummary(String userId) {
        AnalyticsSummaryDTO dto = new AnalyticsSummaryDTO();
        LocalDate now = LocalDate.now();

        // 1. Current month range
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        // 2. Previous month range
        LocalDate prevMonthDate = now.minusMonths(1);
        LocalDateTime startOfPrevMonth = prevMonthDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfPrevMonth = prevMonthDate.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        // 3. Today range
        LocalDateTime startOfToday = now.atStartOfDay();
        LocalDateTime endOfToday = now.atTime(LocalTime.MAX);

        List<Transaction> currentMonthTxs = transactionRepository.findByUserIdAndDateBetween(userId, startOfMonth, endOfMonth);
        List<Transaction> prevMonthTxs = transactionRepository.findByUserIdAndDateBetween(userId, startOfPrevMonth, endOfPrevMonth);

        BigDecimal monthlySpend = BigDecimal.ZERO;
        BigDecimal todaySpend = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (Transaction tx : currentMonthTxs) {
            if ("expense".equalsIgnoreCase(tx.getType())) {
                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                monthlySpend = monthlySpend.add(amt);

                if (tx.getDate() != null && !tx.getDate().isBefore(startOfToday) && !tx.getDate().isAfter(endOfToday)) {
                    todaySpend = todaySpend.add(amt);
                }

                String cat = tx.getCategory() != null ? tx.getCategory() : "General";
                breakdown.put(cat, breakdown.getOrDefault(cat, BigDecimal.ZERO).add(amt));
            }
        }

        BigDecimal prevMonthSpend = BigDecimal.ZERO;
        for (Transaction tx : prevMonthTxs) {
            if ("expense".equalsIgnoreCase(tx.getType())) {
                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                prevMonthSpend = prevMonthSpend.add(amt);
            }
        }

        // Active Goals
        List<SavingsGoal> goals = savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long activeGoals = goals.stream().filter(g -> "in_progress".equalsIgnoreCase(g.getStatus())).count();

        // Average Daily Spend
        int dayOfMonth = Math.max(1, now.getDayOfMonth());
        BigDecimal avgDailySpend = monthlySpend.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);

        // Percent change
        double pctChange = 0.0;
        if (prevMonthSpend.compareTo(BigDecimal.ZERO) > 0) {
            pctChange = ((monthlySpend.doubleValue() - prevMonthSpend.doubleValue()) / prevMonthSpend.doubleValue()) * 100.0;
        }

        dto.setMonthlySpend(monthlySpend);
        dto.setTodaySpend(todaySpend);
        dto.setActiveGoalsCount(activeGoals);
        dto.setAvgDailySpend(avgDailySpend);
        dto.setPreviousMonthSpend(prevMonthSpend);
        dto.setPercentChange(Math.round(pctChange * 10.0) / 10.0);
        dto.setCategoryBreakdown(breakdown);

        // AI Insights
        List<String> insights = new ArrayList<>();
        if (monthlySpend.compareTo(BigDecimal.ZERO) == 0) {
            insights.add("No expenses logged this month yet. Great start keeping your wallet full!");
        } else {
            String topCategory = breakdown.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Daily Life");
            insights.add("Your highest expense category is " + topCategory + ". Check whether that budget's limit still fits.");
            if (pctChange > 15) {
                insights.add("Monthly spending has risen by " + Math.round(pctChange) + "% compared to last month. Pace yourself!");
            } else if (pctChange < -5) {
                insights.add("Great discipline! Spending is down " + Math.abs(Math.round(pctChange)) + "% from last month.");
            }
        }
        if (activeGoals > 0) {
            insights.add("You have " + activeGoals + " active savings goal(s). Depositing spare change weekly accelerates progress.");
        }
        dto.setAiInsights(insights);

        return dto;
    }

    /** Money in and money out for each of the last {@code months} calendar months, oldest first. */
    public List<MonthlyTotalDTO> getMonthlyTotals(String userId, int months) {
        YearMonth current = YearMonth.now();
        YearMonth first = current.minusMonths(months - 1L);
        LocalDateTime from = first.atDay(1).atStartOfDay();
        LocalDateTime to = current.atEndOfMonth().atTime(LocalTime.MAX);

        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            totals.put(first.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Transaction tx : transactionRepository.findByUserIdAndDateBetween(userId, from, to)) {
            if (tx.getDate() == null || tx.getAmount() == null) continue;
            BigDecimal[] row = totals.get(YearMonth.from(tx.getDate()));
            if (row == null) continue;
            if ("income".equalsIgnoreCase(tx.getType())) {
                row[0] = row[0].add(tx.getAmount());
            } else {
                row[1] = row[1].add(tx.getAmount());
            }
        }

        List<MonthlyTotalDTO> result = new ArrayList<>();
        totals.forEach((ym, row) -> result.add(new MonthlyTotalDTO(
                ym.toString(),
                ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                row[0], row[1])));
        return result;
    }

    /** Who received the most money between {@code from} and {@code to} (inclusive), biggest first. */
    public List<PayeeDTO> getTopPayees(String userId, LocalDate from, LocalDate to, int limit) {
        Map<String, BigDecimal> amounts = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Transaction tx : transactionRepository.findByUserIdAndDateBetween(userId, from.atStartOfDay(), to.atTime(LocalTime.MAX))) {
            if ("income".equalsIgnoreCase(tx.getType()) || tx.getAmount() == null) continue;
            String name = firstNonBlank(tx.getMerchant(), tx.getRecipient(), tx.getTitle(), "Other");
            amounts.merge(name, tx.getAmount(), BigDecimal::add);
            counts.merge(name, 1, Integer::sum);
            total = total.add(tx.getAmount());
        }

        final BigDecimal grandTotal = total;
        return amounts.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new PayeeDTO(
                        e.getKey(),
                        e.getValue(),
                        grandTotal.signum() > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(grandTotal, 0, RoundingMode.HALF_UP).intValue()
                                : 0,
                        counts.get(e.getKey())))
                .toList();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "Other";
    }
}
