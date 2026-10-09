package com.xpense.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsSummaryDTO {

    private BigDecimal monthlySpend = BigDecimal.ZERO;
    private BigDecimal todaySpend = BigDecimal.ZERO;
    private long activeGoalsCount = 0;
    private BigDecimal avgDailySpend = BigDecimal.ZERO;
    private BigDecimal previousMonthSpend = BigDecimal.ZERO;
    private double percentChange = 0.0;
    private String peakSpendingDay = "Friday";
    private Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
    private List<String> aiInsights = new ArrayList<>();

    public AnalyticsSummaryDTO() {
    }

    public BigDecimal getMonthlySpend() {
        return monthlySpend;
    }

    public void setMonthlySpend(BigDecimal monthlySpend) {
        this.monthlySpend = monthlySpend;
    }

    public BigDecimal getTodaySpend() {
        return todaySpend;
    }

    public void setTodaySpend(BigDecimal todaySpend) {
        this.todaySpend = todaySpend;
    }

    public long getActiveGoalsCount() {
        return activeGoalsCount;
    }

    public void setActiveGoalsCount(long activeGoalsCount) {
        this.activeGoalsCount = activeGoalsCount;
    }

    public BigDecimal getAvgDailySpend() {
        return avgDailySpend;
    }

    public void setAvgDailySpend(BigDecimal avgDailySpend) {
        this.avgDailySpend = avgDailySpend;
    }

    public BigDecimal getPreviousMonthSpend() {
        return previousMonthSpend;
    }

    public void setPreviousMonthSpend(BigDecimal previousMonthSpend) {
        this.previousMonthSpend = previousMonthSpend;
    }

    public double getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(double percentChange) {
        this.percentChange = percentChange;
    }

    public String getPeakSpendingDay() {
        return peakSpendingDay;
    }

    public void setPeakSpendingDay(String peakSpendingDay) {
        this.peakSpendingDay = peakSpendingDay;
    }

    public Map<String, BigDecimal> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<String> getAiInsights() {
        return aiInsights;
    }

    public void setAiInsights(List<String> aiInsights) {
        this.aiInsights = aiInsights;
    }
}
